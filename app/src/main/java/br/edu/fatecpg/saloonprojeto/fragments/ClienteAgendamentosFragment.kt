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

class ClienteAgendamentosFragment : Fragment() {

    // Lists and Adapters for the two RecyclerViews
    private lateinit var rvPendentes: RecyclerView
    private lateinit var rvConcluidos: RecyclerView
    private lateinit var adapterPendentes: AgendamentoAdapter
    private lateinit var adapterConcluidos: AgendamentoAdapter
    private val listItemsPendentes = mutableListOf<ListItem>()
    private val listItemsConcluidos = mutableListOf<ListItem>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Header Views
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cliente_agendamentos, container, false)

        // Initialize RecyclerViews
        rvPendentes = view.findViewById(R.id.rv_pendentes)
        rvConcluidos = view.findViewById(R.id.rv_concluidos)
        rvPendentes.layoutManager = LinearLayoutManager(context)
        rvConcluidos.layoutManager = LinearLayoutManager(context)

        // Prevent nested scrolling issues
        rvPendentes.isNestedScrollingEnabled = false
        rvConcluidos.isNestedScrollingEnabled = false

        // Initialize Adapters
        adapterPendentes = AgendamentoAdapter(listItemsPendentes, AgendamentoViewType.CLIENTE) { agendamento ->
            cancelarAgendamento(agendamento)
        }
        adapterConcluidos = AgendamentoAdapter(listItemsConcluidos, AgendamentoViewType.CLIENTE) { agendamento ->
            cancelarAgendamento(agendamento)
        }

        rvPendentes.adapter = adapterPendentes
        rvConcluidos.adapter = adapterConcluidos

        // Initialize Header
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
                        val name = document.getString("nome")
                        userName.text = name ?: "Nome não encontrado"

                        val imageUrl = document.getString("fotoUrl")
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(profileImage)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ClienteAgendamentos", "Erro ao carregar informações do cabeçalho", exception)
                }
        }
    }

    private fun carregarAgendamentos() {
        val clienteId = auth.currentUser?.uid ?: return

        db.collection("agendamentos")
            .whereEqualTo("clienteId", clienteId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ClienteAgendamentos", "Erro ao ouvir agendamentos.", e)
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.d("ClienteAgendamentos", "Snapshot nulo.")
                    return@addSnapshotListener
                }

                val agendamentos = mutableListOf<Agendamento>()
                for (document in snapshots.documents) {
                    document.toObject(Agendamento::class.java)?.let {
                        it.id = document.id
                        agendamentos.add(it)
                    }
                }

                // Clear lists before populating
                listItemsPendentes.clear()
                listItemsConcluidos.clear()

                val pendentes = agendamentos.filter { it.status == "Confirmado" }.sortedBy { it.dataInicio }
                val concluidos = agendamentos.filter { it.status == "Cancelado" || it.status == "Realizado" }.sortedByDescending { it.dataInicio }

                // Populate pendentes list
                if (pendentes.isEmpty()) {
                    listItemsPendentes.add(ListItem.EmptyItem)
                } else {
                    pendentes.forEach { listItemsPendentes.add(ListItem.AgendamentoItem(it)) }
                }

                // Populate concluídos list
                if (concluidos.isEmpty()) {
                    listItemsConcluidos.add(ListItem.EmptyItem)
                } else {
                    concluidos.forEach { listItemsConcluidos.add(ListItem.AgendamentoItem(it)) }
                }

                if (isAdded) {
                    adapterPendentes.notifyDataSetChanged()
                    adapterConcluidos.notifyDataSetChanged()
                    Log.d("ClienteAgendamentos", "Adapters notificados.")
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
                        if (isAdded) Toast.makeText(context, "Agendamento cancelado com sucesso", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        if (isAdded) Toast.makeText(context, "Erro ao cancelar: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Não", null)
            .show()
    }
}
