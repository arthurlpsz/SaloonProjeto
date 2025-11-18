package br.edu.fatecpg.saloonprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.bumptech.glide.Glide

class SalaoAdapter(
    private var saloes: List<HashMap<String, Any>>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SalaoAdapter.SalaoViewHolder>() {

    inner class SalaoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val salonImage: ImageView = view.findViewById(R.id.salon_image)
        val salonName: TextView = view.findViewById(R.id.salon_name_text)
        val salonDistance: TextView = view.findViewById(R.id.salon_distance_text)
        val btnVerServicos: AppCompatButton = view.findViewById(R.id.btn_ver_servicos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_salao_card, parent, false)
        return SalaoViewHolder(view)
    }

    override fun getItemCount(): Int = saloes.size

    override fun onBindViewHolder(holder: SalaoViewHolder, position: Int) {
        val salao = saloes[position]

        val nome = (salao["nomeSalao"] as? String)
            ?: (salao["nome"] as? String)
            ?: "Salão"

        val endereco = salao["endereco"] as? String ?: "Localização não informada"

        holder.salonName.text = nome
        holder.salonDistance.text = endereco

        val imagemUrl = salao["fotoUrl"] as? String

        if (!imagemUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imagemUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.salonImage)
        } else {
            holder.salonImage.setImageResource(R.drawable.ic_launcher_background)
        }

        val salaoId = salao["id"] as? String ?: return

        holder.btnVerServicos.setOnClickListener {
            onClick(salaoId)
        }
    }

    fun updateList(newList: List<HashMap<String, Any>>) {
        saloes = newList
        notifyDataSetChanged()
    }
}
