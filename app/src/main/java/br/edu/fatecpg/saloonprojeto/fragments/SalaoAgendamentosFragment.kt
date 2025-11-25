package br.edu.fatecpg.saloonprojeto.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.AgendamentoAdapter
import br.edu.fatecpg.saloonprojeto.adapter.AgendamentoViewType
import br.edu.fatecpg.saloonprojeto.adapter.ListItem
import br.edu.fatecpg.saloonprojeto.model.Agendamento
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoAgendamentosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AgendamentoAdapter
    private val listItems = mutableListOf<ListItem>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_agendamentos, container, false)
        recyclerView = view.findViewById(R.id.salao_bookings_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = AgendamentoAdapter(listItems, AgendamentoViewType.SALAO) { agendamento ->
            cancelarAgendamento(agendamento)
        }
        recyclerView.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        carregarAgendamentos()
    }

    private fun carregarAgendamentos() {
        val salaoId = auth.currentUser?.uid ?: return

        db.collection("agendamentos")
            .whereEqualTo("salaoId", salaoId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                listItems.clear()
                val agendamentos = snapshots?.map { it.toObject(Agendamento::class.java).copy(id = it.id) } ?: emptyList()

                val proximos = agendamentos.filter { it.status == "Confirmado" }
                val historico = agendamentos.filter { it.status == "Cancelado" || it.status == "Realizado" }

                if (proximos.isNotEmpty()) {
                    listItems.add(ListItem.HeaderItem("Próximos Agendamentos"))
                    proximos.forEach { listItems.add(ListItem.AgendamentoItem(it)) }
                }

                if (historico.isNotEmpty()) {
                    listItems.add(ListItem.HeaderItem("Histórico"))
                    historico.forEach { listItems.add(ListItem.AgendamentoItem(it)) }
                }

                adapter.notifyDataSetChanged()
            }
    }

    private fun cancelarAgendamento(agendamento: Agendamento) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar Agendamento")
            .setMessage("Você tem certeza que deseja cancelar este agendamento?")
            .setPositiveButton("Sim") { _, _ ->
                db.collection("agendamentos").document(agendamento.id)
                    .update("status", "Cancelado")
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Agendamento cancelado com sucesso", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Erro ao cancelar o agendamento: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Não", null)
            .show()
    }
}