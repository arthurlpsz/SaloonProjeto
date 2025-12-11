package br.edu.fatecpg.saloonprojeto.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.TimeSlotAdapter
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AgendarFragment : Fragment() {

    private lateinit var rvSlots: RecyclerView
    private lateinit var imgVoltar: ImageView
    private lateinit var btnHoje: Button
    private lateinit var btnAmanha: Button
    private lateinit var btnOutroDia: Button
    private lateinit var btnConfirmarAgendamento: Button
    private lateinit var txvSelectedDate: TextView

    // Header Views
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

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
        rvSlots = view.findViewById(R.id.rv_slots)
        imgVoltar = view.findViewById(R.id.img_voltar)
        btnHoje = view.findViewById(R.id.btn_hoje)
        btnAmanha = view.findViewById(R.id.btn_amanha)
        btnOutroDia = view.findViewById(R.id.btn_outro_dia)
        btnConfirmarAgendamento = view.findViewById(R.id.btn_confirmar_agendamento)
        txvSelectedDate = view.findViewById(R.id.txv_selected_date)

        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        rvSlots.layoutManager = GridLayoutManager(requireContext(), 3)
        slotAdapter = TimeSlotAdapter(listaSlots) { horaSelecionada ->
            selectedSlot = horaSelecionada
            btnConfirmarAgendamento.visibility = View.VISIBLE
        }
        rvSlots.adapter = slotAdapter

        setupDateButtons()

        imgVoltar.setOnClickListener {
            findNavController().popBackStack()
        }

        btnConfirmarAgendamento.setOnClickListener {
            selectedSlot?.let { slot ->
                confirmarAgendamento(slot)
            }
        }

        loadHeaderInfo()
        btnHoje.performClick()
    }

    private fun loadHeaderInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() && isAdded) {
                        val name = document.getString("nome")
                        userName.text = name ?: "Nome não encontrado"

                        val imageUrl = document.getString("fotoUrl")
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(profileImage)
                    }
                }
        }
    }

    private fun setupDateButtons() {
        btnHoje.setOnClickListener {
            selectedDate = Calendar.getInstance()
            updateDateTextView()
            updateButtonStyles(it)
            carregarSlots()
        }

        btnAmanha.setOnClickListener {
            selectedDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            updateDateTextView()
            updateButtonStyles(it)
            carregarSlots()
        }

        btnOutroDia.setOnClickListener {
            updateButtonStyles(it)
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            updateDateTextView()
            carregarSlots()
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateDateTextView() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = sdf.format(selectedDate.time)
        txvSelectedDate.text = "Data do agendamento: $formattedDate"
    }

    private fun updateButtonStyles(selectedButton: View) {
        val buttons = listOf(btnHoje, btnAmanha, btnOutroDia)
        buttons.forEach { button ->
            if (button == selectedButton) {
                button.setBackgroundResource(R.drawable.button_selected_background)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.branco))
            } else {
                button.setBackgroundResource(R.drawable.button_unselected_background)
                button.setTextColor(ContextCompat.getColor(requireContext(), R.color.dourado))
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

                val abreMin = (diaMap["start"] as? Number)?.toInt() ?: 0
                val fechaMin = (diaMap["end"] as? Number)?.toInt() ?: 0

                val ocupados = carregarHorariosOcupados()
                gerarSlots(abreMin, fechaMin, ocupados)

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar horários: ${e.message}", Toast.LENGTH_LONG).show()
                listaSlots.clear()
                slotAdapter.updateList(listaSlots.toList())
            }
        }
    }

    private suspend fun carregarHorariosOcupados(): List<Pair<Int, Int>> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val diaFormatado = dateFormat.format(selectedDate.time)

        try {
            val doc = db.collection("horariosOcupados").document(salaoId!!).collection("dias").document(diaFormatado).get().await()
            if (doc.exists()) {
                val slots = doc.get("slots") as? List<Map<String, Long>>
                return slots?.mapNotNull {
                    val inicio = it["inicio"]?.toInt()
                    val fim = it["fim"]?.toInt()
                    if (inicio != null && fim != null) Pair(inicio, fim) else null
                } ?: emptyList()
            }
        } catch (e: Exception) {
            // O documento pode não existir, o que não é um erro.
        }
        return emptyList()
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

            cursor += 30
        }

        slotAdapter.updateList(listaSlots.toList())

        if (listaSlots.isEmpty()) {
            Toast.makeText(requireContext(), "Sem horários livres para este dia.", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatMinToText(min: Int): String {
        val h = min / 60
        val m = min % 60
        return String.format(Locale.getDefault(), "%02d:%02d", h, m)
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
                val inicioTimestamp = Timestamp(cInicio.time)
                val fimTimestamp = Timestamp(Date(cInicio.timeInMillis + servicoDuracaoMin * 60000L))

                val agendamentoRef = db.collection("agendamentos").document()
                val dadosAgendamento = hashMapOf(
                    "salaoId" to salaoId,
                    "clienteId" to userId,
                    "servicoId" to servicoId,
                    "nomeUsuario" to nomeUsuario,
                    "nomeSalao" to nomeSalao,
                    "servico" to nomeServico,
                    "dataInicio" to inicioTimestamp,
                    "dataFim" to fimTimestamp,
                    "status" to "Confirmado",
                    "createdAt" to Timestamp.now()
                )

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val diaFormatado = dateFormat.format(selectedDate.time)
                val horarioOcupadoRef = db.collection("horariosOcupados").document(salaoId!!).collection("dias").document(diaFormatado)
                val dadosHorario = hashMapOf(
                    "inicio" to (h * 60 + m),
                    "fim" to (h * 60 + m + servicoDuracaoMin)
                )

                // Roda as duas escritas em um batch atômico
                db.runBatch {
                    it.set(agendamentoRef, dadosAgendamento)
                    it.set(horarioOcupadoRef, hashMapOf("slots" to FieldValue.arrayUnion(dadosHorario)), SetOptions.merge())
                }.await()

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
