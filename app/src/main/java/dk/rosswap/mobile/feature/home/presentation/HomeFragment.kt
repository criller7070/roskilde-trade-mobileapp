package dk.rosswap.mobile.feature.home.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.ui.observeAuthState
import dk.rosswap.mobile.databinding.FragmentHomeBinding
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!! // !! = non-null assertion

    @Inject
    lateinit var sessionManager: SessionManager

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

        // observe auth state when created
        observeAuthState(sessionManager.authState) { state ->
            Log.d(TAG, "Auth State Changed: $state")

            // we might as well log here
            when (state) {
                is AuthState.Loading -> {
                    Log.d(TAG, "Loading auth state...")
                }
                is AuthState.Authenticated -> {
                    Log.d(TAG, "User authenticated: ${state.user.email}")
                }
                is AuthState.Unauthenticated -> {
                    Log.d(TAG, "User not authenticated")
                }
                is AuthState.Error -> {
                    Log.e(TAG, "Auth Error: ${state.exception.message}", state.exception)
                }
            }
        }

        // swipe post button
        binding.btnSwipe.setOnClickListener {
            Log.d(TAG, "Swipe Posts clicked")
            findNavController().navigate(R.id.action_nav_home_to_swipe)
        }

        // new posts button
        binding.btnNewPosts.setOnClickListener {
            Toast.makeText(requireContext(), "See New Posts clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "See New Posts clicked")
            findNavController().navigate(R.id.action_nav_home_to_see_new_posts)
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
