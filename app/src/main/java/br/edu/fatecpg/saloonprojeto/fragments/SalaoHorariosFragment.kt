package br.edu.fatecpg.saloonprojeto.fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import br.edu.fatecpg.saloonprojeto.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class SalaoHorariosFragment : Fragment() {

    private lateinit var btnOpenTime: MaterialButton
    private lateinit var btnCloseTime: MaterialButton
    private lateinit var btnSalvar: MaterialButton

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var horarioInicio: String? = null
    private var horarioFim: String? = null

    private var salaoId: String? = null // Pode ser o UID do salão

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_horarios, container, false)

        btnOpenTime = view.findViewById(R.id.btn_open_time)
        btnCloseTime = view.findViewById(R.id.btn_close_time)
        btnSalvar = view.findViewById(R.id.btn_salvar_horarios)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        salaoId = auth.currentUser?.uid // simplificação: o UID é o ID do salão

        btnOpenTime.setOnClickListener { showTimePicker(true) }
        btnCloseTime.setOnClickListener { showTimePicker(false) }
        btnSalvar.setOnClickListener { salvarHorarios() }

        carregarHorariosExistentes()

        return view
    }

    private fun showTimePicker(isOpening: Boolean) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val picker = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                if (isOpening) {
                    horarioInicio = timeString
                    btnOpenTime.text = "Abertura: $timeString"
                } else {
                    horarioFim = timeString
                    btnCloseTime.text = "Fechamento: $timeString"
                }
            },
            hour,
            minute,
            true
        )
        picker.show()
    }

    private fun salvarHorarios() {
        val id = salaoId ?: return
        val inicio = horarioInicio
        val fim = horarioFim

        if (inicio.isNullOrEmpty() || fim.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Selecione os dois horários", Toast.LENGTH_SHORT).show()
            return
        }

        val horarios = mapOf(
            "horarioInicio" to inicio,
            "horarioFim" to fim
        )

        db.collection("saloes").document(id)
            .set(horarios, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Horários salvos com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar horários.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun carregarHorariosExistentes() {
        val id = salaoId ?: return
        db.collection("saloes").document(id).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    horarioInicio = doc.getString("horarioInicio")
                    horarioFim = doc.getString("horarioFim")

                    btnOpenTime.text = "Abertura: ${horarioInicio ?: "Não definido"}"
                    btnCloseTime.text = "Fechamento: ${horarioFim ?: "Não definido"}"
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar horários", Toast.LENGTH_SHORT).show()
            }
    }
}
