package com.example.shopiku.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.shopiku.R
import com.example.shopiku.data.model.Product
import com.example.shopiku.databinding.ItemProductBinding

class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.name ?: ""
            binding.tvPrice.text = product.getFormattedPrice()
            binding.tvRating.text = "★ ${product.rating ?: 0.0}"

            val categoryStr = product.category
            if (!categoryStr.isNullOrEmpty()) {
                binding.tvLocation.text = categoryStr
                binding.tvLocation.visibility = View.VISIBLE
            } else {
                binding.tvLocation.visibility = View.GONE
            }

            binding.tvOriginalPrice.visibility = View.GONE
            binding.tvDiscountBadge.visibility = View.GONE
            binding.tvSold.visibility = View.GONE

            binding.ivProductImage.load(product.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }

            binding.root.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
            oldItem == newItem
    }
}
