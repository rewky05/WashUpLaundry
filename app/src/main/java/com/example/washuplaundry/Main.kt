package com.example.washuplaundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class Main : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bottomNavigation: BottomNavigationView = view.findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_services -> {
                    replaceFragment(Services())
                    true
                }

                R.id.navigation_receipts -> {
                    replaceFragment(Receipts())
                    true
                }

                R.id.navigation_sales -> {
                    replaceFragment(Sales())
                    true
                }

                R.id.navigation_settings -> {
                    replaceFragment(Settings())
                    true
                }

                R.id.navigation_office -> {
                    replaceFragment(BackOffice())
                    true
                }

                else -> false
            }
        }

        // Show the HomeFragment by default
        bottomNavigation.selectedItemId = R.id.navigation_sales
    }

    private fun replaceFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
