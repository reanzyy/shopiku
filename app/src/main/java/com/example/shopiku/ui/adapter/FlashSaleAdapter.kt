package com.example.shopiku.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.shopiku.R
import com.example.shopiku.data.model.FlashSaleItem
import com.example.shopiku.databinding.ItemFlashSaleBinding

class FlashSaleAdapter(
    private val onItemClick: (FlashSaleItem) -> Unit
) : ListAdapter<FlashSaleItem, FlashSaleAdapter.FlashSaleViewHolder>(FlashSaleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlashSaleViewHolder {
        val binding = ItemFlashSaleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FlashSaleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FlashSaleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FlashSaleViewHolder(
        private val binding: ItemFlashSaleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FlashSaleItem) {
            binding.tvFlashSaleDiscount.text = "-${item.discountPercent}%"
            binding.tvFlashSalePrice.text = item.getFormattedFlashSalePrice()
            binding.tvFlashSaleOriginalPrice.text = item.getFormattedOriginalPrice()
            binding.tvFlashSaleOriginalPrice.paintFlags =
                binding.tvFlashSaleOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            binding.progressFlashSaleStock.progress = item.stockPercentage
            binding.tvFlashSaleStock.text = item.stockText

            binding.ivFlashSaleImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class FlashSaleDiffCallback : DiffUtil.ItemCallback<FlashSaleItem>() {
        override fun areItemsTheSame(oldItem: FlashSaleItem, newItem: FlashSaleItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FlashSaleItem, newItem: FlashSaleItem): Boolean =
            oldItem == newItem
    }
}
