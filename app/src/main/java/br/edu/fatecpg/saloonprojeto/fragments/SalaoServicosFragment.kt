package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.ServicoAdapter
import com.google.firebase.firestore.FirebaseFirestore

class SalaoServicosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var servicoAdapter: ServicoAdapter
    private lateinit var ivBack: ImageView
    private val listaServicos = mutableListOf<HashMap<String, Any>>()
    private var salaoId: String? = null

    private val db = FirebaseFirestore.getInstance()

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
        ivBack = view.findViewById(R.id.iv_back)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // false para a visão do cliente
        servicoAdapter = ServicoAdapter(listaServicos, db, false)
        recyclerView.adapter = servicoAdapter

        ivBack.setOnClickListener {
            findNavController().popBackStack()
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
