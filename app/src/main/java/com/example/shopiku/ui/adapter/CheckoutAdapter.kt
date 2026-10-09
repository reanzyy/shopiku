package com.example.shopiku.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.shopiku.R
import com.example.shopiku.data.model.CheckoutItem
import com.example.shopiku.databinding.ItemCheckoutBinding

class CheckoutAdapter : ListAdapter<CheckoutItem, CheckoutAdapter.CheckoutViewHolder>(CheckoutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckoutViewHolder {
        val binding = ItemCheckoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CheckoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CheckoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CheckoutViewHolder(
        private val binding: ItemCheckoutBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CheckoutItem) {
            binding.tvCheckoutProductName.text = item.productName
            binding.tvCheckoutQuantity.text = "Kuantitas: ${item.quantity}x Unit"
            binding.tvCheckoutPrice.text = "@${item.getFormattedPrice()}"
            binding.tvCheckoutSubtotal.text = item.getFormattedSubtotal()

            binding.ivCheckoutProduct.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }
        }
    }

    class CheckoutDiffCallback : DiffUtil.ItemCallback<CheckoutItem>() {
        override fun areItemsTheSame(oldItem: CheckoutItem, newItem: CheckoutItem): Boolean {
            return oldItem.productId == newItem.productId && oldItem.cartItemId == newItem.cartItemId
        }

        override fun areContentsTheSame(oldItem: CheckoutItem, newItem: CheckoutItem): Boolean {
            return oldItem == newItem
        }
    }
}
