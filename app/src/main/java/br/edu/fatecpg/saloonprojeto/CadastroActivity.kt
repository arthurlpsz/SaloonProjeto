package br.edu.fatecpg.saloonprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CadastroActivity : AppCompatActivity() {

    private lateinit var clientButton: Button
    private lateinit var salonButton: Button
    private lateinit var clientFields: Group
    private lateinit var salonFields: Group
    private var isClientSelected = true

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Botões e campos
        clientButton = findViewById(R.id.client_button)
        salonButton = findViewById(R.id.salon_button)
        clientFields = findViewById(R.id.client_fields_group)
        salonFields = findViewById(R.id.salon_fields_group)

        val loginButton: Button = findViewById(R.id.login_button)
        val cadastroButton: Button = findViewById(R.id.cadastro_button)

        val nameField: EditText = findViewById(R.id.name)
        val salonNameField: EditText = findViewById(R.id.salon_name)
        val addressField: EditText = findViewById(R.id.address)
        val phoneField: EditText = findViewById(R.id.phone)
        val emailField: EditText = findViewById(R.id.email)
        val passwordField: EditText = findViewById(R.id.password)

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        cadastroButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val phone = phoneField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Criar usuário no Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    val userData = hashMapOf(
                        "tipo" to if (isClientSelected) "cliente" else "salao",
                        "telefone" to phone,
                        "email" to email
                    )

                    if (isClientSelected) {
                        userData["nome"] = nameField.text.toString().trim()
                    } else {
                        userData["nomeSalao"] = salonNameField.text.toString().trim()
                        userData["endereco"] = addressField.text.toString().trim()
                    }

                    // Salvar no Firestore
                    db.collection("usuarios").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                            // Redirecionar após sucesso
                            val intent = if (isClientSelected) {
                                Intent(this, LoginActivity::class.java)
                            } else {
                                Intent(this, SalaoDashboardActivity::class.java)
                            }
                            startActivity(intent)
                            finish()
                        }
                        // 🔧 Aqui está o trecho que você pediu:
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao salvar dados no Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao criar conta: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        clientButton.setOnClickListener { selectClientForm() }
        salonButton.setOnClickListener { selectSalonForm() }

        selectClientForm()
    }

    private fun selectClientForm() {
        isClientSelected = true
        clientButton.setBackgroundResource(R.drawable.button_selected_background)
        clientButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        salonButton.setBackgroundResource(R.drawable.button_unselected_background)
        salonButton.setTextColor(ContextCompat.getColor(this, R.color.petrol_blue))
        clientFields.visibility = View.VISIBLE
        salonFields.visibility = View.GONE
    }

    private fun selectSalonForm() {
        isClientSelected = false
        salonButton.setBackgroundResource(R.drawable.button_selected_background)
        salonButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        clientButton.setBackgroundResource(R.drawable.button_unselected_background)
        clientButton.setTextColor(ContextCompat.getColor(this, R.color.petrol_blue))
        salonFields.visibility = View.VISIBLE
        clientFields.visibility = View.GONE
    }
}
