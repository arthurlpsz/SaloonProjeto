package br.edu.fatecpg.saloonprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat

class CadastroActivity : AppCompatActivity() {

    private lateinit var clientButton: Button
    private lateinit var salonButton: Button
    private lateinit var clientFields: Group
    private lateinit var salonFields: Group
    private var isClientSelected = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        // --- Button and Field Initialization ---
        clientButton = findViewById(R.id.client_button)
        salonButton = findViewById(R.id.salon_button)
        clientFields = findViewById(R.id.client_fields_group)
        salonFields = findViewById(R.id.salon_fields_group)

        val loginButton: Button = findViewById(R.id.login_button)
        val cadastroButton: Button = findViewById(R.id.cadastro_button)

        // --- Click Listeners ---
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        cadastroButton.setOnClickListener {
            val intent = if (isClientSelected) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, SalaoDashboardActivity::class.java)
            }
            startActivity(intent)
        }

        clientButton.setOnClickListener {
            selectClientForm()
        }

        salonButton.setOnClickListener {
            selectSalonForm()
        }

        // --- Initial State ---
        selectClientForm() // Start with the client form selected by default
    }

    private fun selectClientForm() {
        isClientSelected = true
        // Update button appearance
        clientButton.setBackgroundResource(R.drawable.button_selected_background)
        clientButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        salonButton.setBackgroundResource(R.drawable.button_unselected_background)
        salonButton.setTextColor(ContextCompat.getColor(this, R.color.petrol_blue))

        // Update form visibility
        clientFields.visibility = View.VISIBLE
        salonFields.visibility = View.GONE
    }

    private fun selectSalonForm() {
        isClientSelected = false
        // Update button appearance
        salonButton.setBackgroundResource(R.drawable.button_selected_background)
        salonButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        clientButton.setBackgroundResource(R.drawable.button_unselected_background)
        clientButton.setTextColor(ContextCompat.getColor(this, R.color.petrol_blue))

        // Update form visibility
        salonFields.visibility = View.VISIBLE
        clientFields.visibility = View.GONE
    }
}