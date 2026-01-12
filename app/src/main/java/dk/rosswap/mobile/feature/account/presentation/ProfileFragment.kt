package dk.rosswap.mobile.feature.account.presentation

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import dk.rosswap.mobile.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var avatar: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var cameraButton: ImageView
    private lateinit var privacyNote: TextView

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.uploadProfilePicture(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        avatar = view.findViewById(R.id.profileAvatar)
        nameText = view.findViewById(R.id.profileName)
        emailText = view.findViewById(R.id.profileEmail)
        cameraButton = view.findViewById(R.id.changeAvatarButton)
        privacyNote = view.findViewById(R.id.tvPrivacyNote)

        val user = viewModel.user
        if (user == null) {
            nameText.text = getString(R.string.profile_name_placeholder)
            emailText.text = getString(R.string.profile_email_placeholder)
        } else {
            nameText.text = user.displayName
                ?: getString(R.string.profile_name_placeholder)

            emailText.text = user.email
                ?: getString(R.string.profile_email_placeholder)
        }

        avatar.contentDescription =
            getString(R.string.profile_picture_description)

        cameraButton.contentDescription =
            getString(R.string.profile_camera_button_desc)

        viewModel.photoUrl.observe(viewLifecycleOwner) { url ->
            avatar.load(url) {
                placeholder(R.drawable.default_pfp)
                error(R.drawable.default_pfp)
            }
        }

        cameraButton.setOnClickListener {
            imagePicker.launch("image/*")
        }

        setupPrivacyPolicyLink()
    }

    private fun setupPrivacyPolicyLink() {
        val fullText = getString(R.string.gdpr_privacy_note)
        val clickableText = "privacy policy"

        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(clickableText)
        if (start == -1) return
        val end = start + clickableText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(
                    R.id.action_profileFragment_to_privacyFragment
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
                ds.color = requireContext().getColor(R.color.brand_orange)
            }
        }

        spannable.setSpan(
            clickableSpan,
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        privacyNote.text = spannable
        privacyNote.movementMethod = LinkMovementMethod.getInstance()
        privacyNote.highlightColor = android.graphics.Color.TRANSPARENT
    }
}
