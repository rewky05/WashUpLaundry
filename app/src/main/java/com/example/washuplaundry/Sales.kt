package com.example.washuplaundry

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sales, container, false)

        spinner = view.findViewById(R.id.spinner)
        fragmentContainer = view.findViewById(R.id.fragment_container_dropdown)

        // Set up Firebase Database
        db = FirebaseDatabase.getInstance().getReference("Services")

        // Fetch data from Firebase
        fetchData()

        return view
    }

    private fun fetchData() {
        listener = db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val serviceCategories = mutableListOf<DataSnapshot>()

                // Clear previous data
                serviceCategories.clear()

                // Populate data from Firebase
                for (categorySnapshot in snapshot.children) {
                    serviceCategories.add(categorySnapshot)
                }

                // Set up spinner adapter
                val spinnerAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    serviceCategories.map { it.key }
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = spinnerAdapter

                // Set up Spinner functionality after populating data
                setupSpinner(serviceCategories)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Firebase", "Error fetching data: ${error.message}")
            }
        })
    }

    private fun setupSpinner(serviceCategories: List<DataSnapshot>) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Handle item selection
                replaceFragmentWithSubServices(serviceCategories[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun replaceFragmentWithSubServices(categorySnapshot: DataSnapshot) {
        when (categorySnapshot.key) {
            "Dry Clean" -> {
                fetchDryCleanData { dryClean ->
                    val fragment = DryClean(dryClean.map { it.service })
                    replaceFragment(fragment)
                }
            }
            "Regular" -> {
                fetchRegularServicesData {
//                    val fragment = RegularServices(regularServices.map { it.service })
                    replaceFragment(RegularServices())
                }
            }
            "Self Service" -> {
                fetchSelfServiceData { selfService ->
                    val fragment = SelfService(selfService.map { it.service })
                    replaceFragment(fragment)
                }
            }
        }
    }

    private fun fetchRegularServicesData(callback: (List<RegularServiceList>) -> Unit) {
        val regularServicesRef = FirebaseDatabase.getInstance().getReference("Services").child("Regular")
        regularServicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val regularServices = mutableListOf<RegularServiceList>()
                for (serviceSnapshot in snapshot.children) {
                    val serviceName = serviceSnapshot.child("name").getValue(String::class.java) ?: ""
                    val servicePrice = serviceSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                    val service = RegularServiceList(ServiceList(serviceName, servicePrice))
                    regularServices.add(service)
                }
                callback(regularServices)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching regular services data: ${error.message}")
            }
        })
    }

    private fun fetchDryCleanData(callback: (List<DryCleanList>) -> Unit) {
        val dryCleanServicesRef = FirebaseDatabase.getInstance().getReference("Services").child("Dry Clean")
        dryCleanServicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dryClean = mutableListOf<DryCleanList>()
                for (serviceSnapshot in snapshot.children) {
                    val serviceName = serviceSnapshot.child("name").getValue(String::class.java) ?: ""
                    val servicePrice = serviceSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                    val service = DryCleanList(ServiceList(serviceName, servicePrice))
                    dryClean.add(service)
                }
                callback(dryClean)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching dry clean services data: ${error.message}")
            }
        })
    }

    private fun fetchSelfServiceData(callback: (List<SelfServiceList>) -> Unit) {
        val selfServicesRef = FirebaseDatabase.getInstance().getReference("Services").child("Self Service")
        selfServicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val selfService = mutableListOf<SelfServiceList>()
                for (serviceSnapshot in snapshot.children) {
                    val serviceName = serviceSnapshot.child("name").getValue(String::class.java) ?: ""
                    val servicePrice = serviceSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                    val service = SelfServiceList(ServiceList(serviceName, servicePrice))
                    selfService.add(service)
                }
                callback(selfService)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error fetching self service data: ${error.message}")
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container_dropdown, fragment)
        transaction.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        db.removeEventListener(listener)
    }
}
