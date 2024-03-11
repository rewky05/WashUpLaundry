package com.example.washuplaundry

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RegularService : Fragment() {

    private lateinit var totalTextView: TextView
    private lateinit var servicesContainer: LinearLayout
    private lateinit var dbRef: DatabaseReference
    private lateinit var serviceListener: ValueEventListener
    private lateinit var servicePriceMap: MutableMap<String, Double>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var orderViewModel: OrderViewModel
    private var total = 0.0
    private lateinit var addToOrder: Button

    companion object {
        fun newInstance(): RegularService {
            return RegularService()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_regular_service, container, false)

        orderViewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]
        totalTextView = view.findViewById(R.id.total_price)
        servicesContainer = view.findViewById(R.id.services_container)
        sharedPreferences = requireActivity().getSharedPreferences("order_prefs", Context.MODE_PRIVATE)
        addToOrder = view.findViewById(R.id.btnAdd)
        addToOrder.setOnClickListener{
            addToOrder()
        }

        dbRef = FirebaseDatabase.getInstance().getReference("Services/Regular")
        fetchDataFromFirebase()

        return view
    }

    private fun fetchDataFromFirebase() {
        servicePriceMap = mutableMapOf()
        serviceListener = dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (serviceSnapshot in snapshot.children) {
                        val service = serviceSnapshot.getValue(Service::class.java) ?: continue
                        servicePriceMap[service.name] = service.price
                    }
                    displayServices(servicePriceMap)
                } else {
                    Log.e("RegularService", "Failed to get regular services")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RegularService", "Failed to read from database", error.toException())
            }
        })
    }

    private fun displayServices(servicePriceMap: Map<String, Double>) {
        servicesContainer.removeAllViews()

        for ((serviceName, price) in servicePriceMap) {
            val serviceView = layoutInflater.inflate(R.layout.service_item, servicesContainer, false)
            serviceView.findViewById<TextView>(R.id.service_name).text = serviceName
            serviceView.findViewById<TextView>(R.id.service_price).text = getString(R.string.price_label) + " $price"

            val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
            kiloInput.setText("0.0")

            val buttonMinus = serviceView.findViewById<ImageButton>(R.id.button_minus)
            val buttonPlus = serviceView.findViewById<ImageButton>(R.id.button_plus)

            buttonMinus.setOnClickListener {
                val currentKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0
                val newKilo = if (currentKilo == 3.0) 0.0 else currentKilo - 0.5

                if (newKilo >= 0.0) { // Only update if valid
                    kiloInput.setText(newKilo.toString())
                    updateTotalPrice()
                } else {
                    Toast.makeText(context, "Invalid input: Please enter decimal numbers only", Toast.LENGTH_SHORT).show()
                }
            }

            buttonPlus.setOnClickListener {
                val currentKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0
                val newKilo = if (currentKilo == 0.0) 3.0 else currentKilo + 0.5

                if (newKilo >= 0.0) {
                    kiloInput.setText(newKilo.toString())
                    updateTotalPrice()
                } else {
                    Toast.makeText(context, "Invalid input: Please enter decimal numbers only", Toast.LENGTH_SHORT).show()
                }
            }

            servicesContainer.addView(serviceView)
        }
    }

    private fun updateTotalPrice() {
        total = 0.0

        for (i in 0 until servicesContainer.childCount) {
            val serviceView = servicesContainer.getChildAt(i)

            val serviceName = serviceView.findViewById<TextView>(R.id.service_name).text.toString()
            val price = servicePriceMap[serviceName] ?: 0.0

            val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
            val enteredKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0

            val subtotal = enteredKilo * price
            total += subtotal
        }

        totalTextView.text = getString(R.string.total_label) + " $total"
    }

    private fun addToOrder() {
        val currentTotal = total

        orderViewModel.updateTotalPrice(currentTotal)

        for (i in 0 until servicesContainer.childCount) {
            val serviceView = servicesContainer.getChildAt(i)

            val serviceName = serviceView.findViewById<TextView>(R.id.service_name).text.toString()
            val price = servicePriceMap[serviceName] ?: 0.0
            val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
            val enteredKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0
            val subtotal = enteredKilo * price

            // Add to orderItems only if quantity is not zero
            if (enteredKilo > 0.0) {
                val orderItem = OrderData(name = serviceName, price = price, kilo = enteredKilo, subtotal = subtotal)
                orderViewModel.addOrderItem(orderItem)
            }
        }
        resetInputs()
    }

    private fun resetInputs() {
        for (i in 0 until servicesContainer.childCount) {
            val serviceView = servicesContainer.getChildAt(i)
            val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
            kiloInput.setText("0.0")
            totalTextView.text = getString(R.string.total_label)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRef.removeEventListener(serviceListener)
    }
}
