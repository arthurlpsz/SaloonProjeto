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

    private lateinit var todayButton: Button
    private lateinit var tomorrowButton: Button
    private lateinit var otherDayButton: Button
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

        todayButton = view.findViewById(R.id.today_button)
        tomorrowButton = view.findViewById(R.id.tomorrow_button)
        otherDayButton = view.findViewById(R.id.other_day_button)

        dateButtons = listOf(todayButton, tomorrowButton, otherDayButton)

        todayButton.setOnClickListener {
            updateButtonSelection(it)
            otherDayButton.text = "Outro dia"
        }

        tomorrowButton.setOnClickListener {
            updateButtonSelection(it)
            otherDayButton.text = "Outro dia"
        }

        otherDayButton.setOnClickListener {
            updateButtonSelection(it)
            showDatePicker()
        }

        // Select today by default
        todayButton.performClick()
    }

    private fun updateButtonSelection(selectedButton: View) {
        dateButtons.forEach { button ->
            button.isSelected = button.id == selectedButton.id
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // If "Other day" button already has a date, use it to initialize the picker
        val otherDayText = otherDayButton.text.toString()
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

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            otherDayButton.text = sdf.format(selectedCalendar.time)
        }, year, month, day)

        // Set min date to today
        datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
        datePickerDialog.show()
    }
}
