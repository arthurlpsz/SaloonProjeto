package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditarPerfilFragment : Fragment() {

    private lateinit var tilNome: TextInputLayout
    private lateinit var etNome: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etTelefone: TextInputEditText
    private lateinit var etEndereco: TextInputEditText

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

        tilNome = view.findViewById(R.id.til_nome)
        etNome = view.findViewById(R.id.et_nome)
        etEmail = view.findViewById(R.id.et_email)
        etTelefone = view.findViewById(R.id.et_telefone)
        etEndereco = view.findViewById(R.id.et_endereco)

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
                            tilNome.hint = "Nome do Salão"
                            etNome.setText(document.getString("nomeSalao"))
                        } else {
                            tilNome.hint = "Nome"
                            etNome.setText(document.getString("nome"))
                        }
                        etEmail.setText(document.getString("email"))
                        etTelefone.setText(document.getString("telefone"))
                        etEndereco.setText(document.getString("endereco"))
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Erro ao carregar perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserProfile() {
        val nome = etNome.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val telefone = etTelefone.text.toString().trim()
        val endereco = etEndereco.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || endereco.isEmpty()) {
            Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

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
            db.collection("usuarios").document(it).update(userUpdates)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Erro ao atualizar perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
