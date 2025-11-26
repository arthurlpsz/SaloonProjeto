package br.edu.fatecpg.saloonprojeto.fragments

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

class DefinirHorariosFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var container: LinearLayout
    private val diasSemana = listOf(
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
    }

    private fun criarCamposParaCadaDia() {
        val inflater = LayoutInflater.from(requireContext())

        diasSemana.forEach { dia ->
            val item = inflater.inflate(R.layout.item_dia_horario, container, false)

            item.tag = dia
            item.findViewById<TextView>(R.id.tv_dia).text = dia.replaceFirstChar { it.uppercase() }

            container.addView(item)
        }
    }

    private fun carregarHorarios() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(userId)
            .collection("horarios")
            .document("semana")
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                diasSemana.forEach { dia ->
                    val dados = doc.get(dia) as? Map<String, Any> ?: return@forEach
                    val item = container.findViewWithTag<View>(dia)

                    val edtAbre = item.findViewById<EditText>(R.id.edt_abre)
                    val edtFecha = item.findViewById<EditText>(R.id.edt_fecha)
                    val check = item.findViewById<CheckBox>(R.id.check_fechado)

                    edtAbre.setText(dados["abre"]?.toString() ?: "")
                    edtFecha.setText(dados["fecha"]?.toString() ?: "")
                    check.isChecked = dados["fechado"] as? Boolean ?: false
                }
            }
    }

    private fun salvarHorarios() {
        val userId = auth.currentUser?.uid ?: return

        val dadosSemana = hashMapOf<String, Any>()

        diasSemana.forEach { dia ->
            val item = container.findViewWithTag<View>(dia)
            val abre = item.findViewById<EditText>(R.id.edt_abre).text.toString()
            val fecha = item.findViewById<EditText>(R.id.edt_fecha).text.toString()
            val fechado = item.findViewById<CheckBox>(R.id.check_fechado).isChecked

            dadosSemana[dia] = hashMapOf(
                "abre" to abre,
                "fecha" to fecha,
                "fechado" to fechado
            )
        }

        db.collection("usuarios").document(userId)
            .collection("horarios")
            .document("semana")
            .set(dadosSemana)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Horários salvos!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar!", Toast.LENGTH_SHORT).show()
            }
    }
}
