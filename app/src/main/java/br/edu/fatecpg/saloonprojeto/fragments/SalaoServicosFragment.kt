package br.edu.fatecpg.saloonprojeto.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.ServicoAdapter
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore

class SalaoServicosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var servicoAdapter: ServicoAdapter
    private lateinit var ivBack: ImageView
    private lateinit var profileImage: ImageView
    private val listaServicos = mutableListOf<HashMap<String, Any>>()
    private var salaoId: String? = null

    private val db = FirebaseFirestore.getInstance()
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            salaoId = it.getString("salaoId")
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                // Use Glide to load the image into the ImageView
                Glide.with(this)
                    .load(it)
                    .into(profileImage)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_servicos, container, false)

        recyclerView = view.findViewById(R.id.services_recycler_view)
        ivBack = view.findViewById(R.id.iv_back)
        val headerView = view.findViewById<View>(R.id.header_view)
        val profileImageCard = headerView.findViewById<MaterialCardView>(R.id.mcv_profile_image)
        profileImage = headerView.findViewById(R.id.profile_image)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // false para a visão do cliente
        servicoAdapter = ServicoAdapter(listaServicos, db, false)
        recyclerView.adapter = servicoAdapter

        ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        profileImageCard.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        carregarServicos()

        return view
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
                    snapshots.documents.forEach { doc ->
                        val servico = doc.data as HashMap<String, Any>
                        servico["id"] = doc.id
                        listaServicos.add(servico)
                    }
                    servicoAdapter.notifyDataSetChanged()
                }
            }
    }
}
