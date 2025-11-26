package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.ServicoAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast

class SalaoServicosFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var servicoAdapter: ServicoAdapter
    private val listaServicos = mutableListOf<HashMap<String, Any>>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_servicos, container, false)

        recyclerView = view.findViewById(R.id.services_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        servicoAdapter = ServicoAdapter(listaServicos, db)
        recyclerView.adapter = servicoAdapter

        carregarServicos()

        return view
    }

    private fun carregarServicos() {
        val salaoId = auth.currentUser?.uid ?: return

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
