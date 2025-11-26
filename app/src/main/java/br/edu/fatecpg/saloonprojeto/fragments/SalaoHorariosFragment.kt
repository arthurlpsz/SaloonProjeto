package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoHorariosFragment : Fragment() {

    private lateinit var rvDias: RecyclerView
    private lateinit var btnSalvar: Button
    private lateinit var ivBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val dias = listOf(
        "Segunda", "Terça", "Quarta",
        "Quinta", "Sexta", "Sábado", "Domingo"
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
        ivBack = view.findViewById(R.id.iv_back)

        rvDias.layoutManager = LinearLayoutManager(requireContext())
        rvDias.adapter = HorarioDiaAdapter(dias)

        btnSalvar.setOnClickListener { salvarHorarios() }

        ivBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        carregarHorarios()
    }

    private fun carregarHorarios() {
        val salaoId = auth.currentUser?.uid ?: return

        db.collection("usuarios").document(salaoId).get()
            .addOnSuccessListener { doc ->
                val horarios = doc.get("workingHours") as? Map<*, *> ?: return@addOnSuccessListener

                for (i in dias.indices) {
                    val holder = rvDias.findViewHolderForAdapterPosition(i)
                            as? HorarioDiaAdapter.HorarioDiaViewHolder ?: continue

                    val nomeDia = dias[i].lowercase()
                    val bloco = horarios[nomeDia] as? Map<*, *> ?: continue

                    val aberto = (bloco["fechado"] as? Boolean == false)

                    if (!aberto) {
                        holder.checkFechado.isChecked = true
                        continue
                    }

                    val abre = (bloco["abre"] as? String) ?: ""
                    val fecha = (bloco["fecha"] as? String) ?: ""

                    holder.edtAbre.setText(abre)
                    holder.edtFecha.setText(fecha)
                }
            }
    }

    private fun salvarHorarios() {
        val salaoId = auth.currentUser?.uid ?: return

        val dados = hashMapOf<String, Any>()
        for (i in dias.indices) {

            val holder =
                rvDias.findViewHolderForAdapterPosition(i)
                        as? HorarioDiaAdapter.HorarioDiaViewHolder ?: continue

            val nomeDia = dias[i].lowercase()

            val fechado = holder.checkFechado.isChecked

            val diaInfo = if (fechado) {
                mapOf("fechado" to true)
            } else {
                mapOf(
                    "fechado" to false,
                    "abre" to holder.edtAbre.text.toString().trim(),
                    "fecha" to holder.edtFecha.text.toString().trim()
                )
            }

            dados[nomeDia] = diaInfo
        }

        db.collection("usuarios").document(salaoId)
            .update("workingHours", dados)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Horários atualizados!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar!", Toast.LENGTH_SHORT).show()
            }
    }

    // ADAPTER INTERNO (usa item_dia_horario.xml)
    class HorarioDiaAdapter(
        private val dias: List<String>
    ) : RecyclerView.Adapter<HorarioDiaAdapter.HorarioDiaViewHolder>() {

        class HorarioDiaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDia: TextView = view.findViewById(R.id.tv_dia)
            val edtAbre: EditText = view.findViewById(R.id.edt_abre)
            val edtFecha: EditText = view.findViewById(R.id.edt_fecha)
            val checkFechado: CheckBox = view.findViewById(R.id.check_fechado)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioDiaViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dia_horario, parent, false)
            return HorarioDiaViewHolder(view)
        }

        override fun getItemCount() = dias.size

        override fun onBindViewHolder(holder: HorarioDiaViewHolder, position: Int) {
            holder.tvDia.text = dias[position]
        }
    }
}
