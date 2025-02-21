package net.animetone

import android.app.Activity
import android.app.DownloadManager
import android.app.VoiceInteractor
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.ExtractedTextRequest
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.ktor.client.request.setBody

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class KeyboardService : View.OnClickListener, InputMethodService() {

    // Declare API key as a global variable
    private val groqapikey = "Bearer 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" // Replace with your actual API key
    private val groqapiurl = "https://api.groq.com/openai/v1/chat/completions"

    private val murfapikey = "ap2_79b814cd-518e-4152-9484-ee18c981e69d" // Replace with your actual API key
    private val murfapiurl = "https://api.murf.ai/v1/speech/generate"
    private var audioUrl: String? = null
    private var capsOn = false
    private lateinit var mainView: View
    private lateinit var suggestions: Sequence<View>

    private lateinit var app: KeyboardGPTApp
    private lateinit var sharedPreferences: SharedPreferences
    private val prefsKey = "prediction_settings"

    lateinit var primaryKeyboard: LinearLayout
    lateinit var symbolsKeyboard: LinearLayout
    lateinit var voicegen: LinearLayout
    lateinit var emojiview: LinearLayout
   // lateinit var spinner: Spinner

    // Declare the ImageButton as a global variable
    private lateinit var AiCallButton: ImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var predictionAdapter: PredictionAdapter
    // Declare the selected item as a global variable
    private var item: String? = null

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var clipboardCard: LinearLayout
    private lateinit var clipboardButton: LinearLayout
    private lateinit var clipboardText: TextView
    private lateinit var Nodata: LinearLayout

    private lateinit var Update_container: LinearLayout

    private var isUpdateCheckRunning = true // Control flag for update check loop

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var isVoiceMode = false // State variable to track current mode
    private var isEmojiMode = false // State variable to track current mode

    private lateinit var languageSpinner: Spinner
    private var selectedLanguage: String = ""
    private lateinit var gridview: RecyclerView
    private lateinit var playText: TextView
    private lateinit var lottieProgress: LottieAnimationView
    private lateinit var downloadButton: LottieAnimationView
    private lateinit var sharebutton: LottieAnimationView

    private lateinit var tokenTextView: TextView

    private var selectedItem: ListItem? = null
    private lateinit var voiceactoradapter: voiceactorAdapter
    private lateinit var playButton: FrameLayout
    private lateinit var activityTracker: ActivityTracker

    private val englishData = listOf(
        ListItem(R.raw.female, "Ruby"),
        ListItem(R.raw.female, "Hazel"),
        ListItem(R.raw.male, "Harrison"),
        ListItem(R.raw.female, "Heidi"),
        ListItem(R.raw.male, "Freddie"),
        ListItem(R.raw.female, "Juliet"),
        ListItem(R.raw.male, "Hugo"),
        ListItem(R.raw.male, "Jaxon"),
        ListItem(R.raw.male, "Peter"),
        ListItem(R.raw.female, "Katie")
    )
    private val hindiData = listOf(
        ListItem(R.raw.male, "Kabir"),
        ListItem(R.raw.female, "Ayushi"),
        ListItem(R.raw.male, "Shaan"),
        ListItem(R.raw.male, "Rahul"),
        ListItem(R.raw.female, "Shweta"),
        ListItem(R.raw.male, "Amit")
    )
    private val bengaliData = listOf(
        ListItem(R.raw.female, "Anwesha"),
        ListItem(R.raw.female, "Ishani"),
        ListItem(R.raw.male, "Abhik")
    )

    @Override
    override fun onCreateInputView(): View {
        app = application as KeyboardGPTApp


        mainView = layoutInflater.inflate(R.layout.keyboard_view_primary_english, null)
        suggestions = mainView.findViewById<LinearLayout>(R.id.suggestions_layout).children



        // Initialize the button
        AiCallButton  = mainView.findViewById(R.id.ai_api_call);
        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize ClipboardManager
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        activityTracker = (applicationContext as KeyboardGPTApp).activityTracker

        clipboardCard = mainView.findViewById(R.id.clipboardcard)
        clipboardButton = mainView.findViewById(R.id.clipboarbutton)
        clipboardText = mainView.findViewById(R.id.clipboardtext)
        Nodata = mainView.findViewById(R.id.nodatatext)
        Nodata.visibility = View.VISIBLE
        val screenWidth = resources.displayMetrics.widthPixels
        val movingTextView = mainView.findViewById<TextView>(R.id.nodaataanimtext)

        movingTextView.isSelected = true


        Update_container = mainView.findViewById(R.id.update_container)

        val updateTextView: TextView = mainView.findViewById(R.id.update_text)

        val spannableText = SpannableString("New version available. Update.")

        // Apply an underline span only to the word "Update"
        val startIndex = spannableText.indexOf("Update", ignoreCase = true)
        if (startIndex != -1) {
            spannableText.setSpan(
                UnderlineSpan(),
                startIndex,
                startIndex + "Update".length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Set the styled text to the TextView
        updateTextView.text = spannableText


        updateTextView.setOnClickListener {
            try {
                val url = "https://t.me/animetonee" // Replace with your desired URL
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)

                // Use applicationContext from Service for opening the URL
                val context = applicationContext
                if (context != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // Important in Service
                    context.startActivity(intent) // Open the URL
                } else {
                    Log.e("URL Intent", "Context is null, cannot open the URL")
                }
            } catch (e: Exception) {
                Log.e("URL Intent", "Error opening URL", e)
            }
        }


        val lottieView = suggestions.elementAtOrNull(0) as LottieAnimationView
        setupLottieAnimation(lottieView, R.raw.ain) // Change animation to `ain` and loop by default

        GreetUsers()

        recyclerView = mainView.findViewById(R.id.recyclerView)
        recyclerView.visibility = View.GONE
        sharedPreferences = getSharedPreferences("PredictionSettingsPrefs", Context.MODE_PRIVATE)

        primaryKeyboard = (layoutInflater.inflate(
            R.layout.keyboard_view_primary_english, null
        ) as ViewGroup).findViewById(R.id.keyboard)
        (primaryKeyboard.parent as ViewGroup).removeView(primaryKeyboard)
        symbolsKeyboard = (layoutInflater.inflate(
            R.layout.keyboard_view_symbols_english, null
        ) as ViewGroup).findViewById(R.id.keyboard)
        (symbolsKeyboard.parent as ViewGroup).removeView(symbolsKeyboard)
        voicegen = (layoutInflater.inflate(
            R.layout.voice_gen, null
        ) as ViewGroup).findViewById(R.id.keyboard)
        (voicegen.parent as ViewGroup).removeView(voicegen)
        emojiview = (layoutInflater.inflate(
            R.layout.emoji_picker_view, null
        ) as ViewGroup).findViewById(R.id.keyboard)
        (emojiview.parent as ViewGroup).removeView(emojiview)

        loadPredictionSettingsforrecycleview(Nodata,recyclerView)
        modifySystemNavBar()

       clipboardManager.addPrimaryClipChangedListener {
           updateClipboardContent(clipboardManager, clipboardCard, clipboardText)
       }

        // Listen for clipboard changes
    //  clipboardManager.addPrimaryClipChangedListener {
    //      if (isEmojiMode) {
    //          // Skip clipboard update if emoji picker is rendering
    //          return@addPrimaryClipChangedListener
    //      }

    //      val clip = clipboardManager.primaryClip
    //      val item = clip?.getItemAt(0)
    //      val text = item?.coerceToText(applicationContext)

    //      // If clipboard content has changed, update clipboard bubble and content
    //      if (text != clipboardText) {
    //          updateClipboardContent(clipboardManager, clipboardCard, clipboardText)
    //      }
    //  }

    //  clipboardButton.setOnClickListener {
    //      pasteClipboardContentToSystemField(clipboardManager,clipboardCard)
    //  }

        startUpdateCheckLoop()

        tokenTextView = voicegen.findViewById(R.id.token_count)
        languageSpinner = voicegen.findViewById(R.id.languageSpinner)
        gridview = voicegen.findViewById(R.id.gridview)
        playButton = voicegen.findViewById(R.id.playLayout)
        playText = voicegen.findViewById(R.id.playText)
        lottieProgress = voicegen.findViewById(R.id.lottieProgress)
        downloadButton = voicegen.findViewById(R.id.downloadAnimation)
        sharebutton = voicegen.findViewById(R.id.shareAnimation)

        gridview.layoutManager = GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false)
        voiceactoradapter = voiceactorAdapter(emptyList()) { item ->
            selectedItem = item
        }
        gridview.adapter = voiceactoradapter

        // Spinner items with images
        val spinnerData = listOf(
            SpinnerItem(R.drawable.engflag, "English"),
            SpinnerItem(R.drawable.india, "Hindi"),
            SpinnerItem(R.drawable.bangladesh, "Bengali")
        )

        // Set up Spinner with custom adapter
        val spinnerAdapter = SpinnerAdapter(this, spinnerData)
        languageSpinner.adapter = spinnerAdapter


        // Corrected Spinner item selected listener
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguage = spinnerData[position].text
                updateRecyclerView(position)
                selectedItem = null // Reset selection
                Toast.makeText(this@KeyboardService, "Selected: $selectedLanguage", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLanguage = "" // Reset selected language
                selectedItem = null // Reset selection
            }
        }


        playButton.setOnClickListener {
            // 1. Validate Input Fields
            val allText = getAllText()
            if (allText.isNullOrEmpty() || selectedLanguage.isEmpty() || selectedItem == null) {

                val secondView = suggestions.elementAtOrNull(1) as TextView
                val greetView = suggestions.elementAtOrNull(2) as TextView
                // Safely launch a coroutine for suspend function
                CoroutineScope(Dispatchers.IO).launch {
                    displayAnimatedText(secondView, greetView, getString(R.string.Input_missing))
                }

                return@setOnClickListener
            }

            toggleProgressVisibility(true) // Show progress while processing

            val trimmedPayload = allText.trim() // Trim leading/trailing whitespace
            val characterCount = trimmedPayload.replace("\\s+".toRegex(), "").length // Count non-whitespace characters

            // Fetch credits directly and check
            fetchUserCredits { credits ->
                if (credits >= 0) { // Valid credits fetched
                    if (credits >= characterCount) {
                        // Proceed with API call or other logic
                        val payload = preparePayload(allText)
                        CoroutineScope(Dispatchers.Main).launch {
                            postRequestToMurf(payload, tokenTextView, allText) { result ->
                                when (result) {
                                    is ApiResult.Success -> {
                                        audioUrl = result.audioUrl
                                        // Play audio if successful
                                        playAudio(result.audioUrl)
                                        // Example usage after sharing or when you need to clean up
                                       // deleteAudioFile("downloaded_audio.mp3", this@KeyboardService)

                                        deleteAudioFile(this@KeyboardService) { success ->
                                            if (success) {
                                                Log.d("DeleteAudio", "File deleted successfully")
                                            } else {
                                                Log.e("DeleteAudio", "File deletion failed")
                                            }
                                        }

                                    }
                                    is ApiResult.Error -> {
                                        // Handle error
                                        toggleProgressVisibility(false) // Hide progress after result
                                    }
                                }
                            }
                        }
                    } else {
                        // If credits are insufficient, show a message or handle the error
                        toggleProgressVisibility(false) // Hide progress
                        CoroutineScope(Dispatchers.IO).launch {
                            val secondView = suggestions.elementAtOrNull(1) as TextView
                            val greetView = suggestions.elementAtOrNull(2) as TextView
                            displayAnimatedText(secondView, greetView, "Insufficient credits")
                        }
                    }
                } else {
                    // Handle error fetching credits
                    toggleProgressVisibility(false) // Hide progress
                    Log.e("CreditCheck", "Failed to fetch credits")
                }
            }
        }


      //  downloadButton.setOnClickListener {
      //      if (audioUrl != null) {
      //          downloadAudioFile(audioUrl!!, this) { downloadedFile ->
      //              Toast.makeText(this, "Audio downloaded successfully!", Toast.LENGTH_SHORT).show()
      //          }
      //      } else {
      //          Toast.makeText(this, "Audio URL is not available!", Toast.LENGTH_SHORT).show()
      //      }
      //  }

        downloadButton.setOnClickListener {
            if (!audioUrl.isNullOrEmpty()) { // Check if audioUrl is valid
                Toast.makeText(this, "Kindly wait as the download is in progress. You can check the system notification for updates.", Toast.LENGTH_LONG).show()
             //   deleteAudioFile("downloaded_audio.mp3", this@KeyboardService)
                downloadAudioFile(audioUrl!!, this) { fileUri ->
                    if (fileUri != null) {
                        // Notify the user about the successful download
                        Toast.makeText(this, "Audio downloaded to: $fileUri", Toast.LENGTH_LONG).show()

                        // Example: Play the file or share it
                     shareAudio() // Uncomment if sharing is needed
                    } else {
                        Toast.makeText(this, "Failed to download audio.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Audio URL is not available!", Toast.LENGTH_SHORT).show()
            }
        }


        sharebutton.setOnClickListener {

            // Inside your Service class
            val intent = Intent(this, ShareAudioActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent) // Start the ShareAudioActivity directly

        }

        val emojiPickerView = emojiview.findViewById<EmojiPickerView>(R.id.emoji_picker)
        emojiPickerView.setOnEmojiPickedListener { emoji ->
            currentInputConnection?.commitText(emoji.emoji, 1) // Insert emoji
        }



        //auth
        // Check if the user is signed in
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            Log.d("Animetone","Signed in")
            fetchUserCreditsAndUpdateUI(tokenTextView)

        }else{
            redirectToSignInActivity()
        }

        return mainView
    }
    // Inserts the selected emoji into the input field
    private fun commitText(text: String, length: Int) {
        currentInputConnection?.commitText(text, length)
    }

    private fun shareAudio() {
        val intent = Intent(this, ShareAudioActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Ensures the activity starts in a new task
        }
        startActivity(intent)
    }

  // private fun deleteAudioFile(fileName: String, context: Context): Boolean {
  //     return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
  //         // For Android 10 and above, use MediaStore
  //         val resolver = context.contentResolver
  //         val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

  //         val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
  //         val selectionArgs = arrayOf(fileName)

  //         try {
  //             val deletedRows = resolver.delete(uri, selection, selectionArgs)
  //             if (deletedRows > 0) {
  //                 Toast.makeText(context, "File deleted successfully.", Toast.LENGTH_SHORT).show()
  //                 true
  //             } else {
  //                 Toast.makeText(context, "File not found in MediaStore.", Toast.LENGTH_SHORT).show()
  //                 false
  //             }
  //         } catch (e: Exception) {
  //             Toast.makeText(context, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
  //             false
  //         }
  //     } else {
  //         // For Android 9 and below, use the File API
  //         val destinationDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) // Scoped location for the app
  //             ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) // Legacy storage
  //         val destinationFile = File(destinationDir, fileName)

  //         if (destinationFile.exists()) {
  //             if (destinationFile.delete()) {
  //                 Toast.makeText(context, "File deleted successfully.", Toast.LENGTH_SHORT).show()
  //                 true
  //             } else {
  //                 Toast.makeText(context, "Failed to delete file.", Toast.LENGTH_SHORT).show()
  //                 false
  //             }
  //         } else {
  //             Toast.makeText(context, "File not found.", Toast.LENGTH_SHORT).show()
  //             false
  //         }
  //     }
  // }

    private fun deleteAudioFile(context: Context, onComplete: (Boolean) -> Unit) {
        val fileName = "downloaded_audio.mp3"
        val file: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, fileName) }
        } else {
            @Suppress("DEPRECATION")
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, fileName) }
        }

        // ✅ 1️⃣ First, try to delete file directly
        if (file != null && file.exists()) {
            if (file.delete()) {
                Toast.makeText(context, "File deleted successfully.", Toast.LENGTH_SHORT).show()
                onComplete(true)
                return
            } else {
                Toast.makeText(context, "Failed to delete file.", Toast.LENGTH_SHORT).show()
                onComplete(false)
                return
            }
        }

        // ✅ 2️⃣ If file path deletion fails, try MediaStore (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            try {
                val deletedRows = resolver.delete(uri, selection, selectionArgs)
                if (deletedRows > 0) {
                    Toast.makeText(context, "File deleted successfully from MediaStore.", Toast.LENGTH_SHORT).show()
                    onComplete(true)
                } else {
                    Toast.makeText(context, "File not found in MediaStore.", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error deleting file: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        } else {
            Toast.makeText(context, "File not found.", Toast.LENGTH_SHORT).show()
            onComplete(false)
        }
    }


    // private fun downloadAudioFile(audioUrl: String, context: Context, onComplete: (Uri?) -> Unit) {
 //     val fileName = "downloaded_audio.mp3"
