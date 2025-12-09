package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.ServicoClienteAdapter
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoServicosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var servicoAdapter: ServicoClienteAdapter
    private lateinit var ivBack: ImageView
    private val listaServicos = mutableListOf<HashMap<String, Any>>()
    private var salaoId: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Header Views
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            salaoId = it.getString("salaoId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_servicos, container, false)

        recyclerView = view.findViewById(R.id.services_recycler_view)
        ivBack = view.findViewById(R.id.ic_voltar)

        // Acessa as views do cabeçalho incluído
        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        recyclerView.layoutManager = LinearLayoutManager(context)
        servicoAdapter = ServicoClienteAdapter(listaServicos) { servicoId ->
            val bundle = Bundle()
            bundle.putString("salaoId", salaoId)
            bundle.putString("servicoId", servicoId)
            findNavController().navigate(R.id.action_salaoServicos_to_agendar, bundle)
        }
        recyclerView.adapter = servicoAdapter

        ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHeaderInfo()
        carregarServicos()
    }

    private fun loadHeaderInfo() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("SalaoServicosFragment", "Usuário não logado, o cabeçalho não pode ser carregado.")
            return
        }

        // Busca os dados do usuário logado para exibir no cabeçalho.
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists() && isAdded) {
                    // Usa "nome" do usuário para o texto.
                    val name = document.getString("nome") ?: "Usuário"
                    userName.text = name

                    // Usa "fotoUrl" do usuário para a imagem.
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
                Log.e("SalaoServicosFragment", "Erro ao carregar informações do cabeçalho do usuário", exception)
            }
    }

    private fun carregarServicos() {
        if (salaoId == null) {
            Toast.makeText(context, "ID do salão não fornecido", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("servicos")
            .whereEqualTo("salaoId", salaoId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Erro ao carregar serviços", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                listaServicos.clear()
                if (snapshots != null) {
                    for (doc in snapshots.documents) {
                        val servico = doc.data as HashMap<String, Any>
                        servico["id"] = doc.id
                        listaServicos.add(servico)
                    }
                    servicoAdapter.updateList(listaServicos)
                }
            }
    }
}
