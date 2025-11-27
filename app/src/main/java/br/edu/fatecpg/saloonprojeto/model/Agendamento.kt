package br.edu.fatecpg.saloonprojeto.model

import com.google.firebase.Timestamp

data class Agendamento(
    val id: String = "",
    val clienteId: String = "",
    val salaoId: String = "",
    val servicoId: String = "",
    val nomeUsuario: String = "",
    val nomeSalao: String = "",
    val servico: String = "",
    val dataInicio: Timestamp? = null,
    val dataFim: Timestamp? = null,
    val status: String = "",
    val createdAt: Timestamp? = null
)
