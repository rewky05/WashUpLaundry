// RegularServiceAdapter.kt
package com.example.washuplaundry

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RegularServiceAdapter(private val regularServices: MutableList<RegularServiceList>) :
    RecyclerView.Adapter<RegularServiceAdapter.RegularServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegularServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_regular_service, parent, false)
        return RegularServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegularServiceViewHolder, position: Int) {
        val regularService = regularServices[position]
        holder.bind(regularService, position)
    }

    override fun getItemCount(): Int {
        return regularServices.size
    }

    fun setData(newRegularServices: List<RegularServiceList>) {
        regularServices.clear()
        regularServices.addAll(newRegularServices)
        notifyDataSetChanged()
    }

    inner class RegularServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewServiceName: TextView = itemView.findViewById(R.id.serviceName)
        private val editTextQuantity: EditText = itemView.findViewById(R.id.quantityEditText)
        private val textViewPrice: TextView = itemView.findViewById(R.id.priceTextView)

        fun bind(regularService: RegularServiceList, position: Int) {
            textViewServiceName.text = regularService.service.name
            editTextQuantity.setText(regularService.kilo.toString())

            // Set click listeners for arrow buttons
            itemView.findViewById<View>(R.id.btnDecrease).setOnClickListener {
                decreaseQuantity(regularService, position)
            }
            itemView.findViewById<View>(R.id.btnIncrease).setOnClickListener {
                increaseQuantity(regularService, position)
            }

            // Calculate and display price dynamically
            calculateAndUpdatePrice(regularService.service.price, regularService.kilo)
        }

        private fun decreaseQuantity(regularService: RegularServiceList, position: Int) {
            val currentQuantity = editTextQuantity.text.toString().toIntOrNull() ?: 0
            val newQuantity = if (currentQuantity > 0) currentQuantity - 1 else 0
            editTextQuantity.setText(newQuantity.toString())
            // Recalculate and update price
            calculateAndUpdatePrice(regularService.service.price, newQuantity.toDouble())
            // Notify that item at position has changed
            notifyItemChanged(position)
        }

        private fun increaseQuantity(regularService: RegularServiceList, position: Int) {
            val currentQuantity = editTextQuantity.text.toString().toIntOrNull() ?: 0
            val newQuantity = currentQuantity + 1
            editTextQuantity.setText(newQuantity.toString())
            // Recalculate and update price
            calculateAndUpdatePrice(regularService.service.price, newQuantity.toDouble())
            // Notify that item at position has changed
            notifyItemChanged(position)
        }

        private fun calculateAndUpdatePrice(price: Double, quantity: Double) {
            val totalPrice = price * quantity
            textViewPrice.text = itemView.context.getString(R.string.price_format, totalPrice)
        }

    }
}
