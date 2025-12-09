package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.ServicoAdapter
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class SalaoDashboardFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var servicoAdapter: ServicoAdapter
    private val servicosList = mutableListOf<HashMap<String, Any>>()
    private var servicosListener: ListenerRegistration? = null

    // Views do Header
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    // Salao Info Views
    private lateinit var salaoImage: ImageView
    private lateinit var salaoName: TextView
    private lateinit var salaoAddress: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_salao, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        val salaoInfoView = view.findViewById<View>(R.id.salao_info_view)
        salaoImage = salaoInfoView.findViewById(R.id.img_salao)
        salaoName = salaoInfoView.findViewById(R.id.salon_name)
        salaoAddress = salaoInfoView.findViewById(R.id.salon_address)

        val recyclerView = view.findViewById<RecyclerView>(R.id.services_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        servicoAdapter = ServicoAdapter(servicosList, db, true) // true para a visão do salão
        recyclerView.adapter = servicoAdapter

        val fabAdicionarServico = view.findViewById<FloatingActionButton>(R.id.fab_add_servico)
        fabAdicionarServico.setOnClickListener {
            findNavController().navigate(R.id.action_salaoDashboard_to_adicionarServico)
        }

        val fabDefinirHorario = view.findViewById<FloatingActionButton>(R.id.fab_definir_horarios)
        fabDefinirHorario.setOnClickListener {
            findNavController().navigate(R.id.action_salaoDashboard_to_definirHorario)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHeaderInfo()
        loadSalaoInfo()
    }

    override fun onStart() {
        super.onStart()
        setupServicesListener()
    }

    override fun onStop() {
        super.onStop()
        servicosListener?.remove()
    }

    private fun loadHeaderInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() && isAdded) {
                        val name = document.getString("nomeSalao") ?: "Salão sem nome"
                        userName.text = name

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
                    Log.e("SalaoDashboardFragment", "Erro ao carregar informações do cabeçalho", exception)
                }
        }
    }

    private fun loadSalaoInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() && isAdded) {
                        salaoName.text = document.getString("nomeSalao") ?: "Salão não encontrado"
                        salaoAddress.text = document.getString("endereco") ?: "Endereço não disponível"

                        val imageUrl = document.getString("fotoUrl")
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.salao_feminino) // Imagem padrão
                            .error(R.drawable.salao_feminino)       // Imagem em caso de erro
                            .centerCrop()
                            .into(salaoImage)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("SalaoDashboardFragment", "Erro ao carregar informações do salão", exception)
                }
        }
    }

    private fun setupServicesListener() {
        val salaoId = auth.currentUser?.uid
        if (salaoId == null) {
            Log.e("SalaoDashboardFragment", "ID do salão não encontrado.")
            return
        }

        servicosListener = db.collection("servicos")
            .whereEqualTo("salaoId", salaoId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("SalaoDashboardFragment", "Erro ao ouvir os serviços.", e)
                    return@addSnapshotListener
                }

                val tempList = mutableListOf<HashMap<String, Any>>()
                if (snapshots != null) {
                    for (document in snapshots.documents) {
                        val servico = document.data as HashMap<String, Any>
                        servico["id"] = document.id
                        tempList.add(servico)
                    }
                }
                servicoAdapter.updateList(tempList)
            }
    }
}
