package br.edu.fatecpg.saloonprojeto.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

@Keep // Garante que a classe não seja ofuscada pelo ProGuard
data class Agendamento(
    @DocumentId // Mapeia automaticamente o ID do documento para este campo
    var id: String = "",
    var clienteId: String = "",
    var salaoId: String = "",
    var servicoId: String = "",
    var nomeUsuario: String = "",
    var nomeSalao: String = "",
    var servico: String = "",
    var dataInicio: Timestamp? = null,
    var dataFim: Timestamp? = null,
    var status: String = "",
    var createdAt: Timestamp? = null
) {
    // Construtor vazio explícito para garantir compatibilidade máxima com o Firebase.
    constructor() : this("", "", "", "", "", "", "", null, null, "", null)
}
