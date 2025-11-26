package br.edu.fatecpg.saloonprojeto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SalaoDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard_salao, container, false)

        // Botões do Dashboard
        val fabAdicionarServico = view.findViewById<FloatingActionButton>(R.id.fab_adicionar_servico)
        val fabDefinirHorario = view.findViewById<FloatingActionButton>(R.id.fab_definir_horario)
        val fabPerfil = view.findViewById<FloatingActionButton>(R.id.fab_perfil)
        val fabAgendamentos = view.findViewById<FloatingActionButton>(R.id.fab_agendamentos)
        val fabServicos = view.findViewById<FloatingActionButton>(R.id.fab_servicos)

        fabAdicionarServico.setOnClickListener {
            findNavController().navigate(R.id.action_salaoDashboard_to_adicionarServico)
        }

        fabDefinirHorario.setOnClickListener {
            findNavController().navigate(R.id.action_salaoDashboard_to_definirHorario)
        }

        fabPerfil.setOnClickListener {
            findNavController().navigate(R.id.action_salaoDashboard_to_perfil)
        }

        fabAgendamentos.setOnClickListener {
            findNavController().navigate(R.id.action_salaoDashboard_to_agendamentos)
        }

        fabServicos.setOnClickListener {
            findNavController().navigate(R.id.action_salaoDashboard_to_salaoServicos)
        }

        return view
    }
}
