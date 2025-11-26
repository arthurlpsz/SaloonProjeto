package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilFragment : Fragment() {

    private lateinit var nomeTitulo: TextView
    private lateinit var edtNome: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtTelefone: EditText
    private lateinit var edtEndereco: EditText

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private var userType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editar_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid

        nomeTitulo = view.findViewById(R.id.name_label)
        edtNome = view.findViewById(R.id.edt_nome)
        edtEmail = view.findViewById(R.id.edt_email)
        edtTelefone = view.findViewById(R.id.edt_telefone)
        edtEndereco = view.findViewById(R.id.edt_endereco)

        val backArrow = view.findViewById<ImageView>(R.id.iv_back)
        backArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        val saveButton = view.findViewById<Button>(R.id.btn_salvar)
        saveButton.setOnClickListener {
            saveUserProfile()
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        userId?.let {
            db.collection("usuarios").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userType = document.getString("tipo")
                        if (userType == "salao") {
                            nomeTitulo.text = "Nome do Salão"
                            edtNome.setText(document.getString("nomeSalao"))
                        } else {
                            nomeTitulo.text = "Nome"
                            edtNome.setText(document.getString("nome"))
                        }
                        edtEmail.setText(document.getString("email"))
                        edtTelefone.setText(document.getString("telefone"))
                        edtEndereco.setText(document.getString("endereco"))
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        requireContext(),
                        "Erro ao carregar perfil: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun saveUserProfile() {
        val nome = edtNome.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val telefone = edtTelefone.text.toString().trim()
        val endereco = edtEndereco.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || endereco.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser

        // 1️⃣ Atualizar e-mail no FirebaseAuth
        user?.updateEmail(email)
            ?.addOnSuccessListener {

                // 2️⃣ Depois atualizar no Firestore
                val userUpdates = hashMapOf<String, Any>(
                    "email" to email,
                    "telefone" to telefone,
                    "endereco" to endereco
                )

                if (userType == "salao") {
                    userUpdates["nomeSalao"] = nome
                } else {
                    userUpdates["nome"] = nome
                }

                userId?.let {
                    db.collection("usuarios").document(it)
                        .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Perfil atualizado com sucesso!",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(
                                requireContext(),
                                "Erro ao atualizar Firestore: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

            }
            ?.addOnFailureListener { exception ->
                Toast.makeText(
                    requireContext(),
                    "Erro ao atualizar e-mail: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

}
