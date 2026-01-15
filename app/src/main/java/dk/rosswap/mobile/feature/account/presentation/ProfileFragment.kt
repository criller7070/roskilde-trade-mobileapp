package dk.rosswap.mobile.feature.account.presentation

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseAuth
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.account.domain.UserPost
import dk.rosswap.mobile.feature.account.presentation.adapter.ProfilePostsAdapter
import java.io.File

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var avatar: ImageView
    private lateinit var nameText: TextView
    private lateinit var emailText: TextView
    private lateinit var cameraButton: ImageView
    private lateinit var privacyNote: TextView
    private lateinit var downloadDataButton: Button

    // ---------------- YOUR POSTS ----------------
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var emptyPostsText: TextView
    private lateinit var postsAdapter: ProfilePostsAdapter

    // Holds the exported file until user chooses save location
    private var pendingExportFile: File? = null

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.uploadProfilePicture(it) }
        }

    private val saveFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            val file = pendingExportFile ?: return@registerForActivityResult

            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                }
                Toast.makeText(requireContext(), "Data downloaded successfully", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to save file", Toast.LENGTH_LONG).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        avatar = view.findViewById(R.id.profileAvatar)
        nameText = view.findViewById(R.id.profileName)
        emailText = view.findViewById(R.id.profileEmail)
        cameraButton = view.findViewById(R.id.changeAvatarButton)
        privacyNote = view.findViewById(R.id.tvPrivacyNote)
        downloadDataButton = view.findViewById(R.id.btnDownloadData)

        val deleteButton = view.findViewById<View>(R.id.btnDeleteAccount)

        val user = viewModel.user
        nameText.text = user?.displayName ?: getString(R.string.profile_name_placeholder)
        emailText.text = user?.email ?: getString(R.string.profile_email_placeholder)

        viewModel.photoUrl.observe(viewLifecycleOwner) { url ->
            avatar.load(url) {
                placeholder(R.drawable.default_pfp)
                error(R.drawable.default_pfp)
            }
        }

        cameraButton.setOnClickListener {
            imagePicker.launch("image/*")
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        setupPrivacyPolicyLink()
        setupGdprDownload()

        // ---------------- YOUR POSTS SETUP ----------------

        postsRecyclerView = view.findViewById(R.id.rvPosts)
        emptyPostsText = view.findViewById(R.id.tvNoPosts)
        val addPostButton = view.findViewById<ImageView>(R.id.btnAddPost)

        postsAdapter = ProfilePostsAdapter(
            onClick = { post ->
                findNavController()
                    .navigate(
                        R.id.action_profileFragment_to_addItem,
                        Bundle().apply {
                            putString("itemId", post.id)
                        }
                    )
            },
            onDelete = { post ->
                showDeletePostDialog(post)
            }
        )

        postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postsAdapter.submitList(posts)
            emptyPostsText.visibility =
                if (posts.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loadUserPosts()

        addPostButton.setOnClickListener {
            findNavController()
                .navigate(R.id.action_profileFragment_to_addItem)
        }
    }

    // ---------------- GDPR DOWNLOAD ----------------

    private fun setupGdprDownload() {
        downloadDataButton.isEnabled = true

        downloadDataButton.setOnClickListener {
            downloadDataButton.isEnabled = false

            viewModel.exportUserData(
                requireContext(),
                onSuccess = { file ->
                    downloadDataButton.isEnabled = true
                    pendingExportFile = file
                    saveFileLauncher.launch(file.name)
                },
                onError = {
                    downloadDataButton.isEnabled = true
                    Toast.makeText(requireContext(), "Failed to export your data", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ---------------- PRIVACY POLICY LINK ----------------

    private fun setupPrivacyPolicyLink() {
        val fullText = getString(R.string.gdpr_privacy_note)
        val clickableText = "privacy policy"

        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(clickableText)
        if (start == -1) return
        val end = start + clickableText.length

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController()
                    .navigate(R.id.action_profileFragment_to_privacyFragment)
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

    // ---------------- DELETE ACCOUNT ----------------

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_account_title)
            .setMessage(R.string.delete_account_confirmation)
            .setPositiveButton(R.string.delete_account_confirm) { _, _ ->
                performDeleteAccount()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun performDeleteAccount() {
        viewModel.deleteAccount(
            onSuccess = {
                findNavController()
                    .navigate(R.id.action_profileFragment_to_login)
            },
            onReauthRequired = {
                showReauthenticationRequired()
            },
            onError = { error ->
                Toast.makeText(
                    requireContext(),
                    error.localizedMessage
                        ?: getString(R.string.profile_delete_account_failed),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun showReauthenticationRequired() {
        AlertDialog.Builder(requireContext())
            .setTitle("Re-authentication required")
            .setMessage("Please log in again to delete your account.")
            .setPositiveButton("Log out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                findNavController()
                    .navigate(R.id.action_profileFragment_to_login)
            }
            .show()
    }

    // ---------------- DELETE POST ----------------
    private fun showDeletePostDialog(post: UserPost) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_post_title)
            .setMessage(R.string.delete_post_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteUserPost(post.id)
            }
            .setNegativeButton(R.string.profilecancel, null)
            .show()
    }



}
