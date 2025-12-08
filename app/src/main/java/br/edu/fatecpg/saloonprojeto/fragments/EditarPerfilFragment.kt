package br.edu.fatecpg.saloonprojeto.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import br.edu.fatecpg.saloonprojeto.R
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.hdodenhof.circleimageview.CircleImageView

class EditarPerfilFragment : Fragment() {

    // Views
    private lateinit var nomeTitulo: TextView
    private lateinit var edtNome: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtTelefone: EditText
    private lateinit var edtEndereco: EditText
    private lateinit var profileImage: CircleImageView
    private lateinit var btnChangePhoto: Button
    private lateinit var btnSave: Button

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userId: String? = null
    private var userType: String? = null

    // Image data
    private var imageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            if (isAdded) { // Garante que o fragmento está anexado
                Glide.with(this).load(it).circleCrop().into(profileImage)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editar_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid

        nomeTitulo = view.findViewById(R.id.name_label)
        edtNome = view.findViewById(R.id.edt_nome)
        edtEmail = view.findViewById(R.id.edt_email)
        edtTelefone = view.findViewById(R.id.edt_telefone)
        edtEndereco = view.findViewById(R.id.edt_endereco)
        profileImage = view.findViewById(R.id.profile_image)
        btnChangePhoto = view.findViewById(R.id.btn_change_photo)
        btnSave = view.findViewById(R.id.btn_salvar)

        val backArrow = view.findViewById<ImageView>(R.id.ic_voltar)
        backArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnSave.setOnClickListener {
            if (imageUri != null) {
                uploadImageThenSaveData()
            } else {
                saveUserProfileData(null)
            }
        }

        loadUserProfile()
    }

    private fun loadUserProfile() {
        userId?.let {
            db.collection("usuarios").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null && isAdded) { // A verificação isAdded protege contra crashes
                        userType = document.getString("tipo")

                        if (userType == "salao") {
                            nomeTitulo.text = "Nome do Salão"
                            edtNome.setText(document.getString("nomeSalao"))
                        } else {
                            nomeTitulo.text = "Nome"
                            edtNome.setText(document.getString("nome"))
                        }
                        edtEmail.setText(document.getString("email"))
                        edtTelefone.setText(document.getString("telefone"))
                        edtEndereco.setText(document.getString("endereco"))

                        val currentImageUrl = document.getString("fotoUrl")
                        Glide.with(this)
                            .load(currentImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(profileImage)
                    }
                }
        }
    }

    private fun uploadImageThenSaveData() {
        imageUri?.let { uri ->
            try {
                // Verificação de segurança: garante que o MediaManager está inicializado.
                MediaManager.get()
            } catch (e: IllegalStateException) {
                Log.e("EditarPerfilFragment", "MediaManager não inicializado.", e)
                if (isAdded) {
                    Toast.makeText(context, "Erro no serviço de upload. Tente novamente mais tarde.", Toast.LENGTH_LONG).show()
                }
                return
            }

            if (!isAdded) return
            Toast.makeText(context, "Salvando foto...", Toast.LENGTH_SHORT).show()
            MediaManager.get().upload(uri).callback(object : UploadCallback {
                override fun onStart(requestId: String) {}

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val secureUrl = resultData["secure_url"] as? String
                    saveUserProfileData(secureUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("EditarPerfilFragment", "Cloudinary Error: ${error.description}")
                    if (isAdded) {
                        Toast.makeText(context, "Falha no upload da imagem.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Opcional: Logar se a tarefa for reagendada
                    Log.d("EditarPerfilFragment", "Upload reagendado: $requestId")
                }
            }).dispatch()
        }
    }

    private fun saveUserProfileData(newPhotoUrl: String?) {
        // Proteção crucial: não faz nada se a view já foi destruída
        if (!isAdded) return

        val nome = edtNome.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val telefone = edtTelefone.text.toString().trim()
        val endereco = edtEndereco.text.toString().trim()

        if (nome.isEmpty() || email.isEmpty() || telefone.isEmpty() || endereco.isEmpty()) {
            Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val userUpdates = hashMapOf<String, Any>(
            "email" to email,
            "telefone" to telefone,
            "endereco" to endereco
        )

        if (userType == "salao") {
            userUpdates["nomeSalao"] = nome
        } else {
            userUpdates["nome"] = nome
        }

        if (newPhotoUrl != null) {
            userUpdates["fotoUrl"] = newPhotoUrl
        }

        userId?.let {
            db.collection("usuarios").document(it).set(userUpdates, SetOptions.merge())
                .addOnSuccessListener {
                    if (isAdded) { // Verificação de segurança final
                        Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                }
                .addOnFailureListener { exception ->
                    if (isAdded) {
                        Toast.makeText(context, "Erro ao atualizar perfil: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
