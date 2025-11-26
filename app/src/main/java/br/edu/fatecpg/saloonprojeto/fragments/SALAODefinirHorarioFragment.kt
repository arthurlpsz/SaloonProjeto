package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapters.DiaHorario
import br.edu.fatecpg.saloonprojeto.adapters.HorarioAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SALAODefinirHorarioFragment : Fragment() {

    private lateinit var recyclerHorarios: RecyclerView
    private lateinit var btnSalvar: Button

    private lateinit var adapter: HorarioAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // IMPORTANTE → aqui vai o seu layout correto!
        return inflater.inflate(R.layout.fragment_definir_horarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerHorarios = view.findViewById(R.id.recycler_horarios)
        btnSalvar = view.findViewById(R.id.btn_salvar_horarios)

        // Lista para o adapter
        val dias = listOf(
            DiaHorario("Segunda-feira"),
            DiaHorario("Terça-feira"),
            DiaHorario("Quarta-feira"),
            DiaHorario("Quinta-feira"),
            DiaHorario("Sexta-feira"),
            DiaHorario("Sábado"),
            DiaHorario("Domingo")
        )

        adapter = HorarioAdapter(dias)

        recyclerHorarios.layoutManager = LinearLayoutManager(requireContext())
        recyclerHorarios.adapter = adapter

        btnSalvar.setOnClickListener {
            salvarHorarios()
        }
    }

    private fun salvarHorarios() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Erro: usuário não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val dados = adapter.getHorarios().associate { dia ->
            dia.dia to mapOf(
                "abre" to dia.abre,
                "fecha" to dia.fecha,
                "fechado" to dia.fechado
            )
        }

        db.collection("horarios_salao")
            .document(userId)
            .set(dados)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Horários salvos!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao salvar.", Toast.LENGTH_SHORT).show()
            }
    }
}
