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
    private var kiloTracker = 0.0
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
                for (i in 0 until servicesContainer.childCount) {
                    val serviceView = servicesContainer.getChildAt(i)
                    val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
                    kiloTracker = kiloInput.text.toString().toDoubleOrNull() ?: 0.0
                }

                val currentKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0
                Log.d("currentKilo", "$currentKilo")

                Log.d("KiloTracker", "Updated kiloTracker: $kiloTracker")
                val newKilo =
                    if (currentKilo >= 4.0) {
                        currentKilo - 1.0
                    }
                    else if (currentKilo <= 3.0) {
                        currentKilo - 3.0
                    } else {
                        currentKilo - 1.0
                    }

                if (newKilo >= 0.0) {
                    kiloInput.setText("${String.format("%.2f", newKilo)}")
                    updateTotalPrice()
                } else {
                    kiloInput.setText((currentKilo * 0.0).toString())
                    updateTotalPrice()
                }
            }

            buttonPlus.setOnClickListener {
                val totalPrice = orderViewModel.totalPrice.value?.toDouble() ?: 0.0

                for (i in 0 until servicesContainer.childCount) {
                    val serviceView = servicesContainer.getChildAt(i)
                    val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
                    kiloTracker += kiloInput.text.toString().toDoubleOrNull() ?: 0.0
                }

                val currentKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0
                Log.d("currentKilo", "$currentKilo")

                Log.d("KiloTracker", "Updated kiloTracker: $kiloTracker")
                val newKilo =
                    if (kiloTracker >= 3.0 || currentKilo >= 3.0) {
                        currentKilo + 1.0
                    } else if (totalPrice >= 87.0) {
                        currentKilo + 1.0
                    } else {
                        currentKilo + 3.0
                    }

                if (newKilo >= 0.0) {
                    kiloInput.setText("${String.format("%.2f", newKilo)}")
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

        totalTextView.text = "Total: ₱${String.format("%.2f", total)}"
    }

    private fun addToOrder() {
        var currentTotal = 0.0
        var hasMinimumService = false // Flag to check if at least one service has 3 kilos
        val totalPrice = orderViewModel.totalPrice.value?.toDouble() ?: 0.0

        for (i in 0 until servicesContainer.childCount) {
            val serviceView = servicesContainer.getChildAt(i)
            val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
            val enteredKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0
            if (enteredKilo >= 3.0) {
                hasMinimumService = true
                break
            }
        }

        if (hasMinimumService) {
            for (i in 0 until servicesContainer.childCount) {
                val serviceView = servicesContainer.getChildAt(i)
                val serviceName = serviceView.findViewById<TextView>(R.id.service_name).text.toString()
                val price = servicePriceMap[serviceName] ?: 0.0
                val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
                val enteredKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0

                if (enteredKilo > 0.0) { // Allow adding more kilos if the entered kilos are greater than 0
                    val subtotal = enteredKilo * price
                    val existingItem = orderViewModel.orderItems.value?.find { it.orderData.any { it.name == serviceName } }

                    if (existingItem != null) {
                        val updatedOrderDataList = existingItem.orderData.map { orderData ->
                            if (orderData.itemName == serviceName) {
                                orderData.copy(
                                    itemKilo = orderData.kilo + enteredKilo,
                                    itemSubtotal = (orderData.kilo + enteredKilo) * orderData.itemPrice
                                )
                            } else {
                                orderData
                            }
                        }
                        val updatedOrderItemsData = existingItem.copy(orderData = updatedOrderDataList)
                        orderViewModel.addOrderItem(updatedOrderItemsData)
                    } else {
                        val orderData = OrderData(
                            itemName = serviceName,
                            itemPrice = price,
                            itemKilo = enteredKilo,
                            itemSubtotal = subtotal
                        )
                        val orderItemsData = OrderItemsData(listOf(orderData), emptyList(), emptyList())
                        orderViewModel.addOrderItem(orderItemsData)
                    }
                    currentTotal += subtotal
                }
            }

            if (currentTotal > 0.0) {
                orderViewModel.totalOrder()
                resetInputs()
            } else {
                Toast.makeText(
                    context,
                    "Please add at least 1 kilo to a service.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (totalPrice >= 87.0) {
            for (i in 0 until servicesContainer.childCount) {
                val serviceView = servicesContainer.getChildAt(i)
                val serviceName =
                    serviceView.findViewById<TextView>(R.id.service_name).text.toString()
                val price = servicePriceMap[serviceName] ?: 0.0
                val kiloInput = serviceView.findViewById<EditText>(R.id.kiloInput)
                val enteredKilo = kiloInput.text.toString().toDoubleOrNull() ?: 0.0

                if (enteredKilo > 0.0) {
                    val subtotal = enteredKilo * price
                    val existingItem = orderViewModel.orderItems.value?.find { it.orderData.any { it.name == serviceName } }

                    if (existingItem != null) {
                        val updatedOrderDataList = existingItem.orderData.map { orderData ->
                            if (orderData.itemName == serviceName) {
                                // Update existing orderData item
                                orderData.copy(
                                    itemKilo = orderData.kilo + enteredKilo,
                                    itemSubtotal = (orderData.kilo + enteredKilo) * orderData.itemPrice
                                )
                            } else {
                                orderData
                            }
                        }
                        val updatedOrderItemsData = existingItem.copy(orderData = updatedOrderDataList)
                        orderViewModel.addOrderItem(updatedOrderItemsData)
                    } else {
                        val orderData = OrderData(
                            itemName = serviceName,
                            itemPrice = price,
                            itemKilo = enteredKilo,
                            itemSubtotal = subtotal
                        )
                        val orderItemsData = OrderItemsData(listOf(orderData), emptyList(),  emptyList())
                        orderViewModel.addOrderItem(orderItemsData)
                    }
                    currentTotal += subtotal
                }
            }
            if (currentTotal > 0.0) {
                orderViewModel.totalOrder()
                resetInputs()
            }
        } else {
            Toast.makeText(
                context,
                "At least one service must have 3 kilos to proceed",
                Toast.LENGTH_SHORT
            ).show()
        }
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
