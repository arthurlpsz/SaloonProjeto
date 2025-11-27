package br.edu.fatecpg.saloonprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R

class ServicoClienteAdapter(
    private var lista: List<HashMap<String, Any>>,
    private val onAgendarClick: (String) -> Unit
) : RecyclerView.Adapter<ServicoClienteAdapter.ServicoViewHolder>() {

    inner class ServicoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nome: TextView = view.findViewById(R.id.service_name)
        val preco: TextView = view.findViewById(R.id.service_price)
        val botaoAgendar: Button = view.findViewById(R.id.btn_agendar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServicoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_servico_cliente, parent, false)
        return ServicoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServicoViewHolder, position: Int) {
        val servico = lista[position]

        val nome = servico["nome"] as? String ?: "Serviço"
        val preco = servico["preco"]
        val precoFormatado = when (preco) {
            is Number -> "R$ %.2f".format(preco.toDouble())
            else -> "R$ 0.00"
        }
        val id = servico["id"] as? String ?: ""

        holder.nome.text = nome
        holder.preco.text = precoFormatado

        holder.botaoAgendar.setOnClickListener {
            onAgendarClick(id)
        }
    }

    override fun getItemCount(): Int = lista.size

    fun updateList(newList: List<HashMap<String, Any>>) {
        lista = newList
        notifyDataSetChanged()
    }
}