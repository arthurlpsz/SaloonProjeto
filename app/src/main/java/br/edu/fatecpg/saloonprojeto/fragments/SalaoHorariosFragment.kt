package br.edu.fatecpg.saloonprojeto.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class SalaoHorariosFragment : Fragment() {

    private lateinit var rvDias: RecyclerView
    private lateinit var btnSalvar: Button
    private lateinit var ivBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val diasDisplay = listOf(
        "Segunda", "Terça", "Quarta",
        "Quinta", "Sexta", "Sábado", "Domingo"
    )
    private val diasKeys = listOf(
        "segunda", "terca", "quarta",
        "quinta", "sexta", "sabado", "domingo"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_salao_horarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvDias = view.findViewById(R.id.rv_horarios)
        btnSalvar = view.findViewById(R.id.btn_salvar_horarios)
        ivBack = view.findViewById(R.id.ic_voltar)

        rvDias.layoutManager = LinearLayoutManager(requireContext())
        rvDias.adapter = HorarioDiaAdapter(diasDisplay, diasKeys)

        btnSalvar.setOnClickListener { salvarHorarios() }

        ivBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        carregarHorarios()
    }

    private fun carregarHorarios() {
        val salaoId = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(salaoId).get()
            .addOnSuccessListener { doc ->
                val horarios = doc.get("workingHours") as? Map<*, *> ?: return@addOnSuccessListener

                (rvDias.adapter as? HorarioDiaAdapter)?.setHorarios(horarios)
            }
    }

    private fun salvarHorarios() {
        val salaoId = auth.currentUser?.uid ?: return

        val dados = (rvDias.adapter as? HorarioDiaAdapter)?.getHorarios() ?: return

        db.collection("usuarios").document(salaoId)
            .update("workingHours", dados)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Horários atualizados!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar!", Toast.LENGTH_SHORT).show()
            }
    }

    class HorarioDiaAdapter(
        private val diasDisplay: List<String>,
        private val diasKeys: List<String>
    ) : RecyclerView.Adapter<HorarioDiaAdapter.HorarioDiaViewHolder>() {

        private val horariosData = mutableMapOf<String, Map<String, Any>>()

        init {
            diasKeys.forEach { dia ->
                horariosData[dia] = mapOf("fechado" to true, "start" to 0, "end" to 0)
            }
        }

        class HorarioDiaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDia: TextView = view.findViewById(R.id.tv_dia)
            val tvAbre: TextView = view.findViewById(R.id.tv_abre)
            val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
            val checkFechado: CheckBox = view.findViewById(R.id.check_fechado)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioDiaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dia_horario, parent, false)
            return HorarioDiaViewHolder(view)
        }

        override fun getItemCount() = diasDisplay.size

        override fun onBindViewHolder(holder: HorarioDiaViewHolder, position: Int) {
            val diaKey = diasKeys[position]
            val diaData = horariosData[diaKey] ?: return

            holder.tvDia.text = diasDisplay[position]
            val fechado = diaData["fechado"] as? Boolean ?: true
            holder.checkFechado.isChecked = fechado

            updateEnabledState(holder, !fechado)

            if (!fechado) {
                holder.tvAbre.text = formatMinToText(diaData["start"] as? Int ?: 0)
                holder.tvFecha.text = formatMinToText(diaData["end"] as? Int ?: 0)
            } else {
                holder.tvAbre.text = holder.itemView.context.getString(R.string.abre_hint)
                holder.tvFecha.text = holder.itemView.context.getString(R.string.fecha_hint)
            }

            holder.checkFechado.setOnCheckedChangeListener { _, isChecked ->
                updateEnabledState(holder, !isChecked)
                val currentStart = parseTextToMin(holder.tvAbre.text.toString())
                val currentEnd = parseTextToMin(holder.tvFecha.text.toString())
                horariosData[diaKey] = mapOf("fechado" to isChecked, "start" to currentStart, "end" to currentEnd)
            }

            val timeSetListener = { textView: TextView, isStart: Boolean ->
                View.OnClickListener {
                    if (!textView.isEnabled) return@OnClickListener
                    val cal = Calendar.getInstance()
                    val time = parseTextToMin(textView.text.toString())
                    val currentHour = if (time != -1) time / 60 else cal.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = if (time != -1) time % 60 else cal.get(Calendar.MINUTE)

                    TimePickerDialog(
                        holder.itemView.context,
                        { _, hourOfDay, minute ->
                            val newTime = hourOfDay * 60 + minute
                            textView.text = formatMinToText(newTime)
                            val otherTime = parseTextToMin(if(isStart) holder.tvFecha.text.toString() else holder.tvAbre.text.toString())
                            horariosData[diaKey] = mapOf(
                                "fechado" to false,
                                "start" to if(isStart) newTime else otherTime,
                                "end" to if(isStart) otherTime else newTime
                            )
                        },
                        currentHour,
                        currentMinute,
                        true
                    ).show()
                }
            }

            holder.tvAbre.setOnClickListener(timeSetListener(holder.tvAbre, true))
            holder.tvFecha.setOnClickListener(timeSetListener(holder.tvFecha, false))
        }

        private fun updateEnabledState(holder: HorarioDiaViewHolder, isEnabled: Boolean) {
            holder.tvAbre.isEnabled = isEnabled
            holder.tvFecha.isEnabled = isEnabled
            holder.tvAbre.alpha = if(isEnabled) 1.0f else 0.5f
            holder.tvFecha.alpha = if(isEnabled) 1.0f else 0.5f
        }

        fun setHorarios(horarios: Map<*, *>) {
            diasKeys.forEach { diaKey ->
                val data = horarios[diaKey] as? Map<*, *>
                if (data != null) {
                    val fechado = data["fechado"] as? Boolean ?: true
                    val start = (data["start"] as? Long)?.toInt() ?: 0
                    val end = (data["end"] as? Long)?.toInt() ?: 0
                    horariosData[diaKey] = mapOf("fechado" to fechado, "start" to start, "end" to end)
                }
            }
            notifyDataSetChanged()
        }

        fun getHorarios(): Map<String, Map<String, Any>> {
            // Validate data before returning
            diasKeys.forEach { diaKey ->
                val data = horariosData[diaKey]!!
                val fechado = data["fechado"] as Boolean
                if (!fechado) {
                    val start = data["start"] as Int
                    val end = data["end"] as Int
                    if (start >= end) {
                        throw IllegalStateException("Em ${diasDisplay[diasKeys.indexOf(diaKey)]}, o fim deve ser após o início")
                    }
                }
            }
            return horariosData
        }

        private fun formatMinToText(min: Int): String {
            val h = min / 60
            val m = min % 60
            return String.format("%02d:%02d", h, m)
        }

        private fun parseTextToMin(text: String): Int {
            if (!text.matches(Regex("\\d{2}:\\d{2}"))) return -1
            return try {
                val (h, m) = text.split(":").map { it.toInt() }
                h * 60 + m
            } catch (e: Exception) {
                -1
            }
        }
    }
}
