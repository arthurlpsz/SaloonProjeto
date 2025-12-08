package br.edu.fatecpg.saloonprojeto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import de.hdodenhof.circleimageview.CircleImageView

class IntroActivity : AppCompatActivity() {

    private lateinit var imgProfile: CircleImageView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var imageUri: Uri? = null
    private var userId: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            Glide.with(this).load(it).circleCrop().into(imgProfile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_intro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        userId = intent.getStringExtra("userId")

        if (userId == null) {
            Toast.makeText(this, "Erro: ID do usuário não encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        imgProfile = findViewById(R.id.img_profile)
        btnSelectPhoto = findViewById(R.id.btn_select_photo)
        btnSave = findViewById(R.id.btn_save)
        progressBar = findViewById(R.id.progress_bar)

        btnSelectPhoto.setOnClickListener {
            openFileChooser()
        }

        btnSave.setOnClickListener {
            uploadFileAndContinue()
        }
    }

    private fun openFileChooser() {
        pickImageLauncher.launch("image/*")
    }

    private fun uploadFileAndContinue() {
        if (imageUri == null) {
            redirectToDashboard()
            return
        }

        try {
            // Verificação de segurança: garante que o MediaManager está inicializado.
            MediaManager.get()
        } catch (e: IllegalStateException) {
            Log.e("IntroActivity", "MediaManager não inicializado.", e)
            Toast.makeText(this, "Erro no serviço de upload. Tente novamente mais tarde.", Toast.LENGTH_LONG).show()
            return
        }

        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    progressBar.visibility = View.VISIBLE
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    progressBar.visibility = View.GONE
                    val secureUrl = resultData["secure_url"] as? String
                    if (secureUrl != null) {
                        updateUserPhoto(secureUrl)
                    } else {
                        Toast.makeText(this@IntroActivity, "Não foi possível obter a URL da imagem.", Toast.LENGTH_LONG).show()
                        redirectToDashboard()
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    progressBar.visibility = View.GONE
                    Log.e("IntroActivity", "Cloudinary Upload Error: ${error.description}")
                    Toast.makeText(this@IntroActivity, "Falha no upload. Continuando sem foto.", Toast.LENGTH_LONG).show()
                    redirectToDashboard()
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            }).dispatch()
    }

    private fun updateUserPhoto(photoUrl: String) {
        if (userId == null) return

        val photoUpdate = mapOf("fotoUrl" to photoUrl)

        db.collection("usuarios").document(userId!!)
            .set(photoUpdate, SetOptions.merge()) // Usa set com merge para segurança
            .addOnSuccessListener {
                Toast.makeText(this, "Foto de perfil salva com sucesso!", Toast.LENGTH_SHORT).show()
                redirectToDashboard()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("IntroActivity", "Firestore update failed", e)
                Toast.makeText(this, "Falha ao salvar URL da foto. Continuando.", Toast.LENGTH_SHORT).show()
                redirectToDashboard()
            }
    }

    private fun redirectToDashboard() {
        if (userId == null) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        db.collection("usuarios").document(userId!!).get()
            .addOnSuccessListener { document ->
                val userType = document?.getString("tipo")
                val intent = if (userType == "salao") {
                    Intent(this, SalaoActivity::class.java)
                } else {
                    Intent(this, MainActivity::class.java)
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("IntroActivity", "Redirect failed", e)
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
    }
}
