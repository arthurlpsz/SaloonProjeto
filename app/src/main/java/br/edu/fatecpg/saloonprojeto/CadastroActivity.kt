package br.edu.fatecpg.saloonprojeto

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Botões e campos
        clientButton = findViewById(R.id.client_button)
        salonButton = findViewById(R.id.salon_button)
        clientFields = findViewById(R.id.client_fields_group)
        salonFields = findViewById(R.id.salon_fields_group)

        val loginButton: Button = findViewById(R.id.btn_login)
        val cadastroButton: Button = findViewById(R.id.cadastro_button)

        val nameField: EditText = findViewById(R.id.name)
        val salonNameField: EditText = findViewById(R.id.salon_name)
        val addressField: EditText = findViewById(R.id.address)
        val phoneField: EditText = findViewById(R.id.phone)
        val emailField: EditText = findViewById(R.id.email)
        val passwordField: EditText = findViewById(R.id.password)

        phoneField.addTextChangedListener(TelefoneFormatador(phoneField))

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        cadastroButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val phone = phoneField.text.toString().trim()
            val name = nameField.text.toString().trim()
            val salonName = salonNameField.text.toString().trim()
            val address = addressField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isClientSelected && name.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha o seu nome.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isClientSelected && salonName.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha o nome do salão.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Criar usuário no Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid ?: return@addOnSuccessListener

                    val userData = hashMapOf(
                        "userId" to userId,
                        "tipo" to if (isClientSelected) "cliente" else "salao",
                        "telefone" to phone,
                        "email" to email,
                        "adicionadoEm" to FieldValue.serverTimestamp()
                    )

                    if (isClientSelected) {
                        userData["nome"] = name
                    } else {
                        userData["nomeSalao"] = salonName
                        userData["endereco"] = address
                    }

                    // Salvar no Firestore
                    db.collection("usuarios").document(userId)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, IntroActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                            finish()
                        }
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
        clientButton.setTextColor(ContextCompat.getColor(this, R.color.branco))
        salonButton.setBackgroundResource(R.drawable.button_unselected_background)
        salonButton.setTextColor(ContextCompat.getColor(this, R.color.azul_petroleo))
        clientFields.visibility = View.VISIBLE
        salonFields.visibility = View.GONE
    }

    private fun selectSalonForm() {
        isClientSelected = false
        salonButton.setBackgroundResource(R.drawable.button_selected_background)
        salonButton.setTextColor(ContextCompat.getColor(this, R.color.branco))
        clientButton.setBackgroundResource(R.drawable.button_unselected_background)
        clientButton.setTextColor(ContextCompat.getColor(this, R.color.azul_petroleo))
        salonFields.visibility = View.VISIBLE
        clientFields.visibility = View.GONE
    }
}
