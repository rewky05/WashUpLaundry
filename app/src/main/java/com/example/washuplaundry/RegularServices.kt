package com.example.washuplaundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class RegularServices : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var regularServiceAdapter: RegularServiceAdapter

//    var regularServices: List<RegularServiceList> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_regular_services, container, false)
        val database = FirebaseDatabase.getInstance().reference

        recyclerView = view.findViewById(R.id.recyclerViewRegularServices)
        recyclerView.layoutManager = LinearLayoutManager(context)
        regularServiceAdapter =
            RegularServiceAdapter(getRegularServices(database)) // Provide your regular services data here
        recyclerView.adapter = regularServiceAdapter

        return view
    }

    private fun getRegularServices(database: DatabaseReference): MutableList<RegularServiceList> {
        val regularServices = mutableListOf<RegularServiceList>()

        // Reference to the "Regular" node in your Firebase database
        val regularServicesRef = database.child("Services").child("Regular")

        // Fetch data from Firebase
        regularServicesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterate through the children of "Regular" node
                for (serviceSnapshot in snapshot.children) {
                    // Get service details from the snapshot
                    val serviceName =
                        serviceSnapshot.child("name").getValue(String::class.java) ?: ""
                    val servicePrice =
                        serviceSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                    val serviceKilo =
                        serviceSnapshot.child("kilo").getValue(Double::class.java) ?: 0.0

                    val serviceList = ServiceList(serviceName, servicePrice)
                    val regularService = RegularServiceList(serviceList, serviceKilo)

                    regularServices.add(regularService)
                }

                // Notify adapter of data change
                regularServiceAdapter.setData(regularServices)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })

        return regularServices
    }
}