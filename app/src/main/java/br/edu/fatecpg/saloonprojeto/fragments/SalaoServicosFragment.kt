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
    private var salaoId: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Header Views
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    // Salao Info Views
    private lateinit var salaoImage: ImageView
    private lateinit var salaoName: TextView
    private lateinit var salaoAddress: TextView

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

        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        val salaoInfoView = view.findViewById<View>(R.id.salao_info_view)
        salaoImage = salaoInfoView.findViewById(R.id.img_salao)
        salaoName = salaoInfoView.findViewById(R.id.salon_name)
        salaoAddress = salaoInfoView.findViewById(R.id.salon_address)

        recyclerView.layoutManager = LinearLayoutManager(context)
        // Inicializa o adapter sem passar a lista
        servicoAdapter = ServicoClienteAdapter { servicoId ->
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
        loadSalaoInfo()
        carregarServicos()
    }

    private fun loadHeaderInfo() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("SalaoServicosFragment", "Usuário não logado, o cabeçalho não pode ser carregado.")
            return
        }

        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists() && isAdded) {
                    val name = document.getString("nome") ?: "Usuário"
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
                Log.e("SalaoServicosFragment", "Erro ao carregar informações do cabeçalho do usuário", exception)
            }
    }

    private fun loadSalaoInfo() {
        if (salaoId == null) {
            Log.e("SalaoServicosFragment", "ID do salão não fornecido, não é possível carregar informações.")
            return
        }

        db.collection("usuarios").document(salaoId!!).get()
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
                Log.e("SalaoServicosFragment", "Erro ao carregar informações do salão", exception)
            }
    }

    private fun carregarServicos() {
        if (salaoId == null) {
            Toast.makeText(context, "ID do salão não fornecido", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("servicos")
            .whereEqualTo("salaoId", salaoId)
            .whereEqualTo("ativo", true) // Garante que apenas serviços ativos sejam mostrados
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Erro ao carregar serviços", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val tempList = mutableListOf<HashMap<String, Any>>()
                if (snapshots != null) {
                    for (doc in snapshots.documents) {
                        val servico = doc.data as HashMap<String, Any>
                        servico["id"] = doc.id
                        tempList.add(servico)
                    }
                    // Apenas entrega a nova lista para o adapter
                    servicoAdapter.updateList(tempList)
                }
            }
    }
}
