package dk.rosswap.mobile.feature.liked.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.ItemLikedPostBinding

class LikedItemAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onUnlikeClick: (Item) -> Unit
) : ListAdapter<Item, LikedItemAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLikedPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemLikedPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            binding.btnUnlike.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUnlikeClick(getItem(position))
                }
            }
            binding.btnContact.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: Item) {
            binding.tvTitle.text = item.title
            binding.tvSeller.text = item.userName
            binding.tvDescription.text = item.description
            binding.btnContact.text = "Write to ${item.userName}"

            if (item.imageUrl.isNotBlank()) {
                binding.ivImage.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_photo_placeholder)
                    error(R.drawable.ic_photo_placeholder)
                }
            } else {
                binding.ivImage.setImageResource(R.drawable.ic_photo_placeholder)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}