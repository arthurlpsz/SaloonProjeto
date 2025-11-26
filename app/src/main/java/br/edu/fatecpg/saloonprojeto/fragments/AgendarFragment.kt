// AgendarFragment.kt
package br.edu.fatecpg.saloonprojeto.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/*
  LAYOUT EXPECTED (ajuste se necessário):
  - TextView txv_selected_date
  - Button btn_pick_date
  - Button btn_load_slots
  - RecyclerView rv_slots
  - (Recycler item layout: textview slot_time)
*/
class AgendarFragment : Fragment() {

    private lateinit var tvSelectedDate: TextView
    private lateinit var btnPickDate: Button
    private lateinit var btnLoadSlots: Button
    private lateinit var rvSlots: RecyclerView
    private var selectedDate: Calendar = Calendar.getInstance()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var salaoId: String? = null
    private var servicoId: String? = null
    private var servicoDuracaoMin: Int = 45 // default, will load real value

    private lateinit var slotsAdapter: SlotsAdapter
    private var availableSlots: MutableList<Int> = mutableListOf() // minutes of day

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

        tvSelectedDate = view.findViewById(R.id.txv_selected_date)
        btnPickDate = view.findViewById(R.id.btn_pick_date)
        btnLoadSlots = view.findViewById(R.id.btn_load_slots)
        rvSlots = view.findViewById(R.id.rv_slots)

        rvSlots.layoutManager = LinearLayoutManager(requireContext())
        slotsAdapter = SlotsAdapter { minuteOfDay ->
            attemptBooking(minuteOfDay)
        }
        rvSlots.adapter = slotsAdapter

        updateSelectedDateText()

        btnPickDate.setOnClickListener {
            pickDate()
        }

        btnLoadSlots.setOnClickListener {
            if (salaoId == null || servicoId == null) {
                Toast.makeText(requireContext(), "Dados do salão/serviço ausentes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loadServiceAndSlots()
        }

        // If nav passed a date (optional), you could set it here
    }

    private fun updateSelectedDateText() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        tvSelectedDate.text = sdf.format(selectedDate.time)
    }

