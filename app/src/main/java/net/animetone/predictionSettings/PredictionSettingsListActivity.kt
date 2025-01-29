package net.animetone.predictionSettings

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.animetone.KeyboardGPTApp
import net.animetone.R
import net.animetone.databinding.PredictionSettingsListBinding
import net.animetone.utlis.SwipeToDeleteCallback
import android.view.View // Import this if View is red
import androidx.core.content.ContextCompat
import com.saadahmedev.popupdialog.PopupDialog
import net.animetone.MainActivity

class PredictionSettingsListActivity : AppCompatActivity() {

    private lateinit var binding: PredictionSettingsListBinding
    private lateinit var app: KeyboardGPTApp

    private lateinit var adapter: PredictionSettingsRecyclerViewAdapter
    private var currentPredictionSettings = ArrayList<PredictionSettingModel>()

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val prefsKey = "prediction_settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PredictionSettingsListBinding.inflate(layoutInflater)

        setContentView(binding.root)

        app = application as KeyboardGPTApp

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("PredictionSettingsPrefs", Context.MODE_PRIVATE)

        // Load data from SharedPreferences
        loadPredictionSettings()


        // Hide the ActionBar
        supportActionBar?.hide()

        // Set system bar colors
        setSystemBarColors()

        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
            reverseLayout = true
        }
        binding.settingsRecyclerView.layoutManager = layoutManager

        adapter = PredictionSettingsRecyclerViewAdapter(currentPredictionSettings)
        binding.settingsRecyclerView.adapter = adapter

        updateUIBasedOnData(currentPredictionSettings)

        binding.settingsButton.setOnClickListener {

            // Create an Intent to start MainActivity
            val intent = Intent(this, MainActivity::class.java)
            val comingFromSettings = true
            intent.putExtra("coming_from_settings", comingFromSettings)
            startActivity(intent)

        }

        binding.saveButton.setOnClickListener {
            savePredictionSettings()

            try {
                // Use `this` as the context since this is within an Activity
                PopupDialog.getInstance(this)
                    .statusDialogBuilder()
                    .createWarningDialog()
                    .setHeading("Pending")
                    .setDescription("A system restart is required\nto complete the update process.")
                    .build(Dialog::dismiss)
                    .show()
            } catch (e: Exception) {
                e.printStackTrace()
               // Toast.makeText(this, "Failed to display dialog: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }


        binding.floatingActionButton.setOnClickListener {
            val predictionSetting = PredictionSettingModel()
            currentPredictionSettings.add(predictionSetting)
            adapter.notifyItemInserted(adapter.itemCount - 1)
            updateUIBasedOnData(currentPredictionSettings)
        }

        binding.addprompt.setOnClickListener{

            val predictionSetting = PredictionSettingModel()
            currentPredictionSettings.add(predictionSetting)
            adapter.notifyItemInserted(adapter.itemCount - 1)
            updateUIBasedOnData(currentPredictionSettings)
        }
        
        val swipeDeleteHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                currentPredictionSettings.removeAt(viewHolder.adapterPosition)
                adapter.notifyItemRemoved(viewHolder.adapterPosition)
                savePredictionSettings() // Save updated list
                updateUIBasedOnData(currentPredictionSettings)
            }
        }
        val itemTouchDeleteHelper = ItemTouchHelper(swipeDeleteHandler)
        itemTouchDeleteHelper.attachToRecyclerView(binding.settingsRecyclerView)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_prediction_settings, menu)

        menu!!.findItem(R.id.save_button).setOnMenuItemClickListener {
            savePredictionSettings()
            finish()
            true
        }
        menu.findItem(R.id.cancel_button).setOnMenuItemClickListener {
            finish()
            true
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun setSystemBarColors() {
        window?.apply {
            // Set navigation bar color
           // navigationBarColor = ContextCompat.getColor(this@PredictionSettingsListActivity, R.color.your_navigation_bar_color)

            // Set status bar color
            statusBarColor = ContextCompat.getColor(this@PredictionSettingsListActivity, R.color.your_status_bar_color)

            // Ensure status bar icons are white
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = 0 // Clear all flags related to system UI visibility
            }

        }
    }


    private fun updateUIBasedOnData(currentPredictionSettings: List<Any>) {
        if (currentPredictionSettings.isEmpty()) {
            binding.noDataLayout.visibility = View.VISIBLE
            binding.settingsRecyclerView.visibility = View.GONE
            binding.saveButton.visibility = View.GONE
        } else {
            binding.noDataLayout.visibility = View.GONE
            binding.settingsRecyclerView.visibility = View.VISIBLE
            binding.saveButton.visibility = View.VISIBLE
        }
    }


    private fun loadPredictionSettings() {
        val json = sharedPreferences.getString(prefsKey, null)
        if (json != null) {
            val type = object : TypeToken<ArrayList<PredictionSettingModel>>() {}.type
            currentPredictionSettings = gson.fromJson(json, type)
        }
    }

    private fun savePredictionSettings() {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(currentPredictionSettings)
        editor.putString(prefsKey, json)
        editor.apply()
    }
}
