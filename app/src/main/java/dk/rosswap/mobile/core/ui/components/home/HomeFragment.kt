package dk.rosswap.mobile.core.ui.components.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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

        binding.btnSwipe.setOnClickListener {
            Toast.makeText(requireContext(), "Swipe Posts clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Swipe Posts clicked")
            // TODO: navigate to swipe screen
        }

        binding.btnNewPosts.setOnClickListener {
            Toast.makeText(requireContext(), "See New Posts clicked", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "See New Posts clicked")
            // TODO: navigate to new posts screen
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
