package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.TimeSlotAdapter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AgendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var rvSlots: RecyclerView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var salaoId: String? = null
    private var servicoId: String? = null
    private var servicoDuracaoMin = 45

    private lateinit var slotAdapter: TimeSlotAdapter
    private val listaSlots = mutableListOf<String>()

    private var selectedDate: Calendar = Calendar.getInstance()

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
        calendarView = view.findViewById(R.id.calendar_view)
        rvSlots = view.findViewById(R.id.rv_slots)

        rvSlots.layoutManager = LinearLayoutManager(requireContext())
        slotAdapter = TimeSlotAdapter(listaSlots) { horaSelecionada ->
            confirmarAgendamento(horaSelecionada)
        }
        rvSlots.adapter = slotAdapter

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            carregarSlots()
        }

        // Carregar slots para a data atual inicialmente
        carregarSlots()
    }

    private fun carregarSlots() {
        if (salaoId == null || servicoId == null) {
            Toast.makeText(requireContext(), "Erro: dados ausentes.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("servicos").document(servicoId!!)
            .get()
            .addOnSuccessListener { doc ->
                servicoDuracaoMin = doc.getLong("duracaoMin")?.toInt() ?: 45
                carregarHorariosDoSalao()
            }
    }

    private fun carregarHorariosDoSalao() {
        db.collection("usuarios").document(salaoId!!)
            .get()
            .addOnSuccessListener { doc ->

                val working = doc.get("workingHours") as? Map<*, *>
                if (working == null) {
                    Toast.makeText(requireContext(), "Salão sem horários definidos.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val keyDia = dayKeyFromCalendar(selectedDate)
                val diaMap = working[keyDia] as? Map<*, *>
                if (diaMap == null) {
                    listaSlots.clear()
                    slotAdapter.updateList(listaSlots.toList())
                    Toast.makeText(requireContext(), "Salão fechado neste dia.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val abreMin = (diaMap["start"] as? Long)?.toInt() ?: 0
                val fechaMin = (diaMap["end"] as? Long)?.toInt() ?: 0

                carregarAgendamentosExistentes(abreMin, fechaMin)
            }
    }

    private fun carregarAgendamentosExistentes(startMin: Int, endMin: Int) {
        val iniDia = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val fimDia = (iniDia.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        db.collection("agendamentos")
            .whereEqualTo("salaoId", salaoId)
            .whereGreaterThanOrEqualTo("dataInicio", Timestamp(iniDia.time))
            .whereLessThan("dataInicio", Timestamp(fimDia.time))
            .get()
            .addOnSuccessListener { snap ->
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

                gerarSlots(startMin, endMin, ocupados)
            }
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
        return when (c[Calendar.DAY_OF_WEEK]) {
            Calendar.MONDAY -> "segunda"
            Calendar.TUESDAY -> "terca"
            Calendar.WEDNESDAY -> "quarta"
            Calendar.THURSDAY -> "quinta"
            Calendar.FRIDAY -> "sexta"
            Calendar.SATURDAY -> "sabado"
            else -> "domingo"
        }
    }

    private fun confirmarAgendamento(hora: String) {
        val userId = auth.currentUser?.uid ?: return

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
            "dataInicio" to inicio,
            "dataFim" to fim,
            "status" to "agendado",
            "createdAt" to Timestamp.now()
        )

        db.collection("agendamentos")
            .add(dados)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Agendado com sucesso!", Toast.LENGTH_LONG).show()
                // Recarregar os slots para remover o horário agendado
                carregarSlots()
            }
    }
}
