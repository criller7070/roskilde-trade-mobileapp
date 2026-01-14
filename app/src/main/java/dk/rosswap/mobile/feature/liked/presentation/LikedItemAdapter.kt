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
import dk.rosswap.mobile.feature.liked.domain.LikedItem

class LikedItemAdapter(
    private val onItemClick: (LikedItem) -> Unit,
    private val onUnlikeClick: (LikedItem) -> Unit
) : ListAdapter<LikedItem, LikedItemAdapter.ViewHolder>(DiffCallback()) {

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

        fun bind(likedItem: LikedItem) {
            val item: Item = likedItem.item
            binding.tvTitle.text = item.title
            binding.tvSeller.text = item.userName
            binding.tvDescription.text = item.description
            binding.btnContact.text = binding.root.context.getString(R.string.write_to, item.userName)

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

    class DiffCallback : DiffUtil.ItemCallback<LikedItem>() {
        override fun areItemsTheSame(oldItem: LikedItem, newItem: LikedItem) = oldItem.item.id == newItem.item.id
        override fun areContentsTheSame(oldItem: LikedItem, newItem: LikedItem) = oldItem == newItem
    }
}