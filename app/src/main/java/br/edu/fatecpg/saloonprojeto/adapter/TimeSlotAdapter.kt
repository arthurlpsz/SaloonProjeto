package br.edu.fatecpg.saloonprojeto.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R

class TimeSlotAdapter(
    private val timeSlots: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return TimeSlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val time = timeSlots[position]
        val isSelected = position == selectedPosition
        holder.bind(time, isSelected)

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position

            // Atualiza apenas os dois itens afetados
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            onClick(time)
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    class TimeSlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.time_text)
        private val card: CardView = itemView.findViewById(R.id.time_card)

        fun bind(time: String, isSelected: Boolean) {
            timeText.text = time

            val context = itemView.context
            val backgroundColor = if (isSelected)
                context.getColor(R.color.gold)
            else
                context.getColor(R.color.white)

            val textColor = if (isSelected)
                context.getColor(R.color.white)
            else
                context.getColor(R.color.black)

            card.setCardBackgroundColor(backgroundColor)
            timeText.setTextColor(textColor)
        }
    }
}
