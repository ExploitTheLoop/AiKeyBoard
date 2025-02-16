package net.animetone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

class UpdateJobService : JobService() {

    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private val CHANNEL_ID = "update_channel"

    override fun onStartJob(params: JobParameters?): Boolean {
        // Initialize Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        // Perform the update check
        checkForUpdates()
        return true // Return true because the job is running asynchronously
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Return false to indicate that the job does not need to be rescheduled
        return false
    }

    private fun checkForUpdates() {
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val newVersionCode = mFirebaseRemoteConfig.getString("new_version_code")
                try {

                    Log.d("appversion", getVersionCode().toString()) //returning 2
                    Log.d("FirebaseVersion", newVersionCode) //returning 1

                    if (newVersionCode.toInt() > getVersionCode()) {
                        // Push a notification if a new update is available
                        Log.d("UpdateJobService", "New version available")
                        UpdateStatusManager.setUpdateFound(true) // Indicating that an update is found
                        showNotification("Update Available", "A new version of the app is available. Update now!")
                    } else {
                        UpdateStatusManager.setUpdateFound(false)

                        Log.d("UpdateJobService", "App is up to date")
                    }
                } catch (e: NumberFormatException) {
                    Log.e("UpdateJobService", "Invalid version code fetched: $newVersionCode")
                }
            } else {
                Log.e("UpdateJobService", "Remote config fetch failed")
            }
        }
    }

    private fun getVersionCode(): Int {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                (pInfo.longVersionCode and 0xFFFFFFFF).toInt()
            } else {
                pInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e("UpdateJobService", "Error fetching version code: ${e.message}")
            -1
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Update Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifications for app updates"
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent to open a URL when the notification is clicked
        val url = "https://t.me/animetonee" // Replace with your desired URL
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(url)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.chatbot) // Replace with your app's notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Dismiss notification when clicked
            .build()

        // Show the notification
        notificationManager.notify(1, notification)
    }

}
