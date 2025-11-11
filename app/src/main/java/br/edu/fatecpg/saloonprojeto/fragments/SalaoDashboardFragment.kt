package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R

class SalaoDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_salao, container, false)

        val manageServicesButton = view.findViewById<Button>(R.id.manage_services_button)
        manageServicesButton.setOnClickListener {
            findNavController().navigate(R.id.action_salao_dashboard_to_salao_gerenciar)
        }

        return view
    }
}