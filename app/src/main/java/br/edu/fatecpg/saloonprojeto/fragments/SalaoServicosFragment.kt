package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.adapter.ServicoClienteAdapter
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class SalaoServicosFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var salonImage: ImageView
    private lateinit var salonName: TextView
    private lateinit var salonAddress: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ServicoClienteAdapter

    private var salaoId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_salao_servicos, container, false)

        salonImage = view.findViewById(R.id.img_salao)
        salonName = view.findViewById(R.id.salon_name)
        salonAddress = view.findViewById(R.id.salon_address)
        recyclerView = view.findViewById(R.id.services_recycler_view)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Recebe o ID do salão vindo da home
        salaoId = arguments?.getString("salaoId") ?: ""

        // Adapter do cliente (apenas visualizar e agendar)
        adapter = ServicoClienteAdapter(emptyList()) { servicoId ->
            val bundle = bundleOf(
                "servicoId" to servicoId,
                "salaoId" to salaoId
            )
            findNavController().navigate(
                R.id.action_salaoServicosFragment_to_navigation_agendar,
                bundle
            )
        }
        recyclerView.adapter = adapter

        carregarDadosDoSalao()
        carregarServicosDoSalao()

        return view
    }

    private fun carregarDadosDoSalao() {
        if (salaoId.isEmpty()) return

        db.collection("usuarios").document(salaoId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    preencherDadosSalaoDoc(doc.data)
                } else {
                    // fallback
                    db.collection("saloes").document(salaoId).get()
                        .addOnSuccessListener { doc2 ->
                            if (doc2 != null && doc2.exists()) {
                                preencherDadosSalaoDoc(doc2.data)
                            }
                        }
                }
            }
    }

    private fun preencherDadosSalaoDoc(data: Map<String, Any>?) {
        if (data == null) return

        val nome = (data["nomeSalao"] as? String)
            ?: (data["nome"] as? String)
            ?: "Salão"

        val endereco = (data["endereco"] as? String) ?: ""

        val foto = (data["fotoUrl"] as? String)
            ?: (data["imagemUrl"] as? String)

        salonName.text = nome
        salonAddress.text = endereco

        Glide.with(requireContext())
            .load(if (foto.isNullOrEmpty()) null else foto) // se não houver imagem, Glide usa placeholder
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(salonImage)
    }

    private fun carregarServicosDoSalao() {
        if (salaoId.isEmpty()) return

        // Caso seus serviços estejam na coleção raiz "servicos"
        db.collection("servicos")
            .whereEqualTo("salaoId", salaoId)
            .get()
            .addOnSuccessListener { result ->
                processarResultadoServicos(result)
            }
            .addOnFailureListener {
                // fallback: serviços como subcoleção do salão
                db.collection("usuarios").document(salaoId)
                    .collection("servicos")
                    .get()
                    .addOnSuccessListener { result2 ->
                        processarResultadoServicos(result2)
                    }
            }
    }

    private fun processarResultadoServicos(result: QuerySnapshot) {
        val lista: List<HashMap<String, Any>> = result.documents.map { doc ->
            hashMapOf(
                "id" to doc.id,
                "nome" to (doc.getString("nome") ?: "Serviço"),
                "preco" to (doc.get("preco")?.toString() ?: "0.00")
            )
        }

        adapter.updateList(lista)
    }
}
