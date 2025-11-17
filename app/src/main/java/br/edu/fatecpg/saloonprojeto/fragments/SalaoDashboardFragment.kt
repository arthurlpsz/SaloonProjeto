package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.ServicoAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoDashboardFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var servicesRecyclerView: RecyclerView
    private lateinit var servicoAdapter: ServicoAdapter
    private val servicosList = mutableListOf<HashMap<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashboard_salao, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser
        val userNameTextView = view.findViewById<TextView>(R.id.user_name)

        if (user != null) {
            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val userType = document.getString("tipo")
                        val name = if (userType == "salao") {
                            document.getString("nomeSalao")
                        } else {
                            document.getString("nome")
                        }
                        userNameTextView.text = name ?: user.displayName // Fallback to displayName
                    } else {
                        // Fallback to displayName if firestore doc doesn't exist
                        userNameTextView.text = user.displayName
                    }
                }
                .addOnFailureListener {
                    // Fallback to displayName on failure
                    userNameTextView.text = user.displayName
                }
        }

        servicesRecyclerView = view.findViewById(R.id.services_recycler_view)
        servicesRecyclerView.layoutManager = LinearLayoutManager(context)
        servicoAdapter = ServicoAdapter(servicosList, db)
        servicesRecyclerView.adapter = servicoAdapter

        loadServices()

        view.findViewById<FloatingActionButton>(R.id.fab_add_service).setOnClickListener {
            findNavController().navigate(R.id.action_salao_dashboard_to_salao_adicionar)
        }
    }

    private fun loadServices() {
        val salaoId = auth.currentUser?.uid ?: return

        db.collection("servicos").whereEqualTo("salaoId", salaoId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val newServicos = snapshots.documents.map { document ->
                        val servico = document.data as HashMap<String, Any>
                        servico["id"] = document.id
                        servico
                    }
                    servicoAdapter.updateList(newServicos)
                }
            }
    }
}
