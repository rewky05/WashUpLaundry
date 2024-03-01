package com.example.washuplaundry

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class Settings : Fragment() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences =
            requireActivity().getSharedPreferences("loggedCredentials", Context.MODE_PRIVATE)

        val btnSignOut = view.findViewById<Button>(R.id.btnSignOut)
        btnSignOut.setOnClickListener {
            signOut()
        }
    }

    private fun clearSharedPreferences() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }

    private fun signOut() {
        clearSharedPreferences()
        firebaseAuth.signOut()

        val bottomNavigationView = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE

        replaceFragment(SignIn())
    }

    private fun replaceFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
