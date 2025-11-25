package br.edu.fatecpg.saloonprojeto.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.TimeSlotAdapter
import br.edu.fatecpg.saloonprojeto.model.Agendamento
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale

class AgendarFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var salonName: TextView
    private lateinit var serviceName: TextView
    private lateinit var servicePrice: TextView
    private lateinit var selectedDate: TextView
    private lateinit var ivBack: ImageView

    private lateinit var recyclerView: RecyclerView
    private lateinit var timeAdapter: TimeSlotAdapter

    private lateinit var btnHoje: Button
    private lateinit var btnAmanha: Button
    private lateinit var btnOutroDia: Button
    private lateinit var btnConfirmar: Button

    private var salaoId: String = ""
    private var servicoId: String = ""
    private var dataSelecionada: String = ""
    private var horarioSelecionado: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_agendar, container, false)

        // Recebendo parâmetros
        salaoId = arguments?.getString("salaoId") ?: ""
        servicoId = arguments?.getString("servicoId") ?: ""

        // Views
        salonName = view.findViewById(R.id.salon_name)
        serviceName = view.findViewById(R.id.service_name)
        servicePrice = view.findViewById(R.id.service_price)
        selectedDate = view.findViewById(R.id.selected_date)
        ivBack = view.findViewById(R.id.iv_back)

        btnHoje = view.findViewById(R.id.btn_hoje)
        btnAmanha = view.findViewById(R.id.btn_amanha)
        btnOutroDia = view.findViewById(R.id.btn_outro_dia)
        btnConfirmar = view.findViewById(R.id.btn_confirmar_agendamento)

        recyclerView = view.findViewById(R.id.time_slots_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        // Adapter para horários
        timeAdapter = TimeSlotAdapter { horario ->
            horarioSelecionado = horario
        }
        recyclerView.adapter = timeAdapter

        carregarDadosSalao()
        carregarDadosServico()
        configurarSelecaoDatas()

        ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnConfirmar.setOnClickListener {
            confirmarAgendamento()
        }

        return view
    }

    private fun carregarDadosSalao() {
        db.collection("usuarios").document(salaoId).get()
            .addOnSuccessListener { doc ->
                salonName.text = doc.getString("nomeSalao") ?: "Salão"
            }
    }

    private fun carregarDadosServico() {
        db.collection("servicos").document(servicoId).get()
            .addOnSuccessListener { doc ->
                serviceName.text = doc.getString("nome") ?: "Serviço"
                servicePrice.text = "R$ " + (doc.get("preco")?.toString() ?: "0,00")
            }
    }

    /** DATAS **/
    private fun configurarSelecaoDatas() {
        val calendar = Calendar.getInstance()
        updateDateLabel(calendar) // Define a data de hoje inicialmente
        dataSelecionada = formatarData(calendar)
        carregarHorarios()

        btnHoje.setOnClickListener {
            val today = Calendar.getInstance()
            dataSelecionada = formatarData(today)
            updateDateLabel(today)
            carregarHorarios()
        }

        btnAmanha.setOnClickListener {
            val tomorrow = Calendar.getInstance()
            tomorrow.add(Calendar.DAY_OF_MONTH, 1)
            dataSelecionada = formatarData(tomorrow)
            updateDateLabel(tomorrow)
            carregarHorarios()
        }

        btnOutroDia.setOnClickListener {
            val currentCalendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val date = Calendar.getInstance()
                    date.set(year, month, day)
                    dataSelecionada = formatarData(date)
                    updateDateLabel(date)
                    carregarHorarios()
                },
                currentCalendar.get(Calendar.YEAR),
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateLabel(cal: Calendar) {
        val sdf = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        selectedDate.text = sdf.format(cal.time)
    }

    private fun formatarData(cal: Calendar): String {
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val y = cal.get(Calendar.YEAR)
        return "$d/$m/$y"
    }

    /** HORÁRIOS **/
    private fun carregarHorarios() {
        val todosHorarios = listOf(
            "08:00", "09:00", "10:00",
            "11:00", "12:00", "13:00",
            "14:00", "15:00", "16:00",
            "17:00"
        )

        val hoje = Calendar.getInstance()
        val sdfCompare = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val dataHojeString = sdfCompare.format(hoje.time)

        val horariosFiltrados = if (dataSelecionada == dataHojeString) {
            val horaAtual = hoje.get(Calendar.HOUR_OF_DAY)
            todosHorarios.filter { horario ->
                val horaDoSlot = horario.substringBefore(':').toInt()
                horaDoSlot > horaAtual
            }
        } else {
            todosHorarios
        }

        timeAdapter.updateList(horariosFiltrados)
    }

    /** CONFIRMAR **/
    private fun confirmarAgendamento() {
        val userId = auth.currentUser?.uid
        if (dataSelecionada.isEmpty() || horarioSelecionado.isEmpty()) {
            Toast.makeText(requireContext(), "Selecione data e horário", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == null) {
            Toast.makeText(requireContext(), "Você precisa estar logado para agendar", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("usuarios").document(userId).get().addOnSuccessListener { document ->
            var nomeUsuario = document.getString("nome") ?: document.getString("name")
            if (nomeUsuario.isNullOrEmpty()) {
                nomeUsuario = "Usuário não identificado"
            }

            val dateTimeString = "$dataSelecionada $horarioSelecionado"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
            val date = try {
                sdf.parse(dateTimeString)
            } catch (e: Exception) {
                null
            }

            if (date == null) {
                Toast.makeText(requireContext(), "Formato de data ou hora inválido.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val timestamp = com.google.firebase.Timestamp(date)

            val agendamento = Agendamento(
                userId = userId,
                salaoId = salaoId,
                nomeUsuario = nomeUsuario,
                nomeSalao = salonName.text.toString(),
                data = timestamp,
                servico = serviceName.text.toString(),
                status = "Confirmado"
            )

            db.collection("agendamentos").add(agendamento)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Agendamento confirmado!", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.navigation_home)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erro ao criar agendamento: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Erro ao buscar dados do usuário: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}