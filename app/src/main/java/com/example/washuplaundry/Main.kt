package com.example.washuplaundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class Main : Fragment() {
    private var currentFragmentTag: String? = null
    private lateinit var orderViewModel: OrderViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderViewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]

        val bottomNavigation: BottomNavigationView = view.findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
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

                else -> false
            }
        }

        bottomNavigation.selectedItemId = R.id.navigation_sales

        requireActivity().findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            if (currentFragmentTag != null) {
                val handled = parentFragmentManager.popBackStackImmediate(currentFragmentTag, 0)
                if (!handled) {
                    parentFragmentManager.popBackStack()
                }
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        requireActivity().findViewById<MaterialButton>(R.id.btnClearPreview).setOnClickListener {
            orderViewModel.resetTotalPrice()
        }

    }

    private fun replaceFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
