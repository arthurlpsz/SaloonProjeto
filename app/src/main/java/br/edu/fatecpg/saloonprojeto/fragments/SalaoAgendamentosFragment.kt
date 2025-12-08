package br.edu.fatecpg.saloonprojeto.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.AgendamentoAdapter
import br.edu.fatecpg.saloonprojeto.adapter.AgendamentoViewType
import br.edu.fatecpg.saloonprojeto.adapter.ListItem
import br.edu.fatecpg.saloonprojeto.model.Agendamento
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoAgendamentosFragment : Fragment() {

    private lateinit var rvPendentes: RecyclerView
    private lateinit var rvHistorico: RecyclerView
    private lateinit var adapterPendentes: AgendamentoAdapter
    private lateinit var adapterHistorico: AgendamentoAdapter
    private val listItemsPendentes = mutableListOf<ListItem>()
    private val listItemsHistorico = mutableListOf<ListItem>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_agendamentos, container, false)

        rvPendentes = view.findViewById(R.id.rv_pendentes_salao)
        rvHistorico = view.findViewById(R.id.rv_historico_salao)
        rvPendentes.layoutManager = LinearLayoutManager(context)
        rvHistorico.layoutManager = LinearLayoutManager(context)

        rvPendentes.isNestedScrollingEnabled = false
        rvHistorico.isNestedScrollingEnabled = false

        adapterPendentes = AgendamentoAdapter(listItemsPendentes, AgendamentoViewType.SALAO) { agendamento ->
            cancelarAgendamento(agendamento)
        }
        adapterHistorico = AgendamentoAdapter(listItemsHistorico, AgendamentoViewType.SALAO) { agendamento ->
            // O botão de cancelar não deve aparecer no histórico, mas mantemos por consistência
        }

        rvPendentes.adapter = adapterPendentes
        rvHistorico.adapter = adapterHistorico

        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHeaderInfo()
        carregarAgendamentos()
    }

    private fun loadHeaderInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() && isAdded) {
                        val name = document.getString("nomeSalao") ?: "Salão sem nome"
                        userName.text = name

                        val imageUrl = document.getString("fotoUrl") // Padronizado para "fotoUrl"
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(profileImage)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("SalaoAgendamentos", "Erro ao carregar informações do cabeçalho", exception)
                }
        }
    }

    private fun carregarAgendamentos() {
        val salaoId = auth.currentUser?.uid ?: return

        db.collection("agendamentos")
            .whereEqualTo("salaoId", salaoId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("SalaoAgendamentos", "Erro ao ouvir agendamentos.", e)
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    return@addSnapshotListener
                }

                val agendamentos = snapshots.map { it.toObject(Agendamento::class.java).copy(id = it.id) }

                listItemsPendentes.clear()
                listItemsHistorico.clear()

                val pendentes = agendamentos.filter { it.status == "Confirmado" }.sortedBy { it.dataInicio }
                val historico = agendamentos.filter { it.status == "Cancelado" || it.status == "Realizado" }.sortedByDescending { it.dataInicio }

                if (pendentes.isEmpty()) {
                    listItemsPendentes.add(ListItem.EmptyItem)
                } else {
                    pendentes.forEach { listItemsPendentes.add(ListItem.AgendamentoItem(it)) }
                }

                if (historico.isEmpty()) {
                    listItemsHistorico.add(ListItem.EmptyItem)
                } else {
                    historico.forEach { listItemsHistorico.add(ListItem.AgendamentoItem(it)) }
                }

                if (isAdded) {
                    adapterPendentes.notifyDataSetChanged()
                    adapterHistorico.notifyDataSetChanged()
                }
            }
    }

    private fun cancelarAgendamento(agendamento: Agendamento) {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar Agendamento")
            .setMessage("Você tem certeza que deseja cancelar este agendamento?")
            .setPositiveButton("Sim") { _, _ ->
                db.collection("agendamentos").document(agendamento.id)
                    .update("status", "Cancelado")
                    .addOnSuccessListener {
                        if(isAdded) Toast.makeText(requireContext(), "Agendamento cancelado com sucesso", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        if(isAdded) Toast.makeText(requireContext(), "Erro ao cancelar o agendamento: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Não", null)
            .show()
    }
}
