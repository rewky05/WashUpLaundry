package com.example.washuplaundry

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Sales : Fragment() {

    private lateinit var spinner: Spinner
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var db: DatabaseReference
    private lateinit var listener: ValueEventListener
    private lateinit var totalPreview: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sales, container, false)

        spinner = view.findViewById(R.id.spinner)
        fragmentContainer = view.findViewById(R.id.fragment_container_dropdown)
        db = FirebaseDatabase.getInstance().getReference("Services")

        totalPreview = view.findViewById(R.id.btnView)
        totalPreview.setOnClickListener{
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TotalPreview())
                .addToBackStack(null)
                .commit()
        }

        fetchData()

        return view
    }

    private fun fetchData() {
        listener = db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val categories = snapshot.children.mapNotNull { it.key }
                    populateSpinner(categories)
                } else {
                    Log.e("Sales", "Failed to fetch data from Firebase")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Sales", "Failed to read from database", error.toException())
            }
        })
    }

    private fun populateSpinner(categories: List<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_layout,
            categories
        )
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_layout)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categories[position]
                val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()

                when (selectedCategory) {
                    "Regular" -> handleFragment("regular_service", { RegularService.newInstance() })
                    "Self Service" -> handleFragment("self_service", { SelfService() })
                    "Dry Clean" -> handleFragment("dry_clean", { DryCleanService() })
                    else -> { /* Handle other categories if needed */ }
                }

                fragmentTransaction.commit()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun handleFragment(tag: String, createFragment: () -> Fragment) {
        val fragment = childFragmentManager.findFragmentByTag(tag) ?: createFragment()

        childFragmentManager.beginTransaction()
            .apply {
                for (existingFragment in childFragmentManager.fragments) {
                    if (existingFragment.tag != tag) hide(existingFragment)
                }
                if (fragment.isAdded) {
                    show(fragment)
                } else {
                    add(R.id.fragment_container_dropdown, fragment, tag)
                }
            }
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        db.removeEventListener(listener)
    }
}
