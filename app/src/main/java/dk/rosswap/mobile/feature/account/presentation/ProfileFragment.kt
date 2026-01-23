package dk.rosswap.mobile.feature.account.presentation

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val viewModel: ProfileViewModel by viewModels()
    @javax.inject.Inject
    lateinit var sessionManager: SessionManager

    // views. IDE complained if it wasn't lateint
    private lateinit var avatar: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var cameraButton: ImageView
    private lateinit var privacyNote: TextView
    private lateinit var adapter: ProfilePostsAdapter
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.uploadProfilePicture(it) }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // get info
        avatar = view.findViewById(R.id.profileAvatar)
        nameText = view.findViewById(R.id.profileName)
        emailText = view.findViewById(R.id.profileEmail)
        cameraButton = view.findViewById(R.id.changeAvatarButton)
        privacyNote = view.findViewById(R.id.tvPrivacyNote)

        // get components
        val deleteButton = view.findViewById<View>(R.id.btnDeleteAccount)
        val rvPosts = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPosts)
        val tvNoPosts = view.findViewById<TextView>(R.id.tvNoPosts)
        val btnAddPost = view.findViewById<View>(R.id.btnAddPost)
        val btnDownloadData = view.findViewById<View>(R.id.btnDownloadData)

        // setup recyclerview Adapter
        adapter = ProfilePostsAdapter(
            onClick = { item ->
                // navigate to item detail on click
                val args = Bundle().apply {
                    putString("itemId", item.id)
                    putString("itemTitle", item.title)
                    putString("itemDescription", item.description)
                    putString("itemImage", item.imageUrl)
                    putString("itemUserId", "")
                    putString("itemUserName", "")
                }
                findNavController().navigate(R.id.action_profileFragment_to_itemDetail, args)
            },
            onDelete = { item ->
                // show confirmation and call viewModel to delete
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.delete_post_title)
                    .setMessage(R.string.delete_post_message)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        viewModel.deletePost(item.id,
                            onSuccess = {
                                Toast.makeText(requireContext(), getString(R.string.delete_post_success), Toast.LENGTH_SHORT).show()
                            },
                            onError = { e ->
                                Toast.makeText(requireContext(), e.localizedMessage ?: getString(R.string.delete_post_failed), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
        rvPosts.layoutManager = LinearLayoutManager(requireContext())
        rvPosts.adapter = adapter

        // populate views
        val user = viewModel.user
        if (user == null) {
            nameText.text = getString(R.string.profile_name_placeholder)
            emailText.text = getString(R.string.profile_email_placeholder)
        } else {
            val displayName = user.name.takeIf { it.isNotBlank() } ?: getString(R.string.profile_name_placeholder)
            nameText.text = displayName

            val email = user.email.takeIf { it.isNotBlank() } ?: getString(R.string.profile_email_placeholder)
            emailText.text = email
        }

        lifecycleScope.launch {
            sessionManager.authState.collect { state ->
                when (state) {
                    is dk.rosswap.mobile.core.common.AuthState.Authenticated -> {
                        val u = state.user
                        val displayName = u.name.takeIf { it.isNotBlank() } ?: getString(R.string.profile_name_placeholder)
                        nameText.text = displayName
                        val email = u.email.takeIf { it.isNotBlank() } ?: getString(R.string.profile_email_placeholder)
                        emailText.text = email

                        // enable GDPR download for authenticated user
                        btnDownloadData.isEnabled = true
                        btnDownloadData.isClickable = true
                    }
                    is dk.rosswap.mobile.core.common.AuthState.Unauthenticated -> {
                        nameText.text = getString(R.string.profile_name_placeholder)
                        emailText.text = getString(R.string.profile_email_placeholder)

                        // disable GDPR download when signed out
                        btnDownloadData.isEnabled = false
                        btnDownloadData.isClickable = false
                    }
                    else -> {
                        // loading/error - keep current values
                    }
                }
            }
        }

        // set content descriptions
        avatar.contentDescription = getString(R.string.profile_picture_description)
        cameraButton.contentDescription = getString(R.string.profile_camera_button_desc)

        // load profile picture
        viewModel.photoUrl.observe(viewLifecycleOwner) { url ->
            avatar.load(url) {
                placeholder(R.drawable.default_pfp)
                error(R.drawable.default_pfp)
            }
        }

        // observe upload state and errors
        viewModel.isUploadingProfilePicture.observe(viewLifecycleOwner) { isUploading ->
            cameraButton.isEnabled = !isUploading
            cameraButton.alpha = if (isUploading) 0.5f else 1f
        }

        viewModel.uploadError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(
                    requireContext(),
                    "Failed to upload profile picture: $error",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // observe posts and update UI if exists
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            tvNoPosts.visibility = if (posts.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        // observe exporting state to optionally disable UI
        viewModel.isExporting.observe(viewLifecycleOwner) { exporting ->
            // You might disable the button while exporting
            btnDownloadData.isEnabled = !exporting
        }

        // click listeners
        cameraButton.setOnClickListener { imagePicker.launch("image/*") }
        btnAddPost.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_addItem) }
        deleteButton.setOnClickListener { showDeleteConfirmation() }

        btnDownloadData.setOnClickListener { showExportConfirmation() }
        setupPrivacyPolicyLink()
    }

    private fun setupPrivacyPolicyLink() {
        val fullText = getString(R.string.gdpr_privacy_note)
        val clickableText = "privacy policy"

        // span
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(clickableText)
        if (start == -1) return
        val end = start + clickableText.length
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_profileFragment_to_privacyFragment)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
                ds.color = requireContext().getColor(R.color.brand_orange)
            }
        }

        spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        privacyNote.text = spannable
        privacyNote.movementMethod = LinkMovementMethod.getInstance()
        privacyNote.highlightColor = android.graphics.Color.TRANSPARENT
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_account_title)
            .setMessage(R.string.delete_account_confirmation)
            .setPositiveButton(R.string.delete_account_confirm) { _, _ -> performDeleteAccount() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun performDeleteAccount() {
        viewModel.deleteAccount(
            onSuccess = {
                // Only navigate if the fragment is still added and the current destination
                // is the profile fragment. This prevents crashes when the ViewModel
                // completes after the fragment view has been destroyed.
                if (isAdded) {
                    try {
                        safeNavigateAction(R.id.action_profileFragment_to_login)
                    } catch (e: Exception) {
                        android.util.Log.w("ProfileFragment", "Navigation to login failed: ${e.message}")
                    }
                }
            },
            onReauthRequired = { showReauthenticationRequired() },
            onError = { error ->
                Toast.makeText(requireContext(), error.localizedMessage ?: getString(R.string.profile_delete_account_failed), Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun showReauthenticationRequired() {
        // Show a dialog but guard navigation since auth change may happen in background
        AlertDialog.Builder(requireContext())
            .setTitle("Re-authentication required")
            .setMessage("Please log in again to delete your account.")
            .setPositiveButton("Log out") { _, _ ->
                // Perform sign-out and navigate safely to login only if fragment is still attached
                if (!isAdded) return@setPositiveButton
                try {
                    sessionManager.signOut()
                } catch (e: Exception) {
                    android.util.Log.w("ProfileFragment", "Sign out failed: ${e.message}")
                }

                if (isAdded) {
                    try {
                        safeNavigateAction(R.id.action_profileFragment_to_login)
                    } catch (e: Exception) {
                        android.util.Log.w("ProfileFragment", "Navigation to login failed after sign-out: ${e.message}")
                    }
                }
            }
            .show()
    }
    private fun safeNavigateAction(actionId: Int) {
        if (!isAdded) return
        try {
            val navController = findNavController()
            val dest = navController.currentDestination
            if (dest != null && dest.getAction(actionId) != null) {
                navController.navigate(actionId)
            } else {
                // fallback: try to navigate to the login destination id directly
                navController.popBackStack(R.id.nav_login, false)
            }
        } catch (e: Exception) {
            android.util.Log.w("ProfileFragment", "safeNavigateAction failed: ${e.message}")
        }
    }

    private fun showExportConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.gdpr_download_title)
            .setMessage(R.string.gdpr_download_description)
            .setPositiveButton(R.string.gdpr_download_button) { _, _ -> performExportData() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun performExportData() {
        val currentUser = viewModel.user ?: run {
            Toast.makeText(requireContext(), getString(R.string.profile_not_signed_in), Toast.LENGTH_LONG).show()
            return
        }

        // simple progress dialog
        val progress = AlertDialog.Builder(requireContext())
            .setTitle(R.string.gdpr_download_title)
            .setMessage(getString(R.string.gdpr_download_in_progress))
            .setCancelable(false)
            .create()
        progress.show()

        viewModel.exportAccount(currentUser.uid) { result ->
            progress.dismiss()

            result.fold(
                onSuccess = { path ->
                    try {
                        val file = File(path)
                        val authority = requireContext().packageName + ".fileprovider"
                        val uri: Uri = FileProvider.getUriForFile(requireContext(), authority, file)

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        val chooser = Intent.createChooser(shareIntent, getString(R.string.gdpr_share_chooser_title))
                        startActivity(chooser)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), e.localizedMessage ?: getString(R.string.gdpr_download_failed), Toast.LENGTH_LONG).show()
                    }
                },
                onFailure = { e ->
                    Toast.makeText(requireContext(), e.localizedMessage ?: getString(R.string.gdpr_download_failed), Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}
