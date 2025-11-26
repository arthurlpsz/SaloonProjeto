package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapters.DiaHorario
import br.edu.fatecpg.saloonprojeto.adapters.HorarioAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoHorariosFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: HorarioAdapter

    private val diasSemana = listOf(
        "Segunda-feira",
        "Terça-feira",
        "Quarta-feira",
        "Quinta-feira",
        "Sexta-feira",
        "Sábado",
        "Domingo"
    )

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_salao_horarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid

        val back = view.findViewById<ImageView>(R.id.iv_back)
        back.setOnClickListener { findNavController().navigateUp() }

        recycler = view.findViewById(R.id.rv_horarios)

        adapter = HorarioAdapter(diasSemana.map { DiaHorario(it) })
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val btnSalvar = view.findViewById<Button>(R.id.btn_salvar_horarios)
        btnSalvar.setOnClickListener {
            salvarHorarios()
        }

        carregarHorarios()
    }

    private fun carregarHorarios() {
        userId?.let { uid ->
            db.collection("usuarios").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.contains("horarios")) {
                        val horarios = doc.get("horarios") as Map<String, Map<String, Any>>

                        horarios.forEach { (dia, dados) ->
                            val item = adapter.getHorarios().find { it.dia == dia }
                            if (item != null) {
                                item.abre = dados["abre"] as? String ?: ""
                                item.fecha = dados["fecha"] as? String ?: ""
                                item.fechado = dados["fechado"] as? Boolean ?: false
                            }
                        }

                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    private fun salvarHorarios() {
        val horariosFirestore = hashMapOf<String, Any>()

        adapter.getHorarios().forEach {
            horariosFirestore[it.dia] = hashMapOf(
                "abre" to it.abre,
                "fecha" to it.fecha,
                "fechado" to it.fechado
            )
        }

        userId?.let { uid ->
            db.collection("usuarios").document(uid)
                .update("horarios", horariosFirestore)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Horários salvos!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro ao salvar horários", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
