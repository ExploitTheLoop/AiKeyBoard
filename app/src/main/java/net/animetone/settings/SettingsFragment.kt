package net.animetone.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import net.animetone.databinding.FragmentSettingsBinding
import android.provider.Settings
import android.os.Build
import androidx.core.content.ContextCompat
import net.animetone.MainActivity
import net.animetone.R
import net.animetone.predictionSettings.PredictionSettingsListActivity

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the ActionBar when the fragment is visible
        (activity as? AppCompatActivity)?.supportActionBar?.hide()

        // Set system bar colors
        setSystemBarColors()

        // Disable the back button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing to completely block the back press
                }
            }
        )

        // Set click listeners for buttons
        binding.enableKeyboardButton.setOnClickListener {
            switchToAnotherKeyboard()
        }

        binding.switchKeyboardButton.setOnClickListener {
            showCustomKeyboard()
        }

        binding.closeButton.setOnClickListener {
            val intent = Intent(requireContext(), PredictionSettingsListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setSystemBarColors() {
        activity?.window?.apply {
            // Set navigation bar color
            navigationBarColor = ContextCompat.getColor(requireContext(), R.color.your_navigation_bar_color)

            // Set status bar color
            statusBarColor = ContextCompat.getColor(requireContext(), R.color.your_status_bar_color)

            // Ensure status bar icons are white
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = 0 // Clear all flags related to system UI visibility
            }
        }
    }

    private fun showCustomKeyboard() {
        val inputMethodManager =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showInputMethodPicker()
    }

    private fun switchToAnotherKeyboard() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore the ActionBar when the fragment is destroyed
        (activity as? AppCompatActivity)?.supportActionBar?.show()
        _binding = null
    }
}
