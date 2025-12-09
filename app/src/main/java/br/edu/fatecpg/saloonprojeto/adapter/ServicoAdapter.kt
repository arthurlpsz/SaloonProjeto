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
        val btnEditar: ImageButton? = view.findViewById(R.id.btn_editar_sevico)
        val btnExcluir: ImageButton? = view.findViewById(R.id.delete_button)
        val btnAgendar: Button? = view.findViewById(R.id.btn_agendar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicoViewHolder {
        val layoutId = if (isSalaoView) R.layout.item_salao_servico_gerenciavel else R.layout.item_servico_cliente
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ServicoViewHolder(view)
    }

    override fun getItemCount(): Int = listaServicos.size

    override fun onBindViewHolder(holder: ServicoViewHolder, position: Int) {
        val servico = listaServicos[position]
        val isAtivo = servico["ativo"] as? Boolean ?: true // Padrão é ativo

        holder.nome.text = servico["nome"].toString()
        val preco = servico["preco"]
        holder.preco.text = when (preco) {
            is Number -> "R$ %.2f".format(preco.toDouble())
            else -> "R$ 0.00"
        }
        holder.duracao.text = "Duração: ${servico["duracaoMin"]} min"

        // Diferenciação visual para serviços inativos
        holder.itemView.alpha = if (isAtivo) 1.0f else 0.5f

        if (isSalaoView) {
            // O botão de editar só funciona para serviços ativos
            holder.btnEditar?.isEnabled = isAtivo

            holder.btnExcluir?.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val servicoId = listaServicos[currentPosition]["id"] as? String ?: return@setOnClickListener

                if (isAtivo) {
                    // Lógica para DESATIVAR
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Desativar Serviço")
                        .setMessage("Deseja desativar este serviço? Ele não aparecerá para novos agendamentos.")
                        .setPositiveButton("Sim, desativar") { _, _ ->
                            atualizarStatusServico(servicoId, false, holder)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    // Lógica para REATIVAR
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Reativar Serviço")
                        .setMessage("Deseja reativar este serviço? Ele voltará a ser visível para agendamentos.")
                        .setPositiveButton("Sim, reativar") { _, _ ->
                            atualizarStatusServico(servicoId, true, holder)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }

            holder.btnEditar?.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val id = listaServicos[currentPosition]["id"] as? String ?: return@setOnClickListener
                val bundle = Bundle().apply { putString("servicoId", id) }
                holder.itemView.findNavController().navigate(R.id.action_salaoDashboard_to_editarServico, bundle)
            }
        } else {
            holder.btnAgendar?.setOnClickListener {
                 val currentPosition = holder.adapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                val servicoAgendar = listaServicos[currentPosition]
                val id = servicoAgendar["id"] as? String ?: return@setOnClickListener
                val bundle = Bundle().apply {
                    putString("servicoId", id)
                    putString("salaoId", servicoAgendar["salaoId"] as? String)
                }
                holder.itemView.findNavController().navigate(R.id.action_salaoServicos_to_agendar, bundle)
            }
        }
    }
    
    private fun atualizarStatusServico(servicoId: String, novoStatus: Boolean, holder: ServicoViewHolder) {
        db.collection("servicos").document(servicoId)
            .update("ativo", novoStatus)
            .addOnSuccessListener {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    // Atualiza o item na lista local e notifica o adapter para redesenhar
                    listaServicos[pos]["ativo"] = novoStatus
                    notifyItemChanged(pos)
                    val msg = if (novoStatus) "Serviço reativado!" else "Serviço desativado!"
                    Toast.makeText(holder.itemView.context, msg, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                 Toast.makeText(holder.itemView.context, "Erro ao atualizar o serviço: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun updateList(novaLista: List<HashMap<String, Any>>) {
        listaServicos.clear()
        listaServicos.addAll(novaLista)
        notifyDataSetChanged()
    }
}