    private fun pickDate() {
        val c = selectedDate
        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            selectedDate.set(year, month, dayOfMonth)
            updateSelectedDateText()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadServiceAndSlots() {
        // load service to get duration
        db.collection("servicos").document(servicoId!!)
            .get()
            .addOnSuccessListener { doc ->
                servicoDuracaoMin = doc.getLong("duracaoMin")?.toInt() ?: servicoDuracaoMin
                // now load working hours and existing appointments and compute free slots
                computeAvailableSlots()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao carregar serviço: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun computeAvailableSlots() {
        // 1) load workingHours from salao document
        db.collection("usuarios").document(salaoId!!)
            .get()
            .addOnSuccessListener { doc ->
                val working = doc.get("workingHours") as? Map<*, *>
                if (working == null) {
                    Toast.makeText(requireContext(), "Horários do salão não definidos", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val dayKey = dayKeyFromCalendar(selectedDate)
                val dayMap = working[dayKey] as? Map<*, *>
                if (dayMap == null) {
                    Toast.makeText(requireContext(), "Salão fechado neste dia", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val startMin = (dayMap["start"] as? Long)?.toInt() ?: (dayMap["start"] as? Int) ?: 0
                val endMin = (dayMap["end"] as? Long)?.toInt() ?: (dayMap["end"] as? Int) ?: 0

                // 2) load existing appointments for the same day
                val dayStart = (selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val dayEnd = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

                db.collection("agendamentos")
                    .whereEqualTo("salaoId", salaoId)
                    .whereGreaterThanOrEqualTo("dataInicio", Timestamp(dayStart.time))
                    .whereLessThan("dataInicio", Timestamp(dayEnd.time))
                    .get()
                    .addOnSuccessListener { snap ->
                        val existing = mutableListOf<Pair<Int, Int>>() // startMin,endMin (minutes of day)
                        for (d in snap.documents) {
                            val sTs = d.getTimestamp("dataInicio")?.toDate()
                            val eTs = d.getTimestamp("dataFim")?.toDate()
                            if (sTs == null || eTs == null) continue
                            val sc = Calendar.getInstance().apply { time = sTs }
                            val ec = Calendar.getInstance().apply { time = eTs }
                            val sMin = sc.get(Calendar.HOUR_OF_DAY) * 60 + sc.get(Calendar.MINUTE)
                            val eMin = ec.get(Calendar.HOUR_OF_DAY) * 60 + ec.get(Calendar.MINUTE)
                            existing.add(Pair(sMin, eMin))
                        }

                        // compute free slots at 15-minute steps
                        availableSlots.clear()
                        var cursor = startMin
                        while (cursor + servicoDuracaoMin <= endMin) {
                            val slotStart = cursor
                            val slotEnd = cursor + servicoDuracaoMin
                            val overlaps = existing.any { ap -> ap.first < slotEnd && ap.second > slotStart }
                            if (!overlaps) availableSlots.add(slotStart)
                            cursor += 15
                        }

                        // update UI
                        slotsAdapter.submitList(availableSlots.toList())
                        if (availableSlots.isEmpty()) {
                            Toast.makeText(requireContext(), "Nenhum horário disponível neste dia", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erro ao carregar agendamentos: ${e.message}", Toast.LENGTH_LONG).show()
                    }

            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao carregar horário do salão: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun attemptBooking(minuteOfDay: Int) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Usuário não autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingCalStart = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTs = Timestamp(bookingCalStart.time)
        val endMs = bookingCalStart.timeInMillis + servicoDuracaoMin * 60_000L
        val endTs = Timestamp(Date(endMs))

        // final overlap check and booking creation (atomic enough for this scope)
        // reuse same overlap check pattern as computeAvailableSlots but for the exact interval
        val dayStart = (selectedDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dayEnd = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }

        db.collection("agendamentos")
            .whereEqualTo("salaoId", salaoId)
            .whereGreaterThanOrEqualTo("dataInicio", Timestamp(dayStart.time))
            .whereLessThan("dataInicio", Timestamp(dayEnd.time))
            .get()
            .addOnSuccessListener { snap ->
                for (d in snap.documents) {
                    val sTs = d.getTimestamp("dataInicio")?.toDate()?.time ?: continue
                    val eTs = d.getTimestamp("dataFim")?.toDate()?.time ?: continue
                    if (startTs.toDate().time < eTs && sTs < endTs.toDate().time) {
                        Toast.makeText(requireContext(), "Horário já reservado (verifique antes de confirmar)", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }
                }

                // create appointment
                val newAg = hashMapOf(
                    "salaoId" to salaoId,
                    "clienteId" to userId,
                    "servicoId" to servicoId,
                    "dataInicio" to startTs,
                    "dataFim" to endTs,
                    "status" to "agendado",
                    "createdAt" to Timestamp.now()
                )
                db.collection("agendamentos").add(newAg)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Agendamento criado com sucesso!", Toast.LENGTH_LONG).show()
                        // optional: navigate back or to "meus agendamentos"
                        requireActivity().onBackPressed()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erro ao criar agendamento: ${e.message}", Toast.LENGTH_LONG).show()
                    }

            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao checar conflitos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun dayKeyFromCalendar(c: Calendar): String {
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            Calendar.SATURDAY -> "saturday"
            else -> "sunday"
        }
    }

    // --- Adapter for slots (simple) ---
    class SlotsAdapter(private val onClick: (Int) -> Unit) : RecyclerView.Adapter<SlotsAdapter.VH>() {
        private var list: List<Int> = emptyList()
        private val fmt = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

        fun submitList(newList: List<Int>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val mins = list[position]
            val hour = mins / 60
            val minute = mins % 60
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            holder.tv.text = fmt.format(cal.time)
            holder.itemView.setOnClickListener { onClick(mins) }
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tv: TextView = view.findViewById(android.R.id.text1)
        }
    }
}
