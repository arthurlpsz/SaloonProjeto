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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoEditarServicoFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var servicoId: String? = null

    private lateinit var etNome: EditText
    private lateinit var etDescricao: EditText
    private lateinit var etPreco: EditText
    private lateinit var etDuracao: EditText

    // Header Views
    private lateinit var profileImage: ImageView
    private lateinit var userName: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_salao_editar_servico, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        servicoId = arguments?.getString("servicoId")

        etNome = view.findViewById(R.id.et_service_name)
        etDescricao = view.findViewById(R.id.et_service_description)
        etPreco = view.findViewById(R.id.et_service_price)
        etDuracao = view.findViewById(R.id.et_service_duration)

        val headerView = view.findViewById<View>(R.id.header_view)
        profileImage = headerView.findViewById(R.id.profile_image)
        userName = headerView.findViewById(R.id.user_name)

        val btnSalvar = view.findViewById<Button>(R.id.btn_save_service)
        val btnCancelar = view.findViewById<Button>(R.id.btn_cancel)

        btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSalvar.setOnClickListener {
            updateServico()
        }

        loadHeaderInfo()
        loadServicoData()
    }

    private fun loadHeaderInfo() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists() && isAdded) {
                        val name = document.getString("nome")
                        userName.text = name ?: "Nome não encontrado"

                        val imageUrl = document.getString("fotoUrl")
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .centerCrop()
                            .into(profileImage)
                    }
                }
        }
    }

    private fun loadServicoData() {
        servicoId?.let {
            db.collection("servicos").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        etNome.setText(document.getString("nome"))
                        etDescricao.setText(document.getString("descricao"))
                        etPreco.setText(document.getDouble("preco").toString())
                        // Ajuste para "duracaoMin" como em AgendarFragment
                        etDuracao.setText(document.getLong("duracaoMin").toString())
                    } else {
                        Toast.makeText(requireContext(), "Serviço não encontrado.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Erro ao carregar dados do serviço.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
        }
    }

    private fun updateServico() {
        val nome = etNome.text.toString().trim()
        val descricao = etDescricao.text.toString().trim()
        val preco = etPreco.text.toString().toDoubleOrNull()
        val duracao = etDuracao.text.toString().toIntOrNull()

        if (nome.isEmpty() || preco == null || duracao == null) {
            Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        servicoId?.let {
            val servico = hashMapOf(
                "nome" to nome,
                "descricao" to descricao,
                "preco" to preco,
                // Ajuste para "duracaoMin" como em AgendarFragment
                "duracaoMin" to duracao
            )

            db.collection("servicos").document(it)
                .update(servico as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Serviço atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erro ao atualizar serviço: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
