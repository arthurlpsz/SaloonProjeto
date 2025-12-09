package br.edu.fatecpg.saloonprojeto.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.bumptech.glide.Glide

class SalaoAdapter(
    private var saloes: List<HashMap<String, Any>>,
    private val onVerServicosClick: (String) -> Unit
) : RecyclerView.Adapter<SalaoAdapter.SalaoViewHolder>() {

    inner class SalaoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nomeSalao: TextView = view.findViewById(R.id.txv_nome_salao)
        val enderecoSalao: TextView = view.findViewById(R.id.txv_endereco_salao)
        val imagemSalao: ImageView = view.findViewById(R.id.img_salao)
        val btnVerServicos: Button = view.findViewById(R.id.btn_ver_servicos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_salao_card, parent, false)
        return SalaoViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalaoViewHolder, position: Int) {
        val salao = saloes[position]
        val salaoId = salao["id"] as String

        holder.nomeSalao.text = salao["nomeSalao"] as? String ?: "Nome não encontrado"
        holder.enderecoSalao.text = salao["endereco"] as? String ?: "Endereço não disponível"

        val imageUrl = salao["imageUrl"] as? String
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.salao_feminino)
                .into(holder.imagemSalao)
        } else {
            holder.imagemSalao.setImageResource(R.drawable.salao_feminino)
        }

        holder.btnVerServicos.setOnClickListener {
            onVerServicosClick(salaoId)
        }
    }

    override fun getItemCount() = saloes.size

    fun updateSaloes(newSaloes: List<HashMap<String, Any>>) {
        saloes = newSaloes
        notifyDataSetChanged()
    }
}
