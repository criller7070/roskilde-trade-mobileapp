package dk.rosswap.mobile.feature.items.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class ItemDetailFragment : Fragment() {

    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var chatRepository: ChatRepository
    @Inject lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTv = view.findViewById<TextView>(R.id.tv_item_title)
        val imageIv = view.findViewById<ImageView>(R.id.iv_item_image)
        val descTv = view.findViewById<TextView>(R.id.tv_item_description)
        val authorNameTv = view.findViewById<TextView>(R.id.tv_author_name)
        val authorSubTv = view.findViewById<TextView>(R.id.tv_author_sub)
        val authorAvatarIv = view.findViewById<ImageView>(R.id.iv_author_avatar)
        val messageBtn = view.findViewById<Button>(R.id.btn_message_author)
        val loginHint = view.findViewById<TextView>(R.id.tv_login_required_hint)

        val args = requireArguments()
        val itemId = args.getString("itemId").orEmpty()
        val itemTitle = args.getString("itemTitle").orEmpty()
        val itemDescription = args.getString("itemDescription").orEmpty()
        val itemImage = args.getString("itemImage").orEmpty()
        val itemUserId = args.getString("itemUserId").orEmpty()
        val itemUserName = args.getString("itemUserName").orEmpty()

        titleTv.text = itemTitle
        descTv.text = itemDescription
        authorNameTv.text = itemUserName.ifBlank { itemUserId }
        authorSubTv.text = getString(R.string.write_to_user, itemUserName.ifBlank { itemUserId })

        if (itemImage.isBlank()) {
            imageIv.setImageResource(R.drawable.loading2)
        } else {
            imageIv.load(itemImage) {
                placeholder(R.drawable.loading2)
                error(R.drawable.loading2)
            }
        }

        // Load author avatar from Firestore
        if (itemUserId.isNotBlank()) {
            lifecycleScope.launch {
                try {
                    val document = firestore.collection("users").document(itemUserId).get().await()
                    val photoUrl = document.getString("photoURL")
                    if (!photoUrl.isNullOrBlank()) {
                        authorAvatarIv.load(photoUrl) {
                            placeholder(R.drawable.default_pfp)
                            error(R.drawable.default_pfp)
                        }
                    } else {
                        loadDefaultAvatar(authorAvatarIv)
                    }
                } catch (e: Exception) {
                    loadDefaultAvatar(authorAvatarIv)
                }
            }
        } else {
            loadDefaultAvatar(authorAvatarIv)
        }

        // Toggle UI based on login state: show message button for logged-in users; show hint otherwise.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            messageBtn.visibility = View.GONE
            loginHint.visibility = View.VISIBLE
            loginHint.text = getString(R.string.login_required_hint)
        } else {
            messageBtn.visibility = View.VISIBLE
            loginHint.visibility = View.GONE
        }

        // Short button label to avoid wrapping; subtitle keeps the full text
        messageBtn.text = getString(R.string.message_button)

        messageBtn.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // Let hosting fragment handle login popup ideally; fallback to a toast
                lifecycleScope.launch { dk.rosswap.mobile.core.ui.components.popup.PopupBus.showError("You must be logged in to message") }
                return@setOnClickListener
            }

            if (itemUserId.isBlank()) {
                lifecycleScope.launch { dk.rosswap.mobile.core.ui.components.popup.PopupBus.showError("Missing item owner") }
                return@setOnClickListener
            }

            val currentUserId = currentUser.uid
            if (currentUserId == itemUserId) {
                lifecycleScope.launch { dk.rosswap.mobile.core.ui.components.popup.PopupBus.showError("You can’t message yourself") }
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = chatRepository.openChat(
                    currentUserId = currentUserId,
                    otherUserId = itemUserId,
                    itemId = itemId,
                    itemName = itemTitle,
                    itemImage = itemImage,
                    currentUserName = currentUser.displayName?.trim().orEmpty(),
                    otherUserName = itemUserName
                )

                result.fold(
                    onSuccess = { chatId ->
                        findNavController().navigate(
                            R.id.nav_chatconvo, Bundle().apply {
                                putString("chatId", chatId)
                                putString("itemName", itemTitle)
                                putString("itemImage", itemImage)
                            }
                        )
                    },
                    onFailure = { e ->
                        dk.rosswap.mobile.core.ui.components.popup.PopupBus.showError(e.message ?: "Could not start chat")
                    }
                )
            }
        }
    }

    private fun loadDefaultAvatar(imageView: ImageView) {
        imageView.setImageResource(R.drawable.default_pfp)
    }
}
