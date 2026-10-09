package com.example.shopiku.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shopiku.data.model.Review
import com.example.shopiku.databinding.ItemReviewBinding

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.tvReviewUsername.text = review.userName?.takeIf { it.isNotBlank() } ?: "Pembeli Shopiku"
            binding.ratingBarReview.rating = review.rating.toFloat()
            binding.tvReviewDate.text = formatDate(review.createdAt)
            binding.tvReviewComment.text = review.comment.takeIf { !it.isNullOrBlank() } ?: "Tidak ada ulasan tertulis."

            // Sembunyikan field yang tidak digunakan di backend schema
            binding.tvReviewVariant.visibility = View.GONE
            binding.reviewPhotosContainer.parent?.let { parent ->
                if (parent is View) parent.visibility = View.GONE
            }
        }

        private fun formatDate(rawDate: String?): String {
            if (rawDate.isNullOrBlank()) return ""
            return try {
                if (rawDate.contains("T")) {
                    val datePart = rawDate.split("T")[0]
                    val parts = datePart.split("-")
                    if (parts.size == 3) {
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    } else datePart
                } else {
                    rawDate
                }
            } catch (e: Exception) {
                rawDate
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Review>() {
            override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
                return oldItem == newItem
            }
        }
    }
}
