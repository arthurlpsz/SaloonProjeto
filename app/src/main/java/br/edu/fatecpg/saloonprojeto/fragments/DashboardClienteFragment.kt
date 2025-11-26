package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardClienteFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_cliente, container, false)

        val fabVerServicos = view.findViewById<FloatingActionButton>(R.id.fab_ver_servicos)
        val fabPerfil = view.findViewById<FloatingActionButton>(R.id.fab_perfil)

        fabVerServicos.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_salaoServicos)
        }

        fabPerfil.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_perfil)
        }

        return view
    }
}
