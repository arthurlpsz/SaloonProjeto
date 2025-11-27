package br.edu.fatecpg.saloonprojeto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import br.edu.fatecpg.saloonprojeto.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class SalaoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_salao)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.salao_nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.salao_bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)
    }
}
