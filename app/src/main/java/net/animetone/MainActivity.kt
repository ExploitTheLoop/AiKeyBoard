package net.animetone
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import android.provider.Settings
import net.animetone.auth.SignInFragment
import net.animetone.databinding.ActivityMainBinding
import net.animetone.predictionSettings.PredictionSettingsListActivity
import net.animetone.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        analytics = Firebase.analytics
        // Schedule the job to check for updates periodically
        scheduleUpdateJob()

        // Retrieve the boolean value from the Intent
        val comingFromSettings = intent.getBooleanExtra("coming_from_settings", false) // Default is false

        if(/*!isKeyboardEnabled() || !isKeyboardDefault() || */comingFromSettings){
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, SettingsFragment())
                .commit()
        }else{
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, SignInFragment())
                .commit()
            // val intent = Intent(this, PredictionSettingsListActivity::class.java)
            // startActivity(intent)
            // finish()
        }

    }

    private fun scheduleUpdateJob() {
        val jobScheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(this, UpdateJobService::class.java)

        val jobInfo = JobInfo.Builder(1, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)  // Optional: Only run on unmetered network
            .setPersisted(true)  // Persist the job across reboots
            .setPeriodic(15 * 60 * 1000) // Run every 15 minutes
            .build()

        val result = jobScheduler.schedule(jobInfo)
        if (result == JobScheduler.RESULT_SUCCESS) {
            // Job scheduled successfully
            Log.d("MainActivity", "Job scheduled to check for updates every 15 minutes.")
        } else {
            // Job scheduling failed
            Log.e("MainActivity", "Job scheduling failed.")
        }
    }

    private fun getMyKeyboardPackageName(): String {
        // Return your app's package name
        return applicationContext.packageName
    }

    private fun getMyKeyboardServiceName(): String {
        // Return your keyboard service's full component name
        return "$packageName/.KeyboardService" // Replace `.YourKeyboardService` with your actual service name
    }

    private fun isKeyboardEnabled(): Boolean {
        // Get the list of enabled input methods
        val enabledMethods = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS)

        // Get your app's keyboard package name
        val myKeyboardPackageName = getMyKeyboardPackageName()

        // Check if your keyboard's package name is in the enabled methods
        return enabledMethods?.split(":")?.any { it.startsWith(myKeyboardPackageName) } == true
    }

    private fun isKeyboardDefault(): Boolean {
        // Get the current default input method (package/service)
        val defaultInputMethod = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

        // Get your app's keyboard service name
        val myKeyboardServiceName = getMyKeyboardServiceName()

        // Compare the current default input method with your custom keyboard's service name
        return defaultInputMethod == myKeyboardServiceName
    }


}
