package com.example.washuplaundry

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
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

class DryCleanService : Fragment() {

    private lateinit var totalTextView: TextView
    private lateinit var servicesContainer: LinearLayout
    private lateinit var dbRef: DatabaseReference
    private lateinit var serviceListener: ValueEventListener
    private lateinit var servicePriceMap: MutableMap<String, Double>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var orderViewModel: OrderViewModel
    private val addedViews = mutableListOf<View>()
    private var total = 0.0
    private var confirmedServiceCount = 0
    private lateinit var addToOrder: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dry_clean_service, container, false)

        orderViewModel = ViewModelProvider(requireActivity())[OrderViewModel::class.java]
        totalTextView = view.findViewById(R.id.total_price)
        servicesContainer = view.findViewById(R.id.services_container)
        sharedPreferences = requireActivity().getSharedPreferences("order_prefs", Context.MODE_PRIVATE)

        addToOrder = view.findViewById(R.id.btnAdd)
        addToOrder.setOnClickListener{
            addToOrder()
        }

        dbRef = FirebaseDatabase.getInstance().getReference("Services/Dry Clean")
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
                    Log.e("DryClean", "Failed to get dry clean services")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to read from database $error.toException()", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayServices(servicePriceMap: Map<String, Double>) {
        servicesContainer.removeAllViews()

        for ((serviceName, price) in servicePriceMap) {
            val serviceView = layoutInflater.inflate(R.layout.dry_clean_item, servicesContainer, false)
//            val serviceNameEditable = Editable.Factory.getInstance().newEditable(serviceName)
//            serviceView.findViewById<EditText>(R.id.service_name).text = serviceNameEditable
//            val servicePriceEditable = Editable.Factory.getInstance().newEditable(price.toString())
//            serviceView.findViewById<EditText>(R.id.service_price).text = servicePriceEditable

//            val pcs = serviceView.findViewById<EditText>(R.id.pcs)
//            pcs.setText("0")

            val buttonCheck = serviceView.findViewById<ImageButton>(R.id.button_check)
            buttonCheck.setOnClickListener { handleButtonClick(serviceView) }

            servicesContainer.addView(serviceView)
        }
    }

    private fun addNewDryCleanItemView() {
        val newServiceView = layoutInflater.inflate(R.layout.dry_clean_item, servicesContainer, false)

        val buttonCheck = newServiceView.findViewById<ImageButton>(R.id.button_check)
        buttonCheck.setOnClickListener { handleButtonClick(newServiceView) }

        servicesContainer.addView(newServiceView)
        addedViews.add(newServiceView)
    }

    private fun removeViews() {
        for (view in addedViews) {
            servicesContainer.removeView(view)
        }
        addedViews.clear()
    }

    private fun addToOrder() {
        var currentTotal = 0.0

        for (i in 0 until servicesContainer.childCount) {
            val pcsInput = servicesContainer.findViewById<EditText>(R.id.pcs)
            val nameInput = servicesContainer.findViewById<EditText>(R.id.service_name)
            val priceInput = servicesContainer.findViewById<EditText>(R.id.service_price)

            val current_pcs = pcsInput.text.toString().toDoubleOrNull() ?: 0.0
            val serviceName = nameInput.text.toString()
            val itemPrice = priceInput.text.toString().removePrefix("₱").toDoubleOrNull() ?: 0.0
            Log.d("itemPrice", "$itemPrice")

            if (!pcsInput.isEnabled && !nameInput.isEnabled && !priceInput.isEnabled) {
                val subtotal = current_pcs * itemPrice
                currentTotal += subtotal

                val existingItem = orderViewModel.orderItems.value?.find { it.dryCleanOrderData.any { it.name == serviceName } }

                if (existingItem != null) {
                    val updatedOrderDataList = existingItem.dryCleanOrderData.map { dryCleanOrderData ->
                        if (dryCleanOrderData.itemName == serviceName) {
                            dryCleanOrderData.copy(
                                itemPcs = dryCleanOrderData.pcs + current_pcs,
                                itemSubtotal = (dryCleanOrderData.pcs + current_pcs) * dryCleanOrderData.itemPrice
                            )
                        } else {
                            dryCleanOrderData
                        }
                    }
                    val updatedOrderItemsData = existingItem.copy(dryCleanOrderData = updatedOrderDataList)
                    orderViewModel.addOrderItem(updatedOrderItemsData)
                } else {
                    val newServiceData = DryCleanOrderData(
                        itemName = serviceName,
                        itemPrice = itemPrice,
                        itemPcs = current_pcs,
                        itemSubtotal = subtotal
                    )
                    val newOrderItemsData = OrderItemsData(emptyList(), emptyList(), listOf(newServiceData))
                    orderViewModel.addOrderItem(newOrderItemsData)
                }
            }

            if (currentTotal > 0.0) {
                orderViewModel.totalOrder()
                Log.d("Total Services Added", confirmedServiceCount.toString())

                resetInputs()
            } else {
                Toast.makeText(
                    context,
                    "Please add at least 1 item to the service.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            removeViews()
        }
    }

    private fun handleButtonClick(serviceView: View) {
        val pcsInput = serviceView.findViewById<EditText>(R.id.pcs)
        val nameInput = serviceView.findViewById<EditText>(R.id.service_name)
        val priceInput = serviceView.findViewById<EditText>(R.id.service_price)
        val buttonCheck = serviceView.findViewById<ImageButton>(R.id.button_check)

        val current_pcs = pcsInput.text.toString().toDoubleOrNull() ?: 0.0
        Log.d("current_pcs", "$current_pcs")
        val serviceName = nameInput.text.toString()
        val itemPrice = priceInput.text.toString().removePrefix("₱").toDoubleOrNull() ?: 0.0
        Log.d("itemPrice", "$itemPrice")

        if (current_pcs > 0.0 && serviceName.isNotBlank() && itemPrice > 0.0) {
            val subtotal = current_pcs * itemPrice
            total += subtotal
            totalTextView.text = "Total: ₱$total"
            val priceInput = serviceView.findViewById<EditText>(R.id.service_price)
            priceInput.setText("₱" + itemPrice.toString())
            val pcsInput = serviceView.findViewById<EditText>(R.id.pcs)
            pcsInput.setText(current_pcs.toString())
            addNewDryCleanItemView()

            pcsInput.isEnabled = false
            nameInput.isEnabled = false
            priceInput.isEnabled = false
            buttonCheck.visibility = View.GONE

            confirmedServiceCount++
            Log.d("Total Services Added", confirmedServiceCount.toString())

        } else {
            Toast.makeText(context, "Please enter valid input in all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetInputs() {
        for (i in 0 until servicesContainer.childCount) {
            val serviceView = servicesContainer.getChildAt(i)
            val pcsInput = serviceView.findViewById<EditText>(R.id.pcs)
            val nameInput = serviceView.findViewById<EditText>(R.id.service_name)
            val priceInput = serviceView.findViewById<EditText>(R.id.service_price)
            val buttonCheck = serviceView.findViewById<ImageButton>(R.id.button_check)

            pcsInput.setText("")
            nameInput.setText("")
            priceInput.setText("")

            pcsInput.isEnabled = true
            nameInput.isEnabled = true
            priceInput.isEnabled = true
            buttonCheck.visibility = View.VISIBLE
            totalTextView.text = getString(R.string.total_label)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRef.removeEventListener(serviceListener)
    }
}
