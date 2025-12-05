package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.SalaoAdapter
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardClienteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var salaoAdapter: SalaoAdapter
    private val salaoList = mutableListOf<HashMap<String, Any>>()
    private val filteredSalaoList = mutableListOf<HashMap<String, Any>>()
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Views do Header
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_cliente, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        recyclerView = view.findViewById(R.id.salons_recycler_view)
        searchView = view.findViewById(R.id.search_view)
        
        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        // Altera para LinearLayoutManager para exibir um card por linha
        recyclerView.layoutManager = LinearLayoutManager(context)
        salaoAdapter = SalaoAdapter(filteredSalaoList)
        recyclerView.adapter = salaoAdapter

        setupSearchView()
        loadSaloes()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadHeaderInfo()
    }

    private fun loadHeaderInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userName.text = document.getString("nome")

                        val imageUrl = document.getString("imageUrl")
                        if (!imageUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(imageUrl)
                                .centerCrop()
                                .into(profileImage)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("DashboardClienteFragment", "Erro ao carregar informações do cabeçalho", exception)
                }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSaloes(newText)
                return true
            }
        })
    }

    private fun loadSaloes() {
        db.collection("usuarios")
            .whereEqualTo("tipo", "salao")
            .get()
            .addOnSuccessListener { result ->
                Log.d("DashboardClienteFragment", "Sucesso! Número de salões encontrados: ${result.size()}")
                salaoList.clear()
                for (document in result) {
                    val salao = document.data as HashMap<String, Any>
                    salao["id"] = document.id
                    salaoList.add(salao)
                }
                filterSaloes(searchView.query.toString()) // Filtra com o texto atual da busca
            }
            .addOnFailureListener { exception ->
                Log.e("DashboardClienteFragment", "Falha ao carregar salões.", exception)
                // Lidar com o erro aqui, talvez mostrando uma mensagem
            }
    }

    private fun filterSaloes(query: String?) {
        filteredSalaoList.clear()
        if (query.isNullOrEmpty()) {
            filteredSalaoList.addAll(salaoList)
        } else {
            val lowerCaseQuery = query.lowercase()
            for (salao in salaoList) {
                val nome = salao["nome"] as? String
                if (nome != null && nome.lowercase().contains(lowerCaseQuery)) {
                    filteredSalaoList.add(salao)
                }
            }
        }
        salaoAdapter.notifyDataSetChanged()
    }
}
