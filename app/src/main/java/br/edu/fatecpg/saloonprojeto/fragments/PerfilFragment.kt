package br.edu.fatecpg.saloonprojeto.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.LoginActivity
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private lateinit var nomePerfil: TextView
    private lateinit var emailPerfil: TextView
    private lateinit var telefonePerfil: TextView
    private lateinit var btnLogout: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        nomePerfil = view.findViewById(R.id.txv_nome)
        emailPerfil = view.findViewById(R.id.txv_email)
        telefonePerfil = view.findViewById(R.id.txv_telefone)
        btnLogout = view.findViewById(R.id.btn_logout)

        // Botão editar perfil
        val editProfileButton = view.findViewById<Button>(R.id.btn_editar_perfil)
        editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_perfilFragment_to_editarPerfilFragment)
        }

        // Botão logout
        btnLogout.setOnClickListener {
            auth.signOut()
            activity?.let {
                val intent = Intent(it, LoginActivity::class.java)
                it.startActivity(intent)
                it.finish()
            }
        }

        // Primeira carga ao abrir o fragment
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        //  Recarrega os dados quando volta da tela de edição
        loadUserProfile()
    }

    private fun formatPhoneNumber(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        if (digits.length < 10) {
            return phone
        }

        val ddd = digits.substring(0, 2)
        val numberPart = digits.substring(2)

        val formattedNumber = if (numberPart.length == 9) {
            "${numberPart.substring(0, 5)}-${numberPart.substring(5)}"
        } else {
            "${numberPart.substring(0, 4)}-${numberPart.substring(4)}"
        }

        return "($ddd) $formattedNumber"
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userType = document.getString("tipo")

                        if (userType == "salao") {
                            nomePerfil.text = document.getString("nomeSalao")
                        } else {
                            nomePerfil.text = document.getString("nome")
                        }

                        emailPerfil.text = document.getString("email")

                        val telefone = document.getString("telefone")
                        if (telefone != null) {
                            telefonePerfil.text = formatPhoneNumber(telefone)
                        }
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
}
