package com.example.washuplaundry

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrderDataAdapter(var orderItems: List<OrderData>) :
        RecyclerView.Adapter<OrderDataAdapter.OrderDataViewHolder>() {

        class OrderDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val name: TextView = itemView.findViewById(R.id.item_name)
                val price: TextView = itemView.findViewById(R.id.item_price)
                val kilo: TextView = itemView.findViewById(R.id.item_kilo)
                val subTotal: TextView = itemView.findViewById(R.id.item_subtotal)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDataViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val view = layoutInflater.inflate(R.layout.fragment_total_preview_data, parent, false)
                return OrderDataViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderDataViewHolder, position: Int) {
                val orderItem = orderItems[position]
                holder.name.text = orderItem.name
                holder.price.text = "₱" + String.format("%.2f", orderItem.price)
                holder.kilo.text = String.format("%.2f", orderItem.kilo) + " kg"
                holder.subTotal.text = "₱" + String.format("%.2f", orderItem.subtotal)
        }

        override fun getItemCount(): Int {
                Log.d("OrderItems Size", "Size of orderItems: ${orderItems.size}")
                return orderItems.size
        }

        }
