package br.edu.fatecpg.saloonprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton: Button = findViewById(R.id.login_button)
        val createAccountButton: Button = findViewById(R.id.create_account_button)

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val senha = passwordField.text.toString().trim()

            if (email.isEmpty() || senha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener { result ->
                    val userId = result.user?.uid

                    if (userId == null) {
                        Toast.makeText(this, "Erro interno: usuário inválido", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    db.collection("usuarios").document(userId).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                val tipo = doc.getString("tipo") // <- campo correto

                                when (tipo) {
                                    "cliente" -> {
                                        startActivity(Intent(this, MainActivity::class.java))
                                        finish()
                                    }
                                    "salao" -> {
                                        startActivity(Intent(this, SalaoDashboardActivity::class.java))
                                        finish()
                                    }
                                    else -> {
                                        Toast.makeText(this, "Tipo de usuário desconhecido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Usuário não encontrado no banco de dados", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao acessar Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Falha no login: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
        }

        createAccountButton.setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
    }
}
