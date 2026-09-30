package com.example.drugdetector

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.example.drugdetector.databinding.ActivityAuthBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var credentialManager: CredentialManager
    private lateinit var sessionManager: SessionManager

    // Using your OAuth Web Client ID for Credential Manager token requests
    private val webClientId = "167711468246-7k1gntudtj52mu0d8u7b2042c6kq8f49.apps.googleusercontent.com"
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)
        sessionManager = SessionManager(this)

        setupListeners()
    }

    private fun setupListeners() {
        // Toggle Between Login and Sign Up
        binding.tvToggleMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            if (isSignUpMode) {
                binding.tvTitle.text = "Create Account"
                binding.btnSubmit.text = "Sign Up"
                binding.tvToggleMode.text = "Already have an account? Log In"
            } else {
                binding.tvTitle.text = "Operator Login"
                binding.btnSubmit.text = "Sign In"
                binding.tvToggleMode.text = "Don't have an account? Sign Up"
            }
        }

        // Email & Password Submit
        binding.btnSubmit.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sessionManager.saveUser(email)
            navigateToMain()
        }

        // Google Authentication
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@AuthActivity
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    val idToken = googleIdTokenCredential.idToken

                    sessionManager.saveUser(email, idToken)
                    navigateToMain()
                }
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                Toast.makeText(this@AuthActivity, "Google Sign-In cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}