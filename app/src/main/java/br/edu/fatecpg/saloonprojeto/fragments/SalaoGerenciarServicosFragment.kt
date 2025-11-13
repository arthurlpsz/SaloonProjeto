package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapters.ServicoAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoGerenciarServicosFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ServicoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_gerenciar_servicos, container, false)

        // Inicializa Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Configura RecyclerView
        recyclerView = view.findViewById(R.id.services_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ServicoAdapter(mutableListOf(), db)
        recyclerView.adapter = adapter

        // Botão de adicionar serviço
        val fabAddService = view.findViewById<FloatingActionButton>(R.id.fab_add_service)
        fabAddService.setOnClickListener {
            findNavController().navigate(R.id.action_salao_gerenciar_to_salao_adicionar)
        }

        // Carrega os serviços do salão logado
        carregarServicos()

        return view
    }

    private fun carregarServicos() {
        val salaoId = auth.currentUser?.uid

        if (salaoId == null) {
            Toast.makeText(requireContext(), "Usuário não autenticado!", Toast.LENGTH_SHORT).show()
            Log.e("SERVICOS", "Usuário não autenticado. Não foi possível carregar os serviços.")
            return
        }

        db.collection("servicos")
            .whereEqualTo("salaoId", salaoId)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.w("SERVICOS", "Nenhum serviço encontrado para este salão.")
                    Toast.makeText(requireContext(), "Nenhum serviço cadastrado.", Toast.LENGTH_SHORT).show()
                }

                val listaServicos = result.documents.map { doc ->
                    hashMapOf<String, Any>(
                        "id" to doc.id,
                        "nome" to (doc.getString("nome") ?: "Sem nome"),
                        "descricao" to (doc.getString("descricao") ?: ""),
                        "preco" to (doc.getDouble("preco") ?: 0.0),
                        "duracao" to (doc.getLong("duracao")?.toInt() ?: 0)
                    )
                }

                Log.d("SERVICOS", "Serviços carregados: $listaServicos")

                adapter.updateList(listaServicos)
            }
            .addOnFailureListener { e ->
                Log.e("SERVICOS", "Erro ao carregar serviços", e)
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar serviços: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}
