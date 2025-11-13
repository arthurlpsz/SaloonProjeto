package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoAdicionarServicoFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_adicionar_servico, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val etNome = view.findViewById<EditText>(R.id.et_service_name)
        val etDescricao = view.findViewById<EditText>(R.id.et_service_description)
        val etPreco = view.findViewById<EditText>(R.id.et_service_price)
        val etDuracao = view.findViewById<EditText>(R.id.et_service_duration)

        val btnSalvar = view.findViewById<Button>(R.id.btn_save_service)
        val btnCancelar = view.findViewById<Button>(R.id.btn_cancel)

        btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSalvar.setOnClickListener {
            val nome = etNome.text.toString().trim()
            val descricao = etDescricao.text.toString().trim()
            val preco = etPreco.text.toString().trim()
            val duracao = etDuracao.text.toString().trim()
            val salaoId = auth.currentUser?.uid

            if (salaoId == null) {
                Toast.makeText(requireContext(), "Erro: usuário não autenticado.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (nome.isEmpty() || preco.isEmpty() || duracao.isEmpty()) {
                Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val servico = hashMapOf(
                "salaoId" to salaoId,
                "nome" to nome,
                "descricao" to descricao,
                "preco" to preco.toDoubleOrNull(),
                "duracao" to duracao.toIntOrNull()
            )

            db.collection("servicos")
                .add(servico)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Serviço cadastrado com sucesso!", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack() // volta para lista
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Erro ao cadastrar serviço: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        return view
    }
}
