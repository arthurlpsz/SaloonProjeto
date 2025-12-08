package br.edu.fatecpg.saloonprojeto.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import br.edu.fatecpg.saloonprojeto.model.Agendamento
import java.text.SimpleDateFormat
import java.util.Locale

sealed class ListItem {
    data class AgendamentoItem(val agendamento: Agendamento) : ListItem()
    object EmptyItem : ListItem()
}

enum class AgendamentoViewType {
    CLIENTE,
    SALAO
}

class AgendamentoAdapter(
    private val items: MutableList<ListItem>,
    private val viewType: AgendamentoViewType,
    private val onCancelarClick: (Agendamento) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_AGENDAMENTO = 0
        private const val VIEW_TYPE_EMPTY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.AgendamentoItem -> VIEW_TYPE_AGENDAMENTO
            is ListItem.EmptyItem -> VIEW_TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AGENDAMENTO -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_agendamento, parent, false)
                AgendamentoViewHolder(view)
            }
            VIEW_TYPE_EMPTY -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_agendamento_vazio, parent, false)
                EmptyViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.AgendamentoItem -> (holder as AgendamentoViewHolder).bind(item.agendamento)
            is ListItem.EmptyItem -> {
                // O EmptyViewHolder não precisa de bind
            }
        }
    }

    override fun getItemCount() = items.size

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class AgendamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceName: TextView = itemView.findViewById(R.id.service_name)
        private val nameTextView: TextView = itemView.findViewById(R.id.salon_name)
        private val bookingDate: TextView = itemView.findViewById(R.id.booking_date)
        private val bookingTime: TextView = itemView.findViewById(R.id.booking_time)
        private val bookingStatus: TextView = itemView.findViewById(R.id.booking_status)
        private val statusBar: View = itemView.findViewById(R.id.status_bar)
        private val btnCancelar: Button = itemView.findViewById(R.id.btn_cancelar)

        fun bind(agendamento: Agendamento) {
            serviceName.text = agendamento.servico
            bookingStatus.text = "Status: ${agendamento.status}"

            if (viewType == AgendamentoViewType.SALAO) {
                nameTextView.text = "Cliente: ${agendamento.nomeUsuario}"
            } else {
                nameTextView.text = "Salão: ${agendamento.nomeSalao}"
            }

            agendamento.dataInicio?.toDate()?.let {
                val sdfData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
                bookingDate.text = sdfData.format(it)
                bookingTime.text = sdfHora.format(it)
            }

            when (agendamento.status) {
                "Confirmado" -> {
                    statusBar.setBackgroundColor(Color.parseColor("#FFD700")) // Gold
                    btnCancelar.visibility = View.VISIBLE
                    btnCancelar.setOnClickListener { onCancelarClick(agendamento) }
                }
                "Cancelado", "Realizado" -> {
                    statusBar.setBackgroundColor(Color.parseColor(if (agendamento.status == "Cancelado") "#FF0000" else "#808080")) // Red for Cancelado, Gray for Realizado
                    btnCancelar.visibility = View.GONE
                }
                else -> {
                    statusBar.setBackgroundColor(Color.parseColor("#808080")) // Gray
                    btnCancelar.visibility = View.GONE
                }
            }
        }
    }
}
