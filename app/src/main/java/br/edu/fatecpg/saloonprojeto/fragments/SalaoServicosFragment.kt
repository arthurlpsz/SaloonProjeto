package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoServicosFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_servicos, container, false)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        val headerView = view.findViewById<View>(R.id.header_view)
        userName = headerView.findViewById(R.id.user_name)
        loadUserName()
        return view
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val userType = document.getString("tipo")
                        if (userType == "salao") {
                            userName.text = document.getString("nomeSalao")
                        } else {
                            userName.text = document.getString("nome")
                        }
                    }
                }
        }
    }
}