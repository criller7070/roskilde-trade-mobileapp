package dk.rosswap.mobile.feature.items.presentation

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth

@AndroidEntryPoint
class ItemPageFragment : Fragment() {
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var chatRepository: ChatRepository
    private val viewModel: ItemPageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_detail, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
        val parcelableItem = args.getParcelable("item", dk.rosswap.mobile.core.model.Item::class.java)
        val itemId = parcelableItem?.id ?: args.getString("itemId").orEmpty()
        val itemTitle = parcelableItem?.title ?: args.getString("itemTitle").orEmpty()
        val itemDescription = parcelableItem?.description ?: args.getString("itemDescription").orEmpty()
        val itemImage = parcelableItem?.imageUrl ?: args.getString("itemImage").orEmpty()
        val itemUserId = parcelableItem?.userId ?: args.getString("itemUserId").orEmpty()
        val itemUserName = parcelableItem?.userName ?: args.getString("itemUserName").orEmpty()

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

        // observe author avatar from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authorPhotoUrl
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collectLatest { photoUrl ->
                    if (!photoUrl.isNullOrBlank()) {
                        authorAvatarIv.load(photoUrl) {
                            placeholder(R.drawable.default_pfp)
                            error(R.drawable.default_pfp)
                        }
                    } else {
                        loadDefaultAvatar(authorAvatarIv)
                    }
                }
        }

        if (itemUserId.isNotBlank()) {
            viewModel.loadAuthorAvatar(itemUserId)
        } else {
            loadDefaultAvatar(authorAvatarIv)
        }

        // toggle UI based on login state: show message button for logged-in users; show hint otherwise.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            messageBtn.visibility = View.GONE
            loginHint.visibility = View.VISIBLE
            loginHint.text = getString(R.string.login_required_hint)
        } else {
            messageBtn.visibility = View.VISIBLE
            loginHint.visibility = View.GONE

            // attach click listener only for logged-in users; currentUser is non-null here.
            messageBtn.setOnClickListener {
                if (itemUserId.isBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch { dk.rosswap.mobile.core.ui.components.popup.PopupBus.showError("Missing item owner") }
                    return@setOnClickListener
                }

                val currentUserId = currentUser.uid
                if (currentUserId == itemUserId) {
                    viewLifecycleOwner.lifecycleScope.launch { dk.rosswap.mobile.core.ui.components.popup.PopupBus.showError("You can’t message yourself") }
                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
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

        // short button label to avoid wrapping; subtitle keeps the full text
        messageBtn.text = getString(R.string.message_button)
    }

    private fun loadDefaultAvatar(imageView: ImageView) {
        imageView.setImageResource(R.drawable.default_pfp)
    }
}
