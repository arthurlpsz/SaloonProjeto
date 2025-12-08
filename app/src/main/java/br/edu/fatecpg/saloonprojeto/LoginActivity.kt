package br.edu.fatecpg.saloonprojeto

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Usuário está logado, tenta redirecionar. Se falhar, a tela de login será exibida.
            redirectToDashboard(currentUser.uid)
        } else {
            // Nenhum usuário logado, exibe a tela de login.
            showLoginScreen()
        }
    }

    private fun showLoginScreen() {
        setContentView(R.layout.activity_login)

        val emailField = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val loginButton: Button = findViewById(R.id.btn_login)
        val createAccountButton: Button = findViewById(R.id.btn_cadastrar)

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
                    if (userId != null) {
                        redirectToDashboard(userId)
                    } else {
                        Toast.makeText(this, "Erro interno: usuário inválido", Toast.LENGTH_LONG).show()
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

    private fun redirectToDashboard(userId: String) {
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val tipo = doc.getString("tipo") // Correção: Usando o campo "tipo"

                    val targetActivity = when (tipo) {
                        "cliente" -> MainActivity::class.java
                        "salao" -> SalaoActivity::class.java
                        else -> null
                    }

                    if (targetActivity != null) {
                        startActivity(Intent(this, targetActivity))
                        finish() // Finaliza a LoginActivity em caso de sucesso
                    } else {
                        // Tipo de usuário desconhecido, desconecta e mostra a tela de login
                        Toast.makeText(this, "Tipo de usuário desconhecido", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        showLoginScreen()
                    }
                } else {
                    // Documento não existe, desconecta e mostra a tela de login
                    Toast.makeText(this, "Usuário não encontrado no banco de dados", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                    showLoginScreen()
                }
            }
            .addOnFailureListener { e ->
                // Erro no Firestore, desconecta e mostra a tela de login
                Toast.makeText(this, "Erro ao acessar Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
                auth.signOut()
                showLoginScreen()
            }
    }
}