//
 //     // Use the appropriate destination path for downloads
 //     val downloadDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
 //         // Scoped storage for Android 10 and above
 //         context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
 //     } else {
 //         // Legacy storage for older Android versions
 //         Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
 //     }
//
 //     val filePath = File(downloadDir, fileName).absolutePath
//
 //     val request = DownloadManager.Request(Uri.parse(audioUrl)).apply {
 //         setTitle("Downloading Audio")
 //         setDescription("Audio is being downloaded...")
 //         setDestinationUri(Uri.fromFile(File(filePath))) // Set the download destination
 //         setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
 //     }
//
 //     val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
 //     val downloadId = downloadManager.enqueue(request)
//
 //     // Register a BroadcastReceiver for download completion
 //     val onDownloadComplete = object : BroadcastReceiver() {
 //         override fun onReceive(context: Context, intent: Intent) {
 //             val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
 //             if (id == downloadId) {
 //                 // Query the DownloadManager for details
 //                 val query = DownloadManager.Query().setFilterById(downloadId)
 //                 val cursor = downloadManager.query(query)
//
 //                 if (cursor != null && cursor.moveToFirst()) {
 //                     val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
 //                     val fileUri = cursor.getString(uriIndex)?.let { Uri.parse(it) }
//
 //                     cursor.close()
 //                     onComplete(fileUri) // Pass the file URI to the callback
 //                 } else {
 //                     onComplete(null) // Failed to retrieve URI
 //                     Toast.makeText(context, "Failed to retrieve download URI.", Toast.LENGTH_SHORT).show()
 //                 }
