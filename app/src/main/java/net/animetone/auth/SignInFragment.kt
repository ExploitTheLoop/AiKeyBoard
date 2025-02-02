package net.animetone.auth

import com.google.android.gms.common.api.ApiException
import net.animetone.R
import net.animetone.predictionSettings.PredictionSettingsListActivity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser
import com.google.android.gms.common.SignInButton
import com.google.android.gms.tasks.OnCompleteListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SignInFragment : Fragment() {

    private val RC_SIGN_IN = 9001
    private lateinit var mAuth: FirebaseAuth

  //  private lateinit var signInButton: SignInButton
  private lateinit var signInButton: LinearLayout // Correct type is LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_sign_in, container, false)
        // Hide the ActionBar when the fragment is visible
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
        setSystemBarColors()

        mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser


        signInButton = rootView.findViewById(R.id.custom_button)

        // Check if the user is already logged in
        if (currentUser != null) {
            // If logged in, redirect to HomeActivity
            redirectToHomeActivity()
        } else {
            // Show Google Sign-In button
            signInButton.setOnClickListener {
                signIn()
            }
        }

        return rootView
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



    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Add your client ID here
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle the result of Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        mAuth.signInWithCredential(credential).addOnCompleteListener(requireActivity(), OnCompleteListener { task ->
            if (task.isSuccessful) {
                // Successfully signed in
                val user = mAuth.currentUser
                //grantFreeCredit(user)  // Grant credits to the user
                grantFreeCreditTrans(user)
                // Redirect to HomeActivity
                redirectToHomeActivity()
            } else {
                Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
            }
        })
    }

    private fun grantFreeCredit(user: FirebaseUser?) {
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(user.uid)

            // Check if user exists
            userRef.get().addOnSuccessListener { document ->
                if (!document.exists()) {
                    // New user, grant initial credit
                    val initialCredits = hashMapOf(
                        "email" to user.email,
                        "credits" to 100 // 100 free credits for new users
                    )
                    userRef.set(initialCredits).addOnSuccessListener {
                        Log.d("GrantCredit", "Initial credits granted to ${user.email}")
                    }.addOnFailureListener { e ->
                        Log.e("GrantCredit", "Error granting credits", e)
                    }
                } else {
                    Log.d("GrantCredit", "User already exists. No credits granted.")
                }
            }.addOnFailureListener { e ->
                Log.e("GrantCredit", "Error fetching user document", e)
            }
        }
    }

    private fun grantFreeCreditTrans(user: FirebaseUser?) {
        if (user == null) return

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(user.uid)

        db.runTransaction { transaction ->
            val document = transaction.get(userRef)

            if (!document.exists()) {
                // New user, grant initial credit
                val initialCredits = hashMapOf(
                    "email" to user.email,
                    "credits" to 300 // Grant 100 free credits only once
                )
                transaction.set(userRef, initialCredits)
            }
        }.addOnSuccessListener {
            Log.d("GrantCredit", "Initial credits granted to ${user.email}")
        }.addOnFailureListener { e ->
            Log.e("GrantCredit", "Error granting credits", e)
        }
    }
    private fun redirectToHomeActivity() {
        // Start the HomeActivity
        val intent = Intent(requireContext(), PredictionSettingsListActivity::class.java)
        startActivity(intent)

        // Finish the current activity or fragment, so the user can't come back to the sign-in screen
        requireActivity().finish()
    }
}
