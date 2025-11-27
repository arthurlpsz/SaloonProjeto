package br.edu.fatecpg.saloonprojeto.adapter

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.firestore.FirebaseFirestore

class ServicoAdapter(
    private var listaServicos: MutableList<HashMap<String, Any>>,
    private val db: FirebaseFirestore,
    private val isSalaoView: Boolean
) : RecyclerView.Adapter<ServicoAdapter.ServicoViewHolder>() {

    inner class ServicoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nome: TextView = view.findViewById(R.id.service_name)
        val preco: TextView = view.findViewById(R.id.service_price)
        val duracao: TextView = view.findViewById(R.id.duracao_servico)
        // Os botões podem ser nulos dependendo do layout
        val btnEditar: ImageButton? = view.findViewById(R.id.btn_editar_sevico)
        val btnExcluir: ImageButton? = view.findViewById(R.id.delete_button)
        val btnAgendar: Button? = view.findViewById(R.id.btn_agendar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicoViewHolder {
        val layoutId = if (isSalaoView) {
            R.layout.item_salao_servico_gerenciavel
        } else {
            R.layout.item_servico_cliente
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ServicoViewHolder(view)
    }

    override fun getItemCount(): Int = listaServicos.size

    override fun onBindViewHolder(holder: ServicoViewHolder, position: Int) {
        val servico = listaServicos[position]

        holder.nome.text = servico["nome"].toString()
        val preco = servico["preco"]
        val precoFormatado = when (preco) {
            is Number -> "R$ %.2f".format(preco.toDouble())
            else -> "R$ 0.00"
        }
        holder.preco.text = precoFormatado
        holder.duracao.text = "Duração: ${servico["duracao"]} min"

        if (isSalaoView) {
            holder.btnExcluir?.setOnClickListener {
                val id = servico["id"] as? String ?: return@setOnClickListener

                AlertDialog.Builder(holder.itemView.context)
                    .setTitle("Excluir Serviço")
                    .setMessage("Você tem certeza que deseja excluir este serviço?")
                    .setPositiveButton("Sim") { _, _ ->
                        db.collection("servicos").document(id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(holder.itemView.context, "Serviço excluído", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(holder.itemView.context, "Erro ao excluir", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }

            holder.btnEditar?.setOnClickListener {
                val id = servico["id"] as? String ?: return@setOnClickListener
                val bundle = Bundle()
                bundle.putString("servicoId", id)
                holder.itemView.findNavController().navigate(R.id.action_salaoDashboard_to_editarServico, bundle)
            }
        } else {
            holder.btnAgendar?.setOnClickListener {
                val id = servico["id"] as? String ?: return@setOnClickListener
                val bundle = Bundle()
                bundle.putString("servicoId", id)
                bundle.putString("salaoId", servico["salaoId"] as? String)

                holder.itemView.findNavController().navigate(R.id.action_salaoServicos_to_agendar, bundle)
            }
        }
    }

    fun updateList(novaLista: List<HashMap<String, Any>>) {
        listaServicos.clear()
        listaServicos.addAll(novaLista)
        notifyDataSetChanged()
    }
}
