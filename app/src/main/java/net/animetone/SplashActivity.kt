package net.animetone

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        // Hide the ActionBar
        supportActionBar?.hide()
        setSystemBarColors()
        // Delay for 5 seconds (5000 ms) before moving to MainActivity



        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Ensure the splash screen activity is removed from the back stack
        }, 5000)
    }

    private fun setSystemBarColors() {
        window?.apply {
            // Set navigation bar color
            navigationBarColor = ContextCompat.getColor(this@SplashActivity, R.color.your_navigation_bar_color)

            // Set status bar color
            statusBarColor = ContextCompat.getColor(this@SplashActivity, R.color.your_status_bar_color)

            // Ensure status bar icons are white
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = 0 // Clear all flags related to system UI visibility
            }
        }
    }

}