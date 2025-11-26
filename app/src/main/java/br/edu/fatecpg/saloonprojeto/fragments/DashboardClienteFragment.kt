package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.SalaoAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardClienteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SalaoAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var userNameTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_cliente, container, false)

        userNameTextView = view.findViewById(R.id.user_name)
        recyclerView = view.findViewById(R.id.salons_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // 🔥 Navegação corrigida aqui
        adapter = SalaoAdapter(emptyList()) { salaoId ->
            val bundle = Bundle()
            bundle.putString("salaoId", salaoId)
            findNavController().navigate(R.id.action_home_to_salaoServicos, bundle)
        }

        recyclerView.adapter = adapter

        carregarDadosUsuario()
        carregarSaloes()

        return view
    }

    private fun carregarDadosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { doc ->
                    val nome = doc.getString("nome") ?: "Usuário"
                    userNameTextView.text = nome
                }
        }
    }

    private fun carregarSaloes() {
        db.collection("usuarios")
            .whereEqualTo("tipo", "salao")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.map { doc ->
                    hashMapOf(
                        "id" to doc.id,
                        "nomeSalao" to (doc.getString("nomeSalao") ?: "Salão"),
                        "endereco" to (doc.getString("endereco") ?: ""),
                        "fotoUrl" to (doc.getString("fotoUrl") ?: "")
                    ) as HashMap<String, Any>
                }

                adapter.updateList(lista)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar salões", Toast.LENGTH_LONG).show()
            }
    }
}
