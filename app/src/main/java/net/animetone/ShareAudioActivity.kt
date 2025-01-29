package net.animetone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

class ShareAudioActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSION_CODE = 100
        private const val FILE_NAME = "downloaded_audio.mp3"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_audio)

        // Hide the ActionBar
        supportActionBar?.hide()

        // Set system bar colors

        setSystemBarColors()
        // Get the file path using the correct method based on SDK version
        val downloadedFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped storage for Android 10 and above
            File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
        } else {
            // Legacy storage for older Android versions
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
        }

        // Check if file exists
        if (downloadedFile.exists()) {
            // For Android 10 and above, Scoped Storage applies, we need to request permission for older versions
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED) {
                // Request permission if not granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_CODE
                )
            } else {
                // Permission granted or not needed, proceed with sharing
                shareAudioFile(downloadedFile)
            }
        } else {
            Toast.makeText(this, "Audio file not found!", Toast.LENGTH_SHORT).show()
        }
    }
    private fun setSystemBarColors() {
        window?.apply {
            // Set navigation bar color
            // navigationBarColor = ContextCompat.getColor(this@PredictionSettingsListActivity, R.color.your_navigation_bar_color)

            // Set status bar color
            statusBarColor = ContextCompat.getColor(this@ShareAudioActivity, R.color.white)

            // Ensure status bar icons are white
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = 0 // Clear all flags related to system UI visibility
            }

        }
    }
    private fun shareAudioFile(file: File) {
        // For Android 10 and above, we must use FileProvider to share the file

        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Scoped storage: Use FileProvider for sharing files
            FileProvider.getUriForFile(
                this,
                "${packageName}.provider", // Ensure the provider name is correct in your AndroidManifest.xml
                file
            )

        } else {
            // Legacy storage: Directly use the file path (already publicly accessible)

            Uri.fromFile(file)
        }

        // Share intent setup
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*" // Specify MIME type for audio files
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read the URI
        }

        // Start the chooser
        startActivity(Intent.createChooser(shareIntent, "Share Audio"))
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sharing
                val downloadedFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Scoped storage: Use getExternalFilesDir for Android 10+
                    File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
                } else {
                    // Legacy storage: Use Environment.getExternalStoragePublicDirectory for older versions
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE_NAME)
                }

                // Check if the file exists before attempting to share
                if (downloadedFile.exists()) {
                    shareAudioFile(downloadedFile)
                } else {
                    Toast.makeText(this, "Audio file not found!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied to read external storage", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

