package dk.rosswap.mobile.feature.items.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dk.rosswap.mobile.databinding.ItemSwipePostBinding
import dk.rosswap.mobile.core.model.Item

class ViewPager2Adapter : ListAdapter<Item, ViewPager2Adapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemSwipePostBinding.inflate(inflater, parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemSwipePostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Item) {
            binding.tvTitle.text = post.title
            binding.tvAuthor.text = post.userName
            binding.tvDescription.text = post.description

            Glide.with(binding.root)
                .load(post.imageUrl)
                .centerCrop()
                .into(binding.ivPostImage)
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.userId == newItem.userId
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
