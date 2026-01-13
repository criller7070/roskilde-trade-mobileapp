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
                Log.d(TAG, "SentVH: Loading image raw=$raw")
                binding.tvMessage.visibility = View.GONE
                binding.ivMessage.visibility = View.VISIBLE

                // Immediate placeholder + debug tag
                binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                binding.ivMessage.tag = raw

                binding.ivMessage.contentDescription = msg.text?.takeIf { it.isNotBlank() } ?: binding.root.context.getString(R.string.image_message_content_desc)

                try {
                    if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("//")) {
                        // HTTP(s) or protocol-relative -> normalize and load
                        val normalized = if (raw.startsWith("//")) "https:$raw" else raw
                        loadImageWithCoil(binding.ivMessage, normalized)
                    } else {
                        // Treat as Firebase Storage path or gs:// URL; resolve to downloadUrl; on failure try getBytes fallback
                        val storage = FirebaseStorage.getInstance()
                        val ref = if (raw.startsWith("gs://")) storage.getReferenceFromUrl(raw) else storage.reference.child(raw)
                        ref.downloadUrl
                            .addOnSuccessListener { uri ->
                                Log.d(TAG, "SentVH: Resolved storage url $raw -> ${uri}")
                                loadImageWithCoil(binding.ivMessage, uri.toString())
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "SentVH: Failed to resolve storage url $raw, trying byte download", e)
                                // Try to fetch bytes directly as fallback
                                val maxBytes: Long = 2L * 1024L * 1024L // 2MB
                                ref.getBytes(maxBytes)
                                    .addOnSuccessListener { bytes ->
                                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        if (bmp != null) {
                                            binding.ivMessage.setImageBitmap(bmp)
                                        } else {
                                            binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                                        }
                                    }
                                    .addOnFailureListener { e2 ->
                                        Log.e(TAG, "SentVH: getBytes fallback failed for $raw", e2)
                                        binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                                    }
                            }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "SentVH: Exception while loading image $raw", e)
                    binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                }

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

        companion object {
            private fun loadImageWithCoil(iv: ImageView, url: String) {
                try {
                    iv.load(url) {
                        crossfade(true)
                        placeholder(R.drawable.ic_photo_placeholder)
                        error(R.drawable.ic_photo_placeholder)
                        listener(
                            onSuccess = { _: ImageRequest, result: SuccessResult ->
                                val drawable = result.drawable
                                if (drawable == null) {
                                    Log.w(TAG, "SentVH: Image loaded but drawable is null for $url")
                                    iv.setImageResource(R.drawable.ic_photo_placeholder)
                                } else {
                                    Log.d(TAG, "SentVH: Image loaded successfully for $url")
                                }
                            },
                            onError = { _: ImageRequest, result: ErrorResult ->
                                Log.e(TAG, "SentVH: Image load failed for $url: ${result.throwable?.message}", result.throwable)
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "SentVH: Exception while loading image $url", e)
                    iv.setImageResource(R.drawable.ic_photo_placeholder)
                }
            }
        }
    }

    class ReceivedVH(private val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            val raw = msg.imageUrl?.trim().orEmpty()

            if (raw.isNotBlank()) {
                Log.d(TAG, "ReceivedVH: Loading image raw=$raw")
                binding.tvMessage.visibility = View.GONE
                binding.ivMessage.visibility = View.VISIBLE

                // placeholder
                binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)

                binding.ivMessage.contentDescription = msg.text?.takeIf { it.isNotBlank() } ?: binding.root.context.getString(R.string.image_message_content_desc)
                try {
                    if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("//")) {
                        val normalized = if (raw.startsWith("//")) "https:$raw" else raw
                        loadImageWithCoil(binding.ivMessage, normalized)
                    } else {
                        val storage = FirebaseStorage.getInstance()
                        val ref = if (raw.startsWith("gs://")) storage.getReferenceFromUrl(raw) else storage.reference.child(raw)
                        ref.downloadUrl
                            .addOnSuccessListener { uri ->
                                Log.d(TAG, "ReceivedVH: Resolved storage url $raw -> ${uri}")
                                loadImageWithCoil(binding.ivMessage, uri.toString())
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "ReceivedVH: Failed to resolve storage url $raw, trying byte download", e)
                                val maxBytes: Long = 2L * 1024L * 1024L
                                ref.getBytes(maxBytes)
                                    .addOnSuccessListener { bytes ->
                                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        if (bmp != null) {
                                            binding.ivMessage.setImageBitmap(bmp)
                                        } else {
                                            binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                                        }
                                    }
                                    .addOnFailureListener { e2 ->
                                        Log.e(TAG, "ReceivedVH: getBytes fallback failed for $raw", e2)
                                        binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                                    }
                            }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "ReceivedVH: Exception while loading image $raw", e)
                    binding.ivMessage.setImageResource(R.drawable.ic_photo_placeholder)
                }

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

        companion object {
            private fun loadImageWithCoil(iv: ImageView, url: String) {
                try {
                    iv.load(url) {
                        crossfade(true)
                        placeholder(R.drawable.ic_photo_placeholder)
                        error(R.drawable.ic_photo_placeholder)
                        listener(
                            onSuccess = { _: ImageRequest, result: SuccessResult ->
                                val drawable = result.drawable
                                if (drawable == null) {
                                    Log.w(TAG, "ReceivedVH: Image loaded but drawable is null for $url")
                                    iv.setImageResource(R.drawable.ic_photo_placeholder)
                                } else {
                                    Log.d(TAG, "ReceivedVH: Image loaded successfully for $url")
                                }
                            },
                            onError = { request: ImageRequest, result: ErrorResult ->
                                Log.e(TAG, "ReceivedVH: Image load failed for $url: ${result.throwable?.message}", result.throwable)
                            }
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "ReceivedVH: Exception while loading image $url", e)
                    iv.setImageResource(R.drawable.ic_photo_placeholder)
                }
            }
        }
    }

    private companion object {
        const val VIEW_SENT = 1
        const val VIEW_RECEIVED = 2
        private const val TAG = "ChatMessageAdapter"
    }
}
