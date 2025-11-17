package br.edu.fatecpg.saloonprojeto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.firestore.FirebaseFirestore

class ServicoAdapter(
    private var listaServicos: MutableList<HashMap<String, Any>>,
    private val db: FirebaseFirestore
) : RecyclerView.Adapter<ServicoAdapter.ServicoViewHolder>() {

    inner class ServicoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nome: TextView = view.findViewById(R.id.service_name)
        val preco: TextView = view.findViewById(R.id.service_price)
        val duracao: TextView = view.findViewById(R.id.duracao_servico)
        val btnEditar: ImageButton = view.findViewById(R.id.btn_editar_sevico)
        val btnExcluir: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_salao_servico_gerenciavel, parent, false)
        return ServicoViewHolder(view)
    }

    override fun getItemCount(): Int = listaServicos.size

    override fun onBindViewHolder(holder: ServicoViewHolder, position: Int) {
        val servico = listaServicos[position]

        holder.nome.text = servico["nome"].toString()
        holder.preco.text = "R$ %.2f".format(servico["preco"] ?: 0.0)
        holder.duracao.text = "Duração: ${servico["duracao"]} min"

        holder.btnExcluir.setOnClickListener {
            val id = servico["id"] as? String ?: return@setOnClickListener
            db.collection("servicos").document(id).delete()
                .addOnSuccessListener {
                    listaServicos.removeAt(position)
                    notifyItemRemoved(position)
                    Toast.makeText(holder.itemView.context, "Serviço excluído", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(holder.itemView.context, "Erro ao excluir", Toast.LENGTH_SHORT).show()
                }
        }

        holder.btnEditar.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Em breve: edição de serviços", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateList(novaLista: List<HashMap<String, Any>>) {
        listaServicos.clear()
        listaServicos.addAll(novaLista)
        notifyDataSetChanged()
    }
}
