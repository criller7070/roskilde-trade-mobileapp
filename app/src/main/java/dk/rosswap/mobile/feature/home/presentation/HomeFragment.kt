package dk.rosswap.mobile.feature.home.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    private val authViewModel: AuthViewModel by activityViewModels()

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
        observeAuthState(authViewModel.authState) { state ->
            Log.d(TAG, "Auth State Changed: $state")

            // Update the login/create-post button depending on auth state
            when (state) {
                is AuthState.Loading -> {
                    Log.d(TAG, "🔄 Loading auth state...")
                    // Treat loading as unauthenticated for UI until we know otherwise
                    binding.btnLogin.setText(R.string.home_btn_login)
                    binding.btnLogin.setOnClickListener {
                        Toast.makeText(requireContext(), "Log In clicked", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Log In clicked (loading)")
                        findNavController().navigate(R.id.action_nav_home_to_loginFragment)
                    }
                }
                is AuthState.Authenticated -> {
                    Log.d(TAG, "✅ User authenticated: ${state.user.email}")
                    Log.d(TAG, "   Name: ${state.user.name}")
                    Log.d(TAG, "   Photo: ${state.user.photoURL}")

                    // Switch button to Create Post and navigate to the Add Item screen
                    binding.btnLogin.setText(R.string.home_btn_create_post)
                    binding.btnLogin.setOnClickListener {
                        Toast.makeText(requireContext(), "Create Post clicked", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Create Post clicked")
                        findNavController().navigate(R.id.action_nav_home_to_createPost)
                    }
                }
                is AuthState.Unauthenticated -> {
                    Log.d(TAG, "❌ User not authenticated")
                    binding.btnLogin.setText(R.string.home_btn_login)
                    binding.btnLogin.setOnClickListener {
                        Toast.makeText(requireContext(), "Log In clicked", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Log In clicked")
                        findNavController().navigate(R.id.action_nav_home_to_loginFragment)
                    }
                }
                is AuthState.Error -> {
                    Log.e(TAG, "⚠️ Auth Error: ${state.exception.message}", state.exception)
                    // On error, fall back to login action so user can try again
                    binding.btnLogin.setText(R.string.home_btn_login)
                    binding.btnLogin.setOnClickListener {
                        Toast.makeText(requireContext(), "Log In clicked", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Log In clicked (error)")
                        findNavController().navigate(R.id.action_nav_home_to_loginFragment)
                    }
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

        // Note: btnLogin click listener is set dynamically in the auth state observer above
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
