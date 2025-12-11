package br.edu.fatecpg.saloonprojeto

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager

class SaloonApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Inicializa o Cloudinary
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isNotEmpty()) {
            try {
                val config = mapOf(
                    "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                    "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                    "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
                )
                MediaManager.init(this, config)
                Log.i("SaloonApplication", "Cloudinary inicializado com sucesso.")
            } catch (e: Exception) {
                Log.e("SaloonApplication", "Falha ao inicializar o Cloudinary", e)
            }
        } else {
            Log.w("SaloonApplication", "Credenciais do Cloudinary não configuradas. Upload de imagens será desativado.")
        }
    }
}
