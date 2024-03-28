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

class SelfService : Fragment() {

    private lateinit var totalTextView: TextView
    private lateinit var servicesContainer: LinearLayout
    private lateinit var dbRef: DatabaseReference
    private lateinit var serviceListener: ValueEventListener
    private lateinit var servicePriceMap: MutableMap<String, Double>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var orderViewModel: OrderViewModel
    private var total = 0.0
    private lateinit var addToOrder: Button

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

        dbRef = FirebaseDatabase.getInstance().getReference("Services/Self Service")
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
                    Log.e("SelfService", "Failed to get self services")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SelfService", "Failed to read from database", error.toException())
            }
        })
    }

    private fun displayServices(servicePriceMap: Map<String, Double>) {
        servicesContainer.removeAllViews()

        for ((serviceName, price) in servicePriceMap) {
            val serviceView = layoutInflater.inflate(R.layout.selfservice_item, servicesContainer, false)
            serviceView.findViewById<TextView>(R.id.service_name).text = serviceName
            serviceView.findViewById<TextView>(R.id.service_price).text = getString(R.string.price_label) + " $price"

            val loadorpcs = serviceView.findViewById<EditText>(R.id.load_pcs)
            loadorpcs.setText("0.0")

            val buttonMinus = serviceView.findViewById<ImageButton>(R.id.button_minus)
            val buttonPlus = serviceView.findViewById<ImageButton>(R.id.button_plus)

            buttonMinus.setOnClickListener {
                val current_loadorpcs = loadorpcs.text.toString().toDoubleOrNull() ?: 0.0
                Log.d("current_loadorpcs", "$current_loadorpcs")

                val newLoadorPcs = current_loadorpcs - 1.0

                if (newLoadorPcs >= 0.0) {
                    loadorpcs.setText("${String.format("%.2f", newLoadorPcs)}")
                    updateTotalPrice()
                } else {
                    Toast.makeText(context, "Invalid Input", Toast.LENGTH_SHORT).show()
                }
            }

            buttonPlus.setOnClickListener {
                val current_loadorpcs = loadorpcs.text.toString().toDoubleOrNull() ?: 0.0
                Log.d("current_loadorpcs", "$current_loadorpcs")

                val newLoadorPcs = current_loadorpcs + 1

                if (newLoadorPcs >= 0.0) {
                    loadorpcs.setText("${String.format("%.2f", newLoadorPcs)}")
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

            val kiloInput = serviceView.findViewById<EditText>(R.id.load_pcs)
            val enteredKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0

            val subtotal = enteredKilo * price
            total += subtotal
        }

        totalTextView.text = "Total: ₱${String.format("%.2f", total)}"
    }

    private fun addToOrder() {
        var currentTotal = 0.0

        for (i in 0 until servicesContainer.childCount) {
            val serviceView = servicesContainer.getChildAt(i)
            val serviceName = serviceView.findViewById<TextView>(R.id.service_name).text.toString()
            val price = servicePriceMap[serviceName] ?: 0.0
            val kiloInput = serviceView.findViewById<EditText>(R.id.load_pcs)
            val enteredLoadorPcs = kiloInput.text.toString().toDoubleOrNull() ?: 0.0

            if (enteredLoadorPcs > 0.0) {
                val subtotal = enteredLoadorPcs * price
                currentTotal += subtotal

                val existingService = orderViewModel.orderItems.value?.find {
                    it.selfServiceOrderData.any { item -> item.itemName == serviceName }
                }

                if (existingService != null) {
                    val updatedServiceData = existingService.selfServiceOrderData.map { item ->
                        if (item.itemName == serviceName) {
                            item.copy(itemLoadOrPcs = enteredLoadorPcs, itemSubtotal = subtotal)
                        } else {
                            item
                        }
                    }
                    val updatedOrderItemsData = existingService.copy(selfServiceOrderData = updatedServiceData)
                    orderViewModel.addOrderItem(updatedOrderItemsData)
                } else {
                    val newServiceData = SelfServiceOrderData(
                        itemName = serviceName,
                        itemPrice = price,
                        itemLoadOrPcs = enteredLoadorPcs,
                        itemSubtotal = subtotal
                    )
                    val newOrderItemsData = OrderItemsData(emptyList(), listOf(newServiceData), emptyList())
                    orderViewModel.addOrderItem(newOrderItemsData)
                }
            }
        }

        if (currentTotal > 0.0) {
            orderViewModel.totalOrder()
            resetInputs()
        } else {
            Toast.makeText(
                context,
                "Please add at least 1 item to the service.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun resetInputs() {
        for (i in 0 until servicesContainer.childCount) {
            val serviceView = servicesContainer.getChildAt(i)
            val kiloInput = serviceView.findViewById<EditText>(R.id.load_pcs)
            kiloInput.setText("0.0")
            totalTextView.text = getString(R.string.total_label)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRef.removeEventListener(serviceListener)
    }
}
