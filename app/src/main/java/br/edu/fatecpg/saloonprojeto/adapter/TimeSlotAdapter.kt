package br.edu.fatecpg.saloonprojeto.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R

class TimeSlotAdapter(
    private var timeSlots: List<String> = emptyList(),   // lista padrão vazia -> permite new TimeSlotAdapter { ... }
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = -1

    inner class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtHorario: TextView = itemView.findViewById(R.id.txt_horario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val horario = timeSlots[position]
        holder.txtHorario.text = horario

        // Destaque do horário selecionado — usa drawables que você deve ter em res/drawable
        val bgRes = if (position == selectedPosition) {
            R.drawable.selected_time_slot
        } else {
            R.drawable.default_time_slot
        }
        holder.txtHorario.setBackgroundResource(bgRes)

        holder.itemView.setOnClickListener {
            selectedPosition = position
            notifyDataSetChanged() // atualiza o destaque
            onClick(horario)
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    // método público para atualizar a lista dinamicamente
    fun updateList(newList: List<String>) {
        timeSlots = newList
        selectedPosition = -1
        notifyDataSetChanged()
    }
}