//
 //                 // Unregister the receiver after completion
 //                 context.unregisterReceiver(this)
 //             }
 //         }
 //     }
//
 //     // Register the receiver with an exported flag for scoped storage compatibility
 //     context.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
 //         RECEIVER_NOT_EXPORTED
 //     )
 // }

    private fun downloadAudioFile(audioUrl: String, context: Context, onComplete: (Uri?) -> Unit) {
        val fileName = "downloaded_audio.mp3"
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(audioUrl)).apply {
            setTitle("Downloading Audio")
            setDescription("Downloading file...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // ✅ Save in correct directory for all versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            } else {
                @Suppress("DEPRECATION")
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            }
        }

        val downloadId = downloadManager.enqueue(request)

        // ✅ Poll for download status instead of using a BroadcastReceiver
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            onComplete(Uri.parse(uriString))
                            return // ✅ Stop checking once download is complete
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            onComplete(null) // ❌ Download failed
                            return
                        }
                    }
                }
                handler.postDelayed(this, 1000) // ✅ Check every 1 second
            }
        }, 1000)
    }

    private fun preparePayload(allText: String): String {
        // Directly access selectedItem assuming it's not null
        val voiceId = "${getLanguageCode(selectedLanguage)}-${selectedItem!!.text.lowercase()}"

        return JSONObject().apply {
            put("voiceId", voiceId)
            put("style", "Conversational")
            put("text", allText)
            put("rate", 0)
            put("pitch", 0)
            put("sampleRate", 48000)
            put("format", "MP3")
            put("channelType", "MONO")
            put("pronunciationDictionary", JSONObject())  // Empty dictionary
            put("encodeAsBase64", false)
            put("variation", 1)
            put("audioDuration", 0)
            put("modelVersion", "GEN2")
        }.toString()
    }


    // Function to post request (suspend function)
    private suspend fun postRequestToMurf(payload: String, creditsTextview: TextView, textfromkeyboard: String, onResult: (ApiResult) -> Unit) {
        try {
            val client = HttpClient()

            val response: HttpResponse = client.post(murfapiurl) {
                header("Content-Type", "application/json")
                header("Accept", "application/json")
                header("api-key", murfapikey)  // Use the global API key here
                setBody(payload)
            }

            val responseBody = response.bodyAsText()

         //   Toast.makeText(this@KeyboardService, responseBody, Toast.LENGTH_LONG).show()
            if (response.status.isSuccess()) {
                // On success, extract the audio URL from the response
                val jsonResponse = JSONObject(responseBody)
                val audioUrl = jsonResponse.getString("audioFile")
                //need to deduct credits based on payload
                deductCreditsBasedOnCharacterstransaction(textfromkeyboard,creditsTextview)
              //  deductCreditsBasedOnCharacters(textfromkeyboard,creditsTextview)
                onResult(ApiResult.Success(audioUrl))  // Success callback
            } else {
                onResult(ApiResult.Error("API request failed: ${response.status.value}"))  // Failure callback
            }
        } catch (e: Exception) {
            onResult(ApiResult.Error("Error occurred: ${e.message}"))  // Failure callback
        }
    }

    // Sealed class to represent the result of the API request
    sealed class ApiResult {
        data class Success(val audioUrl: String) : ApiResult()
        data class Error(val errorMessage: String) : ApiResult()
    }

    private fun playAudio(audioUrl: String) {
        val mediaPlayer = MediaPlayer()
        val handler = Handler(Looper.getMainLooper())
        var preparationTimedOut = false

        // Helper function to release the MediaPlayer safely
        fun releaseMediaPlayer() {
            try {
                mediaPlayer.release()
            } catch (e: IllegalStateException) {
                Log.e("AudioPlayback", "Error releasing MediaPlayer", e)
            }
        }

        try {
            mediaPlayer.apply {
                setDataSource(audioUrl)
                prepareAsync()

                // Timeout for preparation (10 seconds)
                handler.postDelayed({
                    if (!preparationTimedOut) {
                        preparationTimedOut = true
                        releaseMediaPlayer()
                        toggleProgressVisibility(false)
                        Log.e("AudioPlayback", "MediaPlayer preparation timed out.")
                    }
                }, 10_000L)

                // Start playback when prepared
                setOnPreparedListener {
                    if (!preparationTimedOut) {
                        start()
                        toggleProgressVisibility(false)
                    }
                }

                // Release resources after playback completes
                setOnCompletionListener {
                    releaseMediaPlayer()
                    toggleProgressVisibility(false)
                }

                // Handle playback errors
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayback", "Playback error: what=$what, extra=$extra")
                    toggleProgressVisibility(false)
                    releaseMediaPlayer()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayback", "Error initializing MediaPlayer", e)
            toggleProgressVisibility(false)
            releaseMediaPlayer()
        }
    }



    private fun toggleProgressVisibility(isLoading: Boolean) {
        if (isLoading) {
            playText.visibility = View.GONE
            lottieProgress.visibility = View.VISIBLE
            lottieProgress.playAnimation()
            playButton.isClickable = false
        } else {
            lottieProgress.cancelAnimation()
            lottieProgress.visibility = View.GONE
            playText.visibility = View.VISIBLE
            playButton.isClickable = true
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language) {
            "English" -> "en-UK" //error here should be uk
            "Hindi" -> "hi-IN"
            "Bengali" -> "bn-IN"
            else -> "en-UK"
        }
    }

    private fun updateRecyclerView(position: Int) {
        val data = when (position) {
            0 -> englishData
            1 -> hindiData
            else -> bengaliData
        }
        voiceactoradapter.updateData(data)
    }
    private fun fetchUserCreditsAndUpdateUI(creditsTextView: TextView) {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)

            // Fetch credits and update TextView
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val credits = document.getLong("credits")?.toInt() ?: 0
                        creditsTextView.text = "$credits"
                    } else {
                        creditsTextView.text = "0"
                    }
                }
                .addOnFailureListener { e ->
                    creditsTextView.text = ""
                    Log.e("FirebaseCredits", "Error fetching credits", e)
                }
        } else {
            creditsTextView.text = ""
        }
    }

    private fun fetchUserCredits(onCreditsFetched: (Int) -> Unit) {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)

            // Fetch credits and pass them to the callback
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val credits = document.getLong("credits")?.toInt() ?: 0
                        onCreditsFetched(credits)
                    } else {
                        onCreditsFetched(0) // Default to 0 if no credits found
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseCredits", "Error fetching credits", e)
                    onCreditsFetched(-1) // Return -1 in case of an error
                }
        } else {
            onCreditsFetched(-1) // Return -1 if the user is not logged in
        }
    }

    private fun deductCreditsBasedOnCharacters(payload: String, creditsTextView: TextView) {
        // Remove leading/trailing spaces and calculate the length of visible characters
        val trimmedPayload = payload.trim()
        val characterCount = trimmedPayload.replace("\\s+".toRegex(), "").length // Remove all whitespace before counting
        val creditsToDeduct = characterCount // Deduct 1 credit per visible character

        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)

            // Fetch the current credits
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val currentCredits = document.getLong("credits")?.toInt() ?: 0
                        val updatedCredits = if (currentCredits >= creditsToDeduct) {
                            currentCredits - creditsToDeduct
                        } else {
                            0 // If credits are less than what needs to be deducted, set to 0
                        }

                        // Update the credits in Firestore
                        userRef.update("credits", updatedCredits)
                            .addOnSuccessListener {
                                // Update UI with the new credits value
                                creditsTextView.text = "$updatedCredits"
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirebaseCredits", "Error updating credits", e)
                            }
                    } else {
                        creditsTextView.text = "0"
                    }
                }
                .addOnFailureListener { e ->
                    creditsTextView.text = ""
                    Log.e("FirebaseCredits", "Error fetching credits", e)
                }
        } else {
            creditsTextView.text = ""
        }
    }
