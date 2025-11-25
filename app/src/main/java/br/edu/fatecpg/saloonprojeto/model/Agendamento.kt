package br.edu.fatecpg.saloonprojeto.model

import com.google.firebase.Timestamp

data class Agendamento(
    val id: String = "",
    val userId: String = "",
    val salaoId: String = "",
    val nomeUsuario: String = "",
    val nomeSalao: String = "",
    val data: Timestamp? = null,
    val servico: String = "",
    val status: String = ""
)
