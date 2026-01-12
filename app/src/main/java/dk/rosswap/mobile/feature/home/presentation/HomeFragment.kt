package dk.rosswap.mobile.feature.home.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.ui.observeAuthState
import dk.rosswap.mobile.databinding.FragmentHomeBinding
import dk.rosswap.mobile.feature.auth.presentation.AuthViewModel

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe auth state and provide logging and user feedback
        observeAuthState(authViewModel) { state ->
            Log.d(TAG, "Auth State Changed: $state")
            when (state) {
                is AuthState.Loading -> {
                    Log.d(TAG, "🔄 Loading auth state...")
                }
                is AuthState.Authenticated -> {
                    Log.d(TAG, "✅ User authenticated: ${state.user.email}")
                    Log.d(TAG, "   Name: ${state.user.name}")
                    Log.d(TAG, "   Photo: ${state.user.photoURL}")
                }
                is AuthState.Unauthenticated -> {
                    Log.d(TAG, "❌ User not authenticated")
                }
                is AuthState.Error -> {
                    Log.e(TAG, "⚠️ Auth Error: ${state.exception.message}", state.exception)
                }
            }
        }

        binding.btnSwipe.setOnClickListener {
            Toast.makeText(requireContext(), "Swipe Posts clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Swipe Posts clicked")
            // TODO: navigate to swipe screen
            findNavController().navigate(R.id.action_nav_home_to_swipe)

        }

        binding.btnNewPosts.setOnClickListener {
            Toast.makeText(requireContext(), "See New Posts clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "See New Posts clicked")
            // TODO: navigate to new posts screen
            findNavController().navigate(R.id.action_nav_home_to_see_new_posts)
        }

        binding.btnLogin.setOnClickListener {
            Toast.makeText(requireContext(), "Log In clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Log In clicked")
            // TODO: open login fragment / activity
            // navigates using the action defined in `mobile_navigation.xml`
            findNavController().navigate(R.id.action_nav_home_to_loginFragment)
        }

        binding.btnCreatePost.setOnClickListener {
            Toast.makeText(requireContext(), "Create Post clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Create Post clicked")
            // TODO: open create post screen
            findNavController().navigate(R.id.action_nav_home_to_createPost)
        }

        binding.btnCreateAccount.setOnClickListener {
            Toast.makeText(requireContext(), "Create Account clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Create Account clicked")
            // TODO: open create account screen
            findNavController().navigate(R.id.action_nav_home_to_createAccount)
        }
        binding.btnMessages.setOnClickListener {
            Toast.makeText(requireContext(), "Messages clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Messages clicked")
            // navigate to messages screen (ensure action_nav_home_to_messages exists in your nav graph)
            findNavController().navigate(R.id.action_nav_home_to_see_messages)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"

        @JvmStatic
        fun newInstance(): HomeFragment = HomeFragment()
    }
}
