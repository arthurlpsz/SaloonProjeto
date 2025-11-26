package br.edu.fatecpg.saloonprojeto.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.saloonprojeto.R

data class DiaHorario(
    val dia: String,
    var abre: String = "",
    var fecha: String = "",
    var fechado: Boolean = false
)

class HorarioAdapter(private val dias: List<DiaHorario>) :
    RecyclerView.Adapter<HorarioAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDia: TextView = itemView.findViewById(R.id.tv_dia)
        val edtAbre: EditText = itemView.findViewById(R.id.edt_abre)
        val edtFecha: EditText = itemView.findViewById(R.id.edt_fecha)
        val checkFechado: CheckBox = itemView.findViewById(R.id.check_fechado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dia_horario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dias[position]

        holder.tvDia.text = item.dia
        holder.edtAbre.setText(item.abre)
        holder.edtFecha.setText(item.fecha)
        holder.checkFechado.isChecked = item.fechado

        holder.checkFechado.setOnCheckedChangeListener { _, isChecked ->
            item.fechado = isChecked
        }

        holder.edtAbre.addTextChangedListener {
            item.abre = holder.edtAbre.text.toString()
        }

        holder.edtFecha.addTextChangedListener {
            item.fecha = holder.edtFecha.text.toString()
        }
    }

    override fun getItemCount(): Int = dias.size

    fun getHorarios() = dias
}
