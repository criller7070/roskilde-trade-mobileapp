package dk.rosswap.mobile.feature.account.presentation

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.load
import dk.rosswap.mobile.R


class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var avatar: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var cameraButton: ImageView

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

        val user = viewModel.user
        if (user == null) {
            nameText.text = getString(R.string.profile_name_placeholder)
            emailText.text = getString(R.string.profile_email_placeholder)
            return
        }

        nameText.text = user.displayName
            ?: getString(R.string.profile_name_placeholder)

        emailText.text = user.email
            ?: getString(R.string.profile_email_placeholder)

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
    }
}
