package br.edu.fatecpg.saloonprojeto.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgendarFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var btnHoje: MaterialButton
    private lateinit var btnAmanha: MaterialButton
    private lateinit var btnOutroDia: MaterialButton
    private lateinit var btnConfirmar: MaterialButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: String? = null

    private lateinit var adapter: TimeSlotAdapter

    private var salaoId: String? = null
    private var nomeSalao: String? = null
    private var servicoId: String? = null
    private var nomeServico: String? = null

    // Horários padrão (caso o salão ainda não tenha configurado)
    private var horarioInicio: String = "09:00"
    private var horarioFim: String = "18:00"

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_agendar, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        btnHoje = view.findViewById(R.id.btn_hoje)
        btnAmanha = view.findViewById(R.id.btn_amanha)
        btnOutroDia = view.findViewById(R.id.btn_outro_dia)
        btnConfirmar = view.findViewById(R.id.btn_confirmar_agendamento)
        recyclerView = view.findViewById(R.id.time_slots_recycler_view)
        progressBar = view.findViewById(R.id.progress_bar)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TimeSlotAdapter(emptyList()) { time -> selectedTime = time }
        recyclerView.adapter = adapter

        arguments?.let {
            salaoId = it.getString("salaoId")
            nomeSalao = it.getString("nomeSalao")
            servicoId = it.getString("servicoId")
            nomeServico = it.getString("nomeServico")
        }

        setupDateButtons()
        carregarHorarioDeFuncionamento()

        btnConfirmar.setOnClickListener { confirmarAgendamento() }

        return view
    }

    /** Configura os botões de data **/
    private fun setupDateButtons() {
        btnHoje.setOnClickListener {
            selectedDate = Calendar.getInstance()
            loadAvailableTimes()
        }

        btnAmanha.setOnClickListener {
            selectedDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
            loadAvailableTimes()
        }

        btnOutroDia.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate.set(year, month, day)
                    loadAvailableTimes()
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /** Busca horário de funcionamento no Firestore **/
    private fun carregarHorarioDeFuncionamento() {
        val id = salaoId ?: return
        showLoading(true)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = db.collection("saloes").document(id).get().await()
                horarioInicio = doc.getString("horarioInicio") ?: "09:00"
                horarioFim = doc.getString("horarioFim") ?: "18:00"
            } catch (e: Exception) {
                horarioInicio = "09:00"
                horarioFim = "18:00"
            } finally {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    loadAvailableTimes()
                }
            }
        }
    }

    /** Gera intervalos de 30 minutos entre abertura e fechamento **/
    private fun generateSlotsBetween(open: String, close: String): List<String> {
        val slots = mutableListOf<String>()
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val start = Calendar.getInstance().apply { time = sdf.parse(open)!! }
            val end = Calendar.getInstance().apply { time = sdf.parse(close)!! }

            while (start.before(end)) {
                slots.add(sdf.format(start.time))
                start.add(Calendar.MINUTE, 30) // intervalos de meia hora
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return slots
    }

    /** Carrega horários disponíveis **/
    private fun loadAvailableTimes() {
        val baseSlots = generateSlotsBetween(horarioInicio, horarioFim)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = sdf.format(selectedDate.time)

        if (salaoId == null) return
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val booked = db.collection("agendamentos")
                    .whereEqualTo("salaoId", salaoId)
                    .whereGreaterThanOrEqualTo("dataHora", "$dateString 00:00")
                    .whereLessThanOrEqualTo("dataHora", "$dateString 23:59")
                    .get()
                    .await()

                val bookedTimes = booked.documents.mapNotNull {
                    it.getString("dataHora")?.takeLast(5)
                }

                val available = baseSlots.filterNot { bookedTimes.contains(it) }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    adapter = TimeSlotAdapter(available) { time -> selectedTime = time }
                    recyclerView.adapter = adapter

                    if (available.isEmpty()) {
                        Toast.makeText(requireContext(), "Nenhum horário disponível neste dia.", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Erro ao carregar horários: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Confirma agendamento **/
    private fun confirmarAgendamento() {
        val user = auth.currentUser ?: return
        val hora = selectedTime ?: return Toast.makeText(requireContext(), "Selecione um horário", Toast.LENGTH_SHORT).show()

        val dataHoraString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time) + " $hora"

        btnConfirmar.isEnabled = false
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = db.collection("agendamentos")
                    .whereEqualTo("salaoId", salaoId)
                    .whereEqualTo("dataHora", dataHoraString)
                    .get()
                    .await()

                if (!existing.isEmpty) {
                    withContext(Dispatchers.Main) {
                        btnConfirmar.isEnabled = true
                        showLoading(false)
                        Toast.makeText(requireContext(), "Esse horário já está ocupado!", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val agendamento = hashMapOf(
                    "clienteId" to user.uid,
                    "salaoId" to salaoId,
                    "servicoId" to servicoId,
                    "nomeServico" to nomeServico,
                    "dataHora" to dataHoraString,
                    "criadoEm" to Timestamp.now(),
                    "status" to "pendente"
                )

                db.collection("agendamentos").add(agendamento).await()

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    btnConfirmar.isEnabled = true
                    Toast.makeText(requireContext(), "Agendamento criado com sucesso!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    btnConfirmar.isEnabled = true
                    Toast.makeText(requireContext(), "Erro ao criar agendamento: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Exibe/oculta o loading **/
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnConfirmar.isEnabled = !show
        btnHoje.isEnabled = !show
        btnAmanha.isEnabled = !show
        btnOutroDia.isEnabled = !show
    }
}
