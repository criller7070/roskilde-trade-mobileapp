package dk.rosswap.mobile.feature.chat.presentation

import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.ImageRequest
import coil.request.ErrorResult
import coil.request.SuccessResult
import com.google.firebase.storage.FirebaseStorage
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.utils.ChatTimeFormatter
import dk.rosswap.mobile.databinding.ItemMessageReceivedBinding
import dk.rosswap.mobile.databinding.ItemMessageSentBinding
import dk.rosswap.mobile.feature.chat.domain.ChatMessage

class ChatMessageAdapter(
    private val currentUserId: () -> String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<ChatMessage>()

    fun submit(list: List<ChatMessage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val msg = items[position]
        val isSent = msg.senderId != null && msg.senderId == currentUserId()
        return if (isSent) VIEW_SENT else VIEW_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SENT) {
            SentVH(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedVH(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        when (holder) {
            is SentVH -> holder.bind(msg)
            is ReceivedVH -> holder.bind(msg)
        }
    }

    override fun getItemCount(): Int = items.size

    class SentVH(private val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            val raw = msg.imageUrl?.trim().orEmpty()

            if (raw.isNotBlank()) {
                binding.tvMessage.visibility = View.GONE
                binding.ivMessage.visibility = View.VISIBLE

                binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                binding.ivMessage.tag = raw

                binding.ivMessage.contentDescription = msg.text?.takeIf { it.isNotBlank() } ?: binding.root.context.getString(R.string.image_message_content_desc)

                ImageLoader.loadImage(binding.ivMessage, raw, "SentVH")
            } else if (!msg.text.isNullOrEmpty()) {
                binding.tvMessage.visibility = View.VISIBLE
                binding.ivMessage.visibility = View.GONE
                binding.tvMessage.text = msg.text
            } else {
                binding.tvMessage.visibility = View.VISIBLE
                binding.ivMessage.visibility = View.GONE
                binding.tvMessage.text = binding.root.context.getString(R.string.chat_empty_message)
            }

            binding.tvTime.text = ChatTimeFormatter.formatRelativeSeconds(binding.root.context, msg.timestamp?.seconds)
        }
    }

    class ReceivedVH(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            val raw = msg.imageUrl?.trim().orEmpty()

            if (raw.isNotBlank()) {
                binding.tvMessage.visibility = View.GONE
                binding.ivMessage.visibility = View.VISIBLE

                binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)

                binding.ivMessage.contentDescription = msg.text?.takeIf { it.isNotBlank() } ?: binding.root.context.getString(R.string.image_message_content_desc)

                ImageLoader.loadImage(binding.ivMessage, raw, "ReceivedVH")
            } else if (!msg.text.isNullOrEmpty()) {
                binding.tvMessage.visibility = View.VISIBLE
                binding.ivMessage.visibility = View.GONE
                binding.tvMessage.text = msg.text
            } else {
                binding.tvMessage.visibility = View.VISIBLE
                binding.ivMessage.visibility = View.GONE
                binding.tvMessage.text = binding.root.context.getString(R.string.chat_empty_message)
            }

            binding.tvTime.text = ChatTimeFormatter.formatRelativeSeconds(binding.root.context, msg.timestamp?.seconds)
        }
    }

    private object ImageLoader {
        private const val MAX_IMAGE_BYTES = 2L * 1024L * 1024L // 2MB
        
        /**
         * Loads an image from either HTTP(S) URL or Firebase Storage path into the provided ImageView.
         * 
         * @param imageView The ImageView to load the image into
         * @param imageUrl The image URL or Firebase Storage path
         * @param logTag Tag for logging (e.g., "SentVH" or "ReceivedVH")
         */
        fun loadImage(imageView: ImageView, imageUrl: String, logTag: String) {
            Log.d(TAG, "$logTag: Loading image raw=$imageUrl")
            
            try {
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("//")) {
                    // HTTP(s) or protocol-relative -> normalize and load
                    val normalized = if (imageUrl.startsWith("//")) "https:$imageUrl" else imageUrl
                    loadImageWithCoil(imageView, normalized, logTag)
                } else {
                    // Treat as Firebase Storage path or gs:// URL; resolve to downloadUrl; on failure try getBytes fallback
                    val storage = FirebaseStorage.getInstance()
                    val ref = if (imageUrl.startsWith("gs://")) {
                        storage.getReferenceFromUrl(imageUrl)
                    } else {
                        storage.reference.child(imageUrl)
                    }
                    
                    ref.downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.d(TAG, "$logTag: Resolved storage url $imageUrl -> $uri")
                            loadImageWithCoil(imageView, uri.toString(), logTag)
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "$logTag: Failed to resolve storage url $imageUrl, trying byte download", e)
                            // Try to fetch bytes directly as fallback
                            ref.getBytes(MAX_IMAGE_BYTES)
                                .addOnSuccessListener { bytes ->
                                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    if (bmp != null) {
                                        imageView.setImageBitmap(bmp)
                                    } else {
                                        imageView.setImageResource(R.drawable.ic_photo_placeholder)
                                    }
                                }
                                .addOnFailureListener { e2 ->
                                    Log.e(TAG, "$logTag: getBytes fallback failed for $imageUrl", e2)
                                    imageView.setImageResource(R.drawable.ic_photo_placeholder)
                                }
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "$logTag: Exception while loading image $imageUrl", e)
                imageView.setImageResource(R.drawable.ic_photo_placeholder)
            }
        }

        private fun loadImageWithCoil(imageView: ImageView, url: String, logTag: String) {
            try {
                imageView.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_photo_placeholder)
                    error(R.drawable.ic_photo_placeholder)
                    listener(
                        onSuccess = { _: ImageRequest, result: SuccessResult ->
                            val drawable = result.drawable
                            if (drawable == null) {
                                Log.w(TAG, "$logTag: Image loaded but drawable is null for $url")
                                imageView.setImageResource(R.drawable.ic_photo_placeholder)
                            } else {
                                Log.d(TAG, "$logTag: Image loaded successfully for $url")
                            }
                        },
                        onError = { _: ImageRequest, result: ErrorResult ->
                            Log.e(TAG, "$logTag: Image load failed for $url: ${result.throwable?.message}", result.throwable)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "$logTag: Exception while loading image $url", e)
                imageView.setImageResource(R.drawable.ic_photo_placeholder)
            }
        }
    }

    private companion object {
        const val VIEW_SENT = 1
        const val VIEW_RECEIVED = 2
        private const val TAG = "ChatMessageAdapter"
    }
}