//transaction method
    private fun deductCreditsBasedOnCharacterstransaction(payload: String, creditsTextView: TextView) {
        val trimmedPayload = payload.trim()
        val characterCount = trimmedPayload.replace("\\s+".toRegex(), "").length
        val creditsToDeduct = characterCount

        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            val userRef = db.collection("users").document(currentUser.uid)

            // Use Firestore Transaction to prevent race conditions
            db.runTransaction { transaction ->
                val document = transaction.get(userRef)
                if (!document.exists()) {
                    throw Exception("User document does not exist")
                }

                val currentCredits = document.getLong("credits")?.toInt() ?: 0
                if (currentCredits < creditsToDeduct) {
                    throw Exception("Not enough credits") // Prevents negative balance
                }

                val updatedCredits = currentCredits - creditsToDeduct
                transaction.update(userRef, "credits", updatedCredits)
                updatedCredits // Return new credit balance
            }.addOnSuccessListener { updatedCredits ->
                creditsTextView.text = "$updatedCredits"
            }.addOnFailureListener { e ->
                if (e.message == "Not enough credits") {
                    creditsTextView.text = "Not enough credits!"
                } else {
                    Log.e("FirebaseCredits", "Error updating credits", e)
                }
            }
        } else {
            creditsTextView.text = ""
        }
    }


    private fun redirectToSignInActivity() {
        // Start the SignInActivity if the user is not signed in
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK // Set the flag to start a new task
        startActivity(intent)
    }
    private fun startUpdateCheckLoop() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isUpdateCheckRunning) {
                // Check update status
                if (UpdateStatusManager.isUpdateFound()) {
                 //   println("Update is available!")
                    withContext(Dispatchers.Main) {
                        // You can show a UI notification or dialog here if needed
                        Update_container.visibility = View.VISIBLE
                    }
                    break // Exit the loop if the update is found
                } else {
                   // println("No updates found.")
                }
                delay(300_000) // Wait for 5 minutes
            }
        }
    }

    private fun modifySystemNavBar() {
        val window = window?.window ?: return // Attempt to access the Window object (not always available)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = resources.getColor(android.R.color.white, theme) // Change to white
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR // Ensure icons are black
        }
    }

    private fun pasteClipboardContentToSystemField(  clipboardManager: ClipboardManager,
                                                     clipboardCard: LinearLayout) {
        val clipboardContent = getClipboardContent(clipboardManager)
        currentInputConnection.commitText(clipboardContent, 1) // This commits the new text
        clipboardCard.visibility = View.GONE
    }


    /**
     * Updates the clipboard content in the TextView and controls visibility of the clipboard card.
     */
    private fun updateClipboardContent(
        clipboardManager: ClipboardManager,
        clipboardCard: LinearLayout,
        clipboardTextView: TextView
    ) {
        val clipboardContent = getClipboardContent(clipboardManager)

        if (clipboardContent.isNotEmpty()) {
            clipboardCard.visibility = View.VISIBLE
            clipboardTextView.text = clipboardContent
        } else {
            clipboardCard.visibility = View.GONE
        }
    }

    /**
     * Retrieves the current content of the clipboard.
     */
    private fun getClipboardContent(clipboardManager: ClipboardManager): String {
        val primaryClip: ClipData? = clipboardManager.primaryClip
        return if (primaryClip != null && primaryClip.itemCount > 0) {
            primaryClip.getItemAt(0).text.toString()
        } else {
            "" // Return an empty string if clipboard is empty
        }
    }

    private fun GreetUsers(){

        val greetings = listOf(
            "Hi, I am your AI assistant! How can I help you today?",
            "Hello! I am your AI assistant. What can I do for you?",
            "Good day! I'm here to assist you. How may I help?",
            "Hey there! I'm your AI assistant. Ready to assist you!"
        )
        //need to work on it after api call
        val greetingMessage = greetings.random()

        val AiGreetingview = suggestions.elementAtOrNull(2) as TextView
        AiGreetingview.text = greetingMessage;
        AiGreetingview.visibility = View.VISIBLE

    }

    // Method to set up a Lottie animation
    fun setupLottieAnimation(lottieView: LottieAnimationView, animationRes: Int, loop: Boolean = true) {
        lottieView.setAnimation(animationRes) // Set the animation resource
        lottieView.loop(loop)                // Set looping
        lottieView.playAnimation()           // Start playing the animation
    }

    private fun loadPredictionSettingsforrecycleview(Nodata: LinearLayout, datarecycleview: RecyclerView) {
        try {
            // Retrieve JSON string from SharedPreferences
            val json = sharedPreferences.getString(prefsKey, null)

            if (!json.isNullOrEmpty()) {
                // Parse JSON into a list of maps
                val type = object : TypeToken<ArrayList<Map<String, String>>>() {}.type
                val predictionSettings: ArrayList<Map<String, String>> = Gson().fromJson(json, type)

                if (predictionSettings.isNotEmpty()) {
                    // Hide the "No Data" TextView and show RecyclerView
                    Nodata.visibility = View.GONE
                    datarecycleview.visibility = View.VISIBLE

                    // Extract the "text" values for the RecyclerView
                    val textList = predictionSettings.mapNotNull { it["text"] }

                    // Set up the RecyclerView and Adapter
                    predictionAdapter = PredictionAdapter(textList) { selectedItem ->
                        item = selectedItem // Assign the selected item to the global variable
                    }

                    val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    datarecycleview.layoutManager = layoutManager
                    datarecycleview.adapter = predictionAdapter

                } else {
                    // Show "No Data" TextView and hide RecyclerView
                    datarecycleview.visibility = View.GONE
                    Nodata.visibility = View.VISIBLE
                    Log.d("LoadPredictionSettings", "JSON is an empty array")
                }
            } else {
                // Show "No Data" TextView and hide RecyclerView
                datarecycleview.visibility = View.GONE
                Nodata.visibility = View.VISIBLE
                Log.d("LoadPredictionSettings", "No JSON found in SharedPreferences")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Function to trigger vibration
    private fun vibrate() {
        val vibrator = mainView.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) { // Check if the device has a vibrator
            val vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE) // 50ms vibration
            vibrator.vibrate(vibrationEffect)
        }
    }
    /**
     * Called by the caps button.
     */
    private fun toggleCaps(v: View) {
        vibrate()
        capsOn = !capsOn
        for (row in mainView.findViewById<LinearLayout>(R.id.keyboard).children) {
            for (button in (row as LinearLayout).children) {
                if (button is Button && button.text.length == 1) {
                    if (capsOn) {
                       // vibrate()
                        button.text = button.text.toString().uppercase()
                    } else {
                      //  vibrate()
                        button.text = button.text.toString().lowercase()
                    }
                }
            }
        }
    }

    override fun onClick(v: View) {
        when (v.tag) {
            "DEL", "ENTER", "SPACE", "TAB" -> handleSpecialKey(v)
            "SYMBOLS" -> switchToSymbols(mainView as ViewGroup)
            "PRIMARY" -> switchToPrimary(mainView as ViewGroup)
            "voice" -> switchTovoice(mainView as ViewGroup)
            "EMOJI" ->switchToemoji(mainView as ViewGroup)
            "CAPS_LOCK" -> toggleCaps(v)
            "AI_CALL" -> aiCall(v)
            else -> handleRegularKey(v as TextView)
        }
    }

    private fun handleSpecialKey(v: View) {
        vibrate()
        when (v.tag) {
            "DEL" -> {
                v.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isKeyHeld = true
                            sendKeyEvent(KeyEvent.KEYCODE_DEL) // Send initial delete event
                            vibrate()
                            handler.postDelayed(deleteKeyRunnable, 400) // Start repeating after a delay
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isKeyHeld = false
                            handler.removeCallbacks(deleteKeyRunnable) // Stop repeating
                        }
                    }
                    true
                }
            }
            "ENTER" -> sendKeyEvent(KeyEvent.KEYCODE_ENTER) // ENTER key
            "SPACE" -> sendKeyEvent(KeyEvent.KEYCODE_SPACE) // SPACE key
            "TAB" -> sendKeyEvent(KeyEvent.KEYCODE_TAB) // TAB key
        }
    }

    private var isKeyHeld = false
    private val handler = Handler(Looper.getMainLooper())
    private val deleteKeyRunnable = object : Runnable {
        override fun run() {
            if (isKeyHeld) {
                sendKeyEvent(KeyEvent.KEYCODE_DEL)
                vibrate()
                handler.postDelayed(this, 50) // Repeat every 50ms
            }
        }
    }

    private fun sendKeyEvent(keyCode: Int) {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode) // Key Down event
        currentInputConnection.sendKeyEvent(event) // Send the event
        val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode) // Key Up event
        currentInputConnection.sendKeyEvent(upEvent) // Send the up event
    }

    private fun handleRegularKey(v: TextView) {
        vibrate()
        currentInputConnection.commitText(v.text, 1)
    }

    private fun switchToSymbols(keyboardRoot: ViewGroup) {
        vibrate()
        keyboardRoot.removeView(mainView.findViewById(R.id.keyboard))
        keyboardRoot.addView(symbolsKeyboard)
    }

    private fun switchToemoji(keyboardRoot: ViewGroup) {
        vibrate() // Provide haptic feedback

        val currentActivity = activityTracker.getCurrentActivity()

        // You can remove this condition if you want the toggle to happen regardless of the activity.
        if (currentActivity != null && currentActivity.javaClass.name == "net.animetone.predictionSettings.PredictionSettingsListActivity") {
            // Here, you can optionally add specific logic if the activity is PredictionSettingsListActivity
            // If you don't want to change anything when in this activity, you can simply return early.
            return
        }

        // Switch views between emoji and keyboard based on the current mode
        if (isEmojiMode) {
            // Switch back to keyboard
            keyboardRoot.removeView(mainView.findViewById(R.id.keyboard))
            keyboardRoot.addView(primaryKeyboard)
        } else {
            // Switch to emoji view
            keyboardRoot.removeView(mainView.findViewById(R.id.keyboard))
            keyboardRoot.addView(emojiview)
        }

        // Toggle the emoji mode
        isEmojiMode = !isEmojiMode
    }




    private fun switchTovoice(keyboardRoot: ViewGroup) {
        vibrate() // Provide haptic feedback

        val voiceButton: ImageButton = mainView.findViewById(R.id.voice)

        if (isVoiceMode) {
            // Switch back to keyboard
            keyboardRoot.removeView(mainView.findViewById(R.id.keyboard))
            keyboardRoot.addView(primaryKeyboard)
            voiceButton.setImageResource(R.drawable.baseline_mic_24) // Set voice mode icon
        } else {
            isEmojiMode = false
            // Switch to voice generation
            keyboardRoot.removeView(mainView.findViewById(R.id.keyboard))
            keyboardRoot.addView(voicegen)
            voiceButton.setImageResource(R.drawable.baseline_keyboard_24) // Set keyboard mode icon
        }

        // Toggle state
        isVoiceMode = !isVoiceMode
    }

    private fun switchToPrimary(keyboardRoot: ViewGroup) {
        vibrate()
        isEmojiMode = false
        keyboardRoot.removeView(mainView.findViewById(R.id.keyboard))
        keyboardRoot.addView(primaryKeyboard)
    }


    private suspend fun updateSuggestion() {
        // Get selected user input and text
        val userDefinition = item //spinner.selectedItem.toString()
        val allText = getAllText()

        if (allText.isNullOrEmpty() || userDefinition.isNullOrEmpty()) {
            withContext(Dispatchers.Main) {
                val secondView = suggestions.elementAtOrNull(1) as TextView
                val greetView = suggestions.elementAtOrNull(2) as TextView
                displayAnimatedText(secondView,greetView, getString(R.string.no_input_message))

            }
            return
        }

        // Prepare the JSON body manually
        val requestBody = """
        {
          "model": "llama3-8b-8192",
          "messages": [
            {
              "role": "user",
              "content": "$userDefinition, given the following text: $allText. Only provide the transformed content without any additional explanation, and ensure there are no double quotes around the result."
            }
          ]
        }
    """.trimIndent()

        val client = HttpClient()

        try {
            // Make the API request
            val response: HttpResponse = client.post(groqapiurl) {
                header("Authorization", groqapikey) // API key
                header("Content-Type", "application/json")
                setBody(requestBody)
            }

            // Parse the response as plain text
            val responseBody = response.bodyAsText()

            // Parse and handle the response safely
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val choicesJsonArray = jsonResponse.getAsJsonArray("choices")

            if (choicesJsonArray != null && choicesJsonArray.size() > 0) {
                val content = choicesJsonArray[0].asJsonObject
                    .getAsJsonObject("message")
                    .get("content")
                    .asString
                    .trim()


                withContext(Dispatchers.Main) {
                    // Safely retrieve the second view as TextView
                    val view = suggestions.elementAtOrNull(1) as TextView // Safely cast to TextView
                    val greetview = suggestions.elementAtOrNull(2) as TextView // Safely cast to TextView
                    view.visibility = View.VISIBLE
                    greetview.visibility = View.GONE
                    animateTextView(view, content) // Animate the text
                    view.setOnClickListener {
                        currentInputConnection.deleteSurroundingText(allText.length, 0)
                        // Clear existing text (if needed) and insert new content
                        currentInputConnection.setComposingText("", 1) // This clears the current composing text
                        currentInputConnection.commitText(content, 1) // This commits the new text

                        view.visibility = View.GONE // Hide the TextView after click
                        greetview.visibility = View.VISIBLE
                        AiCallButton.isClickable = true
                        val lottieView = suggestions.elementAtOrNull(0) as LottieAnimationView
                        setupLottieAnimation(lottieView, R.raw.ain) // Change animation to `ain` and loop by default
                    }
                }
            }
        } catch (e: Exception) {
           e.printStackTrace();
            AiCallButton.isClickable = true
            val lottieView = suggestions.elementAtOrNull(0) as LottieAnimationView
            setupLottieAnimation(lottieView, R.raw.ain) // Change animation to `ain` and loop by default
        } finally {
            client.close()
            AiCallButton.isClickable = true
            val lottieView = suggestions.elementAtOrNull(0) as LottieAnimationView
            setupLottieAnimation(lottieView, R.raw.ain) // Change animation to `ain` and loop by default
        }
    }

    private suspend fun animateTextView(textView: TextView, text: String, delay: Long = 50) {
        // Clear the current text
        textView.text = ""
        AiCallButton.isClickable = false
        playButton.isClickable = false
        // Reveal the text character by character
        for (i in text.indices) {
            textView.text = text.substring(0, i + 1)
            delay(delay) // Pause to create the typing effect
        }
        AiCallButton.isClickable = true
        playButton.isClickable = true
        val lottieView = suggestions.elementAtOrNull(0) as LottieAnimationView
        setupLottieAnimation(lottieView, R.raw.ain) // Change animation to `ain` and loop by default
    }

    suspend fun displayAnimatedText(view: TextView, greetview:TextView, text: String, duration: Long = 2000) {
        withContext(Dispatchers.Main) {
            view.visibility = View.VISIBLE
            greetview.visibility = View.GONE
            animateTextView(view, text) // Reuse your existing animation function

            // Optional auto-hide logic
            view.animate()
                .alpha(1f)
                .setStartDelay(duration)
                .setDuration(500)
                .withEndAction {
                    view.visibility = View.GONE
                    greetview.visibility = View.VISIBLE
                    AiCallButton.isClickable = true
                    playButton.isClickable = true
                    val lottieView = suggestions.elementAtOrNull(0) as LottieAnimationView
                    setupLottieAnimation(lottieView, R.raw.ain) // Change animation to `ain` and loop by default
                }
                .start()
        }
    }


    private fun getAllText(): String {
        val allText = currentInputConnection.getExtractedText(ExtractedTextRequest(), 0)
        return allText?.text?.toString() ?: ""
    }

    private fun aiCall(view: View) {
      //  Toast.makeText(this,"API CALLED",Toast.LENGTH_LONG).show();
        vibrate()
        AiCallButton.isClickable = false
        val lottieView = suggestions.elementAtOrNull(0) as LottieAnimationView
        setupLottieAnimation(lottieView, R.raw.aiw) // Change animation to `ain` and loop by default
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                updateSuggestion()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isUpdateCheckRunning = false // Stop the loop when the input view is destroyed
    }
}
