package br.edu.fatecpg.saloonprojeto

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView

class IntroActivity : AppCompatActivity() {

    private lateinit var imgProfile: CircleImageView
    private lateinit var btnSelectPhoto: Button
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var imageUri: Uri? = null
    private var userId: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

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
            uploadFile()
        }
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            imgProfile.setImageURI(imageUri)
        }
    }

    private fun uploadFile() {
        if (imageUri != null && userId != null) {
            progressBar.visibility = View.VISIBLE

            val storageRef = storage.reference.child("profile_images/$userId.jpg")

            storageRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val fotoUrl = uri.toString()
                        db.collection("usuarios").document(userId!!)
                            .update("fotoUrl", fotoUrl)
                            .addOnSuccessListener {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Foto de perfil salva com sucesso!", Toast.LENGTH_SHORT).show()
                                
                                db.collection("usuarios").document(userId!!).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null && document.exists()) {
                                            val userType = document.getString("tipo")
                                            val intent = if (userType == "salao") {
                                                Intent(this, SalaoActivity::class.java)
                                            } else {
                                                Intent(this, ClienteActivity::class.java)
                                            }
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            // Fallback to login
                                            val intent = Intent(this, LoginActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                    }
                                    .addOnFailureListener {
                                        // Fallback to login
                                        val intent = Intent(this, LoginActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Erro ao salvar a foto: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erro no upload: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Por favor, selecione uma foto.", Toast.LENGTH_SHORT).show()
        }
    }
}
