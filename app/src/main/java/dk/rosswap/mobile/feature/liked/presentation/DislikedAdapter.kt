package dk.rosswap.mobile.feature.liked.presentation

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
import com.google.android.material.button.MaterialButton
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.liked.domain.DislikedItem

class DislikedAdapter(
    private val onItemClick: (DislikedItem) -> Unit = {},
    private val onLikeAgainClicked: (DislikedItem) -> Unit = {},
    private val onUndislikeClicked: (DislikedItem) -> Unit = {}
) : ListAdapter<DislikedItem, DislikedAdapter.VH>(DiffCallback()) {

    companion object {
        private const val TAG = "DislikedAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_item_disliked_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        private val image: ImageView = itemView.findViewById(R.id.iv_post_image)
        private val title: TextView = itemView.findViewById(R.id.tv_title)
        private val desc: TextView = itemView.findViewById(R.id.tv_description)
        private val type: TextView = itemView.findViewById(R.id.tv_type)
        private val author: TextView = itemView.findViewById(R.id.tv_author)
        private val btnLikeAgain: MaterialButton = itemView.findViewById(R.id.btn_message)
        private val btnUndislike: ImageView? = itemView.findViewById(R.id.btn_undislike)

        fun bind(dislikedItem: DislikedItem) {
            // prepare image
            val item = dislikedItem.item
            val url = item.imageUrl.trim()
            if (url.isBlank()) {
                image.setImageResource(R.drawable.loading2)
            } else {
                image.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.loading2)
                    error(R.drawable.loading2)
                    listener(
                        onError = { request: ImageRequest, result: ErrorResult ->
                            Log.e(
                                TAG,
                                "Coil load failed for id=${item.id} url=${request.data}: ${result.throwable.message}",
                                result.throwable
                            )
                        }
                    )
                }
            }

            title.text = item.title
            desc.text = item.description
            type.text = item.mode
            author.text = item.userName.ifBlank { item.userId }

            // click listener for like
            btnLikeAgain.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLikeAgainClicked(getItem(position))
                }
            }
            // click listener for dislike
            btnUndislike?.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUndislikeClicked(getItem(position))
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DislikedItem>() {
        override fun areItemsTheSame(oldItem: DislikedItem, newItem: DislikedItem) = oldItem.item.id == newItem.item.id
        override fun areContentsTheSame(oldItem: DislikedItem, newItem: DislikedItem) = oldItem == newItem
    }
}
