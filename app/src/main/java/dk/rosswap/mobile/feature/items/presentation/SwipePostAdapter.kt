package dk.rosswap.mobile.feature.items.presentation

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ErrorResult
import coil.request.ImageRequest
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.R

class SwipePostAdapter(
    private val onClick: (Item) -> Unit = {}
) : ListAdapter<Item, SwipePostAdapter.ViewHolder>(DiffCallback()) {

    fun setPosts(newPosts: List<Item>) {
        submitList(newPosts)
    }

    fun addPost(post: Item) {
        val currentList = currentList.toMutableList()
        currentList.add(0, post)
        submitList(currentList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Replace `R.layout.item_swipe_post` with your actual item layout
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_swipe_post, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Replace these IDs with the IDs in your item layout
        private val titleTv: TextView = itemView.findViewById(R.id.tv_title)
        private val descTv: TextView = itemView.findViewById(R.id.tv_description)
        private val userTv: TextView = itemView.findViewById(R.id.tv_author)
        private val imageIv: ImageView = itemView.findViewById(R.id.iv_post_image)

        fun bind(post: Item, click: (Item) -> Unit) {
            titleTv.text = post.title
            descTv.text = post.description
            userTv.text = post.userName

            // Use Coil to load images with the same loading placeholder as ItemsAdapter
            val url = post.imageUrl.trim()
            if (url.isBlank()) {
                imageIv.setImageResource(R.drawable.loading2)
            } else {
                imageIv.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.loading2)
                    error(R.drawable.loading2)
                    listener(
                        onError = { request: ImageRequest, result: ErrorResult ->
                            Log.e(
                                "SwipePostAdapter",
                                "Coil load failed for id=${post.id} url=${request.data}: ${result.throwable.message}",
                                result.throwable
                            )
                        }
                    )
                }
            }

            itemView.setOnClickListener { click(post) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
    }
}
