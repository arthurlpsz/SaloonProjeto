package br.edu.fatecpg.saloonprojeto.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R
import com.google.android.material.card.MaterialCardView

class TimeSlotAdapter(
    private var timeSlots: List<String> = emptyList(),
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = -1

    inner class TimeSlotViewHolder(val cardView: MaterialCardView) : RecyclerView.ViewHolder(cardView) {
        val txtHorario: TextView = cardView.findViewById(R.id.tv_slot_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val cardView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hora, parent, false) as MaterialCardView
        return TimeSlotViewHolder(cardView)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val horario = timeSlots[position]
        holder.txtHorario.text = horario

        holder.cardView.isChecked = (position == selectedPosition)

        holder.cardView.setOnClickListener {
            if (holder.adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val previousSelectedPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            if (previousSelectedPosition != -1) {
                notifyItemChanged(previousSelectedPosition)
            }
            notifyItemChanged(selectedPosition)

            onClick(horario)
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<String>) {
        timeSlots = newList
        selectedPosition = -1
        notifyDataSetChanged()
    }
}
