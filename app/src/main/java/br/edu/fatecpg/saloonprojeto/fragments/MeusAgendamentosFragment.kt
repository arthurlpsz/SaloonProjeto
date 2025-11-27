// MeusAgendamentosFragment.kt
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MeusAgendamentosFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvPendentes: RecyclerView
    private lateinit var rvConcluidos: RecyclerView
    private val pendentesAdapter = SimpleAgendamentoAdapter()
    private val concluidosAdapter = SimpleAgendamentoAdapter()

    // Views do Header
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_meus_agendamentos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvPendentes = view.findViewById(R.id.rv_pendentes)
        rvConcluidos = view.findViewById(R.id.rv_concluidos)
        
        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        rvPendentes.layoutManager = LinearLayoutManager(requireContext())
        rvConcluidos.layoutManager = LinearLayoutManager(requireContext())

        rvPendentes.adapter = pendentesAdapter
        rvConcluidos.adapter = concluidosAdapter

        loadHeaderInfo()
        loadLastThreeMonths()
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
                    Log.e("MeusAgendamentosFragment", "Erro ao carregar informações do cabeçalho", exception)
                }
        }
    }

    private fun loadLastThreeMonths() {
        val userId = auth.currentUser?.uid ?: return
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -90) }
        val threshold = Timestamp(cal.time)

        db.collection("agendamentos")
            .whereEqualTo("clienteId", userId)
            .whereGreaterThanOrEqualTo("dataInicio", threshold)
            .orderBy("dataInicio")
            .get()
            .addOnSuccessListener { snap ->
                val pend = mutableListOf<Map<String, Any>>()
                val conc = mutableListOf<Map<String, Any>>()
                for (d in snap.documents) {
                    val m = d.data ?: continue
                    val status = (m["status"] as? String) ?: "agendado"
                    val withId = HashMap(m)
                    withId["id"] = d.id
                    if (status == "concluido") conc.add(withId) else pend.add(withId)
                }
                pendentesAdapter.submitList(pend)
                concluidosAdapter.submitList(conc)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao carregar agendamentos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Simple adapter (replace with your own)
    class SimpleAgendamentoAdapter : RecyclerView.Adapter<SimpleAgendamentoAdapter.VH>() {
        private var list: List<Map<String, Any>> = emptyList()
        fun submitList(l: List<Map<String, Any>>) { list = l; notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = list.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            val serv = item["servicoId"]?.toString() ?: "Serviço"
            val start = (item["dataInicio"] as? com.google.firebase.Timestamp)?.toDate()
            holder.title.text = serv
            holder.subtitle.text = start?.toString() ?: ""
        }
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: android.widget.TextView = view.findViewById(android.R.id.text1)
            val subtitle: android.widget.TextView = view.findViewById(android.R.id.text2)
        }
    }
}
