package br.edu.fatecpg.saloonprojeto.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class DefinirHorariosFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var container: LinearLayout
    private val diasSemanaDisplay = listOf(
        "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"
    )
    private val diasSemanaKeys = listOf(
        "segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_definir_horarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        container = view.findViewById(R.id.container_horarios)

        criarCamposParaCadaDia()
        carregarHorarios()

        view.findViewById<Button>(R.id.btn_salvar_horarios).setOnClickListener {
            salvarHorarios()
        }

        view.findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun criarCamposParaCadaDia() {
        val inflater = LayoutInflater.from(requireContext())

        diasSemanaKeys.forEachIndexed { index, diaKey ->
            val item = inflater.inflate(R.layout.item_dia_horario, container, false)

            item.tag = diaKey
            item.findViewById<TextView>(R.id.tv_dia).text = diasSemanaDisplay[index]

            val tvAbre = item.findViewById<TextView>(R.id.tv_abre)
            val tvFecha = item.findViewById<TextView>(R.id.tv_fecha)
            val checkFechado = item.findViewById<CheckBox>(R.id.check_fechado)

            checkFechado.setOnCheckedChangeListener { _, isChecked ->
                tvAbre.isEnabled = !isChecked
                tvFecha.isEnabled = !isChecked
                tvAbre.alpha = if(isChecked) 0.5f else 1.0f
                tvFecha.alpha = if(isChecked) 0.5f else 1.0f
                if (isChecked) {
                    tvAbre.text = getString(R.string.abre_hint)
                    tvFecha.text = getString(R.string.fecha_hint)
                }
            }

            val timeSetListener = { textView: TextView ->
                View.OnClickListener {
                    if (!textView.isEnabled) return@OnClickListener
                    val cal = Calendar.getInstance()
                    val time = parseTextToMin(textView.text.toString())
                    val currentHour = if (time != -1) time / 60 else cal.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = if (time != -1) time % 60 else cal.get(Calendar.MINUTE)

                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            textView.text = String.format("%02d:%02d", hourOfDay, minute)
                        },
                        currentHour,
                        currentMinute,
                        true
                    ).show()
                }
            }

            tvAbre.setOnClickListener(timeSetListener(tvAbre))
            tvFecha.setOnClickListener(timeSetListener(tvFecha))

            container.addView(item)
        }
    }

    private fun carregarHorarios() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val horarios = doc.get("workingHours") as? Map<String, Any> ?: return@addOnSuccessListener

                diasSemanaKeys.forEach { diaKey ->
                    val item = container.findViewWithTag<View>(diaKey) ?: return@forEach
                    val dados = horarios[diaKey] as? Map<String, Any> ?: return@forEach

                    val tvAbre = item.findViewById<TextView>(R.id.tv_abre)
                    val tvFecha = item.findViewById<TextView>(R.id.tv_fecha)
                    val check = item.findViewById<CheckBox>(R.id.check_fechado)

                    val fechado = dados["fechado"] as? Boolean ?: true
                    check.isChecked = fechado

                    tvAbre.isEnabled = !fechado
                    tvFecha.isEnabled = !fechado
                    tvAbre.alpha = if(fechado) 0.5f else 1.0f
                    tvFecha.alpha = if(fechado) 0.5f else 1.0f

                    if (!fechado) {
                        val start = (dados["start"] as? Long)?.toInt() ?: 0
                        val end = (dados["end"] as? Long)?.toInt() ?: 0
                        tvAbre.text = formatMinToText(start)
                        tvFecha.text = formatMinToText(end)
                    } else {
                        tvAbre.text = getString(R.string.abre_hint)
                        tvFecha.text = getString(R.string.fecha_hint)
                    }
                }
            }
    }

    private fun salvarHorarios() {
        val userId = auth.currentUser?.uid ?: return

        val dadosSemana = hashMapOf<String, Any>()

        for (i in diasSemanaKeys.indices) {
            val diaKey = diasSemanaKeys[i]
            val item = container.findViewWithTag<View>(diaKey) ?: continue
            val abreStr = item.findViewById<TextView>(R.id.tv_abre).text.toString()
            val fechaStr = item.findViewById<TextView>(R.id.tv_fecha).text.toString()
            val fechado = item.findViewById<CheckBox>(R.id.check_fechado).isChecked

            if (fechado) {
                dadosSemana[diaKey] = hashMapOf("fechado" to true)
            } else {
                val abreMin = parseTextToMin(abreStr)
                val fechaMin = parseTextToMin(fechaStr)

                if (abreMin == -1 || fechaMin == -1) {
                    Toast.makeText(requireContext(), "Defina os horários de ${diasSemanaDisplay[i]}", Toast.LENGTH_SHORT).show()
                    return
                }
                if (abreMin >= fechaMin) {
                    Toast.makeText(requireContext(), "Em ${diasSemanaDisplay[i]}, o fim deve ser após o início", Toast.LENGTH_SHORT).show()
                    return
                }
                dadosSemana[diaKey] = hashMapOf("fechado" to false, "start" to abreMin, "end" to fechaMin)
            }
        }

        db.collection("usuarios").document(userId)
            .update("workingHours", dadosSemana)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Horários salvos!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                db.collection("usuarios").document(userId).set(hashMapOf("workingHours" to dadosSemana), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Horários salvos!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
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
