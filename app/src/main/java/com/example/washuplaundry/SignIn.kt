package com.example.washuplaundry

import android.os.Bundle
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class SignIn : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var rememberMe: CheckBox
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("loggedCredentials", Context.MODE_PRIVATE)

        val signInButton = view.findViewById<Button>(R.id.btnSignIn)
        emailInput = view.findViewById(R.id.emailAddress)
        passwordInput = view.findViewById(R.id.password)
        rememberMe = view.findViewById(R.id.remember_me)

        if(sharedPreferences.getBoolean("rememberMe", false)) {
            emailInput.setText(sharedPreferences.getString("email", ""))
            passwordInput.setText(sharedPreferences.getString("password", ""))
            rememberMe.isChecked=true
        }

        signInButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if(rememberMe.isChecked) {
                                saveCredentials(email, password)
                            }
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragmentContainer, Main())
                                .addToBackStack(null)
                                .commit()
                        } else {
                            val errorMessage = when (task.exception) {
                                is FirebaseAuthInvalidUserException -> "Invalid email address"
                                is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                                else -> "Sign in failed. ${task.exception?.message}"
                            }
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else {
                Toast.makeText(requireContext(), "Please enter your email and password first.", Toast.LENGTH_SHORT)
                    .show()
                }
            }
        }
    private fun saveCredentials(email:String, password:String) {
        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("password", password)
        editor.putBoolean("rememberMe", true)
        editor.apply()
    }
}
