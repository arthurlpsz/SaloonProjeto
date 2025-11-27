package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.TimeSlotAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class AgendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var rvSlots: RecyclerView
    private lateinit var imgVoltar: ImageView
    private lateinit var btnHoje: Button
    private lateinit var btnAmanha: Button
    private lateinit var btnOutroDia: Button
    private lateinit var btnConfirmarAgendamento: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var salaoId: String? = null
    private var servicoId: String? = null
    private var servicoDuracaoMin = 45
    private var nomeServico: String? = null

    private lateinit var slotAdapter: TimeSlotAdapter
    private val listaSlots = mutableListOf<String>()

    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedSlot: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            salaoId = it.getString("salaoId")
            servicoId = it.getString("servicoId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_agendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        calendarView = view.findViewById(R.id.calendar_view)
        rvSlots = view.findViewById(R.id.rv_slots)
        imgVoltar = view.findViewById(R.id.img_voltar)
        btnHoje = view.findViewById(R.id.btn_hoje)
        btnAmanha = view.findViewById(R.id.btn_amanha)
        btnOutroDia = view.findViewById(R.id.btn_outro_dia)
        btnConfirmarAgendamento = view.findViewById(R.id.btn_confirmar_agendamento)

        rvSlots.layoutManager = GridLayoutManager(requireContext(), 3)
        slotAdapter = TimeSlotAdapter(listaSlots) { horaSelecionada ->
            selectedSlot = horaSelecionada
            btnConfirmarAgendamento.visibility = View.VISIBLE
        }
        rvSlots.adapter = slotAdapter

        setupDateButtons()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            carregarSlots()
            calendarView.visibility = View.GONE
        }

        imgVoltar.setOnClickListener {
            findNavController().popBackStack()
        }

        btnConfirmarAgendamento.setOnClickListener {
            selectedSlot?.let { slot ->
                confirmarAgendamento(slot)
            }
        }

        // Load slots for today initially
        btnHoje.performClick()
    }

    private fun setupDateButtons() {
        btnHoje.setOnClickListener {
            selectedDate = Calendar.getInstance()
            updateButtonStyles(it)
            calendarView.visibility = View.GONE
            carregarSlots()
        }

        btnAmanha.setOnClickListener {
            selectedDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            updateButtonStyles(it)
            calendarView.visibility = View.GONE
            carregarSlots()
        }

        btnOutroDia.setOnClickListener {
            updateButtonStyles(it)
            calendarView.visibility = if (calendarView.isVisible) View.GONE else View.VISIBLE
        }
    }

    private fun updateButtonStyles(selectedButton: View) {
        val buttons = listOf(btnHoje, btnAmanha, btnOutroDia)
        buttons.forEach { button ->
            if (button == selectedButton) {
                button.setBackgroundResource(R.drawable.button_selected_background)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                button.setBackgroundResource(R.drawable.button_unselected_background)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold))
            }
        }
    }

    private fun carregarSlots() {
        btnConfirmarAgendamento.visibility = View.GONE
        selectedSlot = null
        if (salaoId == null || servicoId == null) {
            Toast.makeText(requireContext(), "Erro: dados ausentes.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val servicoDoc = db.collection("servicos").document(servicoId!!).get().await()
                servicoDuracaoMin = servicoDoc.getLong("duracaoMin")?.toInt() ?: 45
                nomeServico = servicoDoc.getString("nome")

                val salaoDoc = db.collection("usuarios").document(salaoId!!).get().await()
                val workingHours = salaoDoc.get("workingHours") as? Map<*, *>

                if (workingHours == null) {
                    Toast.makeText(requireContext(), "Salão sem horários definidos.", Toast.LENGTH_LONG).show()
                    listaSlots.clear()
                    slotAdapter.updateList(listaSlots.toList())
                    return@launch
                }

                val keyDia = dayKeyFromCalendar(selectedDate)
                val diaMap = workingHours[keyDia] as? Map<*, *>

                if (diaMap == null) {
                    listaSlots.clear()
                    slotAdapter.updateList(listaSlots.toList())
                    Toast.makeText(requireContext(), "Salão fechado neste dia.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val abreMin = (diaMap["start"] as? Long)?.toInt() ?: 0
                val fechaMin = (diaMap["end"] as? Long)?.toInt() ?: 0

                val ocupados = carregarAgendamentosExistentes()
                gerarSlots(abreMin, fechaMin, ocupados)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar horários: ${e.message}", Toast.LENGTH_LONG).show()
                listaSlots.clear()
                slotAdapter.updateList(listaSlots.toList())
            }
        }
    }

    private suspend fun carregarAgendamentosExistentes(): List<Pair<Int, Int>> {
        val iniDia = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val fimDia = (iniDia.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        val snap = db.collection("agendamentos")
            .whereEqualTo("salaoId", salaoId)
            .whereGreaterThanOrEqualTo("dataInicio", Timestamp(iniDia.time))
            .whereLessThan("dataInicio", Timestamp(fimDia.time))
            .orderBy("dataInicio", Query.Direction.ASCENDING)
            .get()
            .await()

        val ocupados = mutableListOf<Pair<Int, Int>>()
        for (doc in snap) {
            val dInicio = doc.getTimestamp("dataInicio")?.toDate() ?: continue
            val dFim = doc.getTimestamp("dataFim")?.toDate() ?: continue

            val calS = Calendar.getInstance().apply { time = dInicio }
            val calE = Calendar.getInstance().apply { time = dFim }

            val s = calS.get(Calendar.HOUR_OF_DAY) * 60 + calS.get(Calendar.MINUTE)
            val e = calE.get(Calendar.HOUR_OF_DAY) * 60 + calE.get(Calendar.MINUTE)
            ocupados.add(Pair(s, e))
        }
        return ocupados
    }

    private fun gerarSlots(startMin: Int, endMin: Int, ocupados: List<Pair<Int, Int>>) {
        listaSlots.clear()

        var cursor = startMin
        while (cursor + servicoDuracaoMin <= endMin) {
            val s = cursor
            val e = cursor + servicoDuracaoMin

            val conflito = ocupados.any { it.first < e && it.second > s }
            if (!conflito) {
                listaSlots.add(formatMinToText(s))
            }

            cursor += 15
        }

        slotAdapter.updateList(listaSlots.toList())

        if (listaSlots.isEmpty()) {
            Toast.makeText(requireContext(), "Sem horários livres para este dia.", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatMinToText(min: Int): String {
        val h = min / 60
        val m = min % 60
        return String.format("%02d:%02d", h, m)
    }

    private fun dayKeyFromCalendar(c: Calendar): String {
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "domingo"
            Calendar.MONDAY -> "segunda"
            Calendar.TUESDAY -> "terca"
            Calendar.WEDNESDAY -> "quarta"
            Calendar.THURSDAY -> "quinta"
            Calendar.FRIDAY -> "sexta"
            Calendar.SATURDAY -> "sabado"
            else -> ""
        }
    }

    private fun confirmarAgendamento(hora: String) {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val userDoc = db.collection("usuarios").document(userId).get().await()
                val nomeUsuario = userDoc.getString("nome")

                val salaoDoc = db.collection("usuarios").document(salaoId!!).get().await()
                val nomeSalao = salaoDoc.getString("nomeSalao")

                val partes = hora.split(":")
                val h = partes[0].toInt()
                val m = partes[1].toInt()

                val cInicio = (selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                }

                val inicio = Timestamp(cInicio.time)
                val fim = Timestamp(Date(cInicio.timeInMillis + servicoDuracaoMin * 60000L))

                val dados = hashMapOf(
                    "salaoId" to salaoId,
                    "clienteId" to userId,
                    "servicoId" to servicoId,
                    "nomeUsuario" to nomeUsuario,
                    "nomeSalao" to nomeSalao,
                    "servico" to nomeServico,
                    "dataInicio" to inicio,
                    "dataFim" to fim,
                    "status" to "Confirmado",
                    "createdAt" to Timestamp.now()
                )

                db.collection("agendamentos").add(dados).await()
                Toast.makeText(requireContext(), "Agendado com sucesso!", Toast.LENGTH_LONG).show()
                btnConfirmarAgendamento.visibility = View.GONE
                selectedSlot = null
                carregarSlots()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao agendar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
