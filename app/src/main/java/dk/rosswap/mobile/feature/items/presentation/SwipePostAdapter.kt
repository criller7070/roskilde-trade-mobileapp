package dk.rosswap.mobile.feature.items.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dk.rosswap.mobile.feature.items.domain.Post
import java.text.SimpleDateFormat
import java.util.Locale
import dk.rosswap.mobile.R

class SwipePostAdapter(
    private val onClick: (Post) -> Unit = {}
) : ListAdapter<Post, SwipePostAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("d MMM yyyy 'at' HH:mm", Locale.getDefault())

    fun setPosts(newPosts: List<Post>) {
        submitList(newPosts)
    }

    fun addPost(post: Post) {
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

        fun bind(post: Post, click: (Post) -> Unit) {
            titleTv.text = post.title
            descTv.text = post.description
            userTv.text = post.userName ?: ""
            Glide.with(itemView).load(post.imageUrl).centerCrop().into(imageIv)
            itemView.setOnClickListener { click(post) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}
