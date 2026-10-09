package com.example.shopiku.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.shopiku.R
import com.example.shopiku.data.model.CartItem
import com.example.shopiku.databinding.ItemCartBinding

class CartAdapter(
    private val onIncreaseQty: (CartItem) -> Unit,
    private val onDecreaseQty: (CartItem) -> Unit,
    private val onDeleteItem: (CartItem) -> Unit,
    private val onItemCheckedChange: (CartItem, Boolean) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    private val checkedItemsMap = mutableMapOf<String, Boolean>()

    fun setAllChecked(isChecked: Boolean) {
        currentList.forEach { item ->
            item.id?.let { checkedItemsMap[it] = isChecked }
        }
        notifyDataSetChanged()
    }

    fun isItemChecked(cartId: String?): Boolean {
        if (cartId == null) return true
        return checkedItemsMap.getOrDefault(cartId, true)
    }

    fun getCheckedItems(): List<CartItem> {
        return currentList.filter { isItemChecked(it.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            val isChecked = isItemChecked(item.id)

            binding.cbCartItem.isChecked = isChecked
            binding.cbStoreSelect.isChecked = isChecked

            binding.tvStoreName.text = "Shopiku Official Store"
            binding.tvCartProductName.text = item.name
            binding.tvCartPrice.text = item.getFormattedPrice()
            binding.tvCartQty.text = item.quantity.toString()

            binding.tvCartVariant.text = "Official Shopiku"

            binding.ivCartProduct.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }

            binding.cbCartItem.setOnCheckedChangeListener { _, checked ->
                item.id?.let { checkedItemsMap[it] = checked }
                onItemCheckedChange(item, checked)
            }

            binding.cbStoreSelect.setOnCheckedChangeListener { _, checked ->
                item.id?.let { checkedItemsMap[it] = checked }
                binding.cbCartItem.isChecked = checked
                onItemCheckedChange(item, checked)
            }

            binding.btnIncreaseCartQty.setOnClickListener {
                onIncreaseQty(item)
            }

            binding.btnDecreaseCartQty.setOnClickListener {
                onDecreaseQty(item)
            }

            binding.btnDeleteCartItem.setOnClickListener {
                onDeleteItem(item)
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
