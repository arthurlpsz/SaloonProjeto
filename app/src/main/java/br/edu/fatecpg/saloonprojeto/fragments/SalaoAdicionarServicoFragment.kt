package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import br.edu.fatecpg.saloonprojeto.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SalaoAdicionarServicoFragment : Fragment() {

    private lateinit var edtNome: EditText
    private lateinit var edtDescricao: EditText
    private lateinit var edtPreco: EditText
    private lateinit var edtDuracao: EditText
    private lateinit var btnSalvar: Button
    private lateinit var btnCancelar: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_salao_adicionar_servico, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edtNome = view.findViewById(R.id.et_service_name)
        edtDescricao = view.findViewById(R.id.et_service_description)
        edtPreco = view.findViewById(R.id.et_service_price)
        edtDuracao = view.findViewById(R.id.et_service_duration)

        btnSalvar = view.findViewById(R.id.btn_save_service)
        btnCancelar = view.findViewById(R.id.btn_cancel)

        btnSalvar.setOnClickListener { salvarServico() }
        btnCancelar.setOnClickListener { requireActivity().onBackPressed() }
    }

    private fun salvarServico() {
        val nome = edtNome.text.toString().trim()
        val descricao = edtDescricao.text.toString().trim()
        val preco = edtPreco.text.toString().trim().toDoubleOrNull()
        val duracao = edtDuracao.text.toString().trim().toIntOrNull()

        if (nome.isEmpty() || descricao.isEmpty() || preco == null || duracao == null) {
            Toast.makeText(requireContext(), "Preencha todos os campos corretamente", Toast.LENGTH_SHORT).show()
            return
        }

        val salaoId = auth.currentUser?.uid ?: return

        val dados = hashMapOf(
            "salaoId" to salaoId,
            "nome" to nome,
            "descricao" to descricao,
            "preco" to preco,
            "duracaoMin" to duracao,
            "createdAt" to Timestamp.now()
        )

        db.collection("servicos").add(dados)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Serviço cadastrado!", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
