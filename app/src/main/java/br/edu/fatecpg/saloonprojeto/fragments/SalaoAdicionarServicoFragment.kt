package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R

class SalaoAdicionarServicoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_salao_adicionar_servico, container, false)

        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        return view
    }
}