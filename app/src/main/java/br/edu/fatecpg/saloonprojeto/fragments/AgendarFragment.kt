package br.edu.fatecpg.saloonprojeto.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgendarFragment : Fragment() {

    private lateinit var btnHoje: Button
    private lateinit var btnAmanha: Button
    private lateinit var btnOutroDia: Button
    private lateinit var dateButtons: List<Button>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_agendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backArrow = view.findViewById<ImageView>(R.id.iv_back)
        backArrow.setOnClickListener {
            findNavController().navigate(R.id.action_agendarFragment_to_dashboardClienteFragment)
        }

        btnHoje = view.findViewById(R.id.btn_hoje)
        btnAmanha = view.findViewById(R.id.btn_amanha)
        btnOutroDia = view.findViewById(R.id.btn_outro_dia)

        dateButtons = listOf(btnHoje, btnAmanha, btnOutroDia)

        btnHoje.setOnClickListener {
            updateButtonSelection(it)
            btnOutroDia.text = "Outro dia"
        }

        btnAmanha.setOnClickListener {
            updateButtonSelection(it)
            btnOutroDia.text = "Outro dia"
        }

        btnOutroDia.setOnClickListener {
            updateButtonSelection(it)
            showDatePicker()
        }

        // Clica no hoje por padrão
        btnHoje.performClick()
    }

    private fun updateButtonSelection(selectedButton: View) {
        dateButtons.forEach { button ->
            button.isSelected = button.id == selectedButton.id
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // If "Other day" button already has a date, use it to initialize the picker
        val otherDayText = btnOutroDia.text.toString()
        if (otherDayText != "Outro dia") {
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val d = sdf.parse(otherDayText)
                if (d != null) {
                    calendar.time = d
                }
            } catch (e: Exception) {
                // Ignore parse exception, use current date
            }
        }

        val ano = calendar.get(Calendar.YEAR)
        val mes = calendar.get(Calendar.MONTH)
        val dia = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            btnOutroDia.text = sdf.format(selectedCalendar.time)
        }, ano, mes, dia)

        // Set min date to today
        datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
        datePickerDialog.show()
    }
}
