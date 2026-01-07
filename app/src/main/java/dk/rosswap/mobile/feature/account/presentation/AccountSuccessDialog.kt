package dk.rosswap.mobile.core.ui.components.account

import android.R
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import dk.rosswap.mobile.databinding.DialogAccountSuccessBinding

open class AccountSuccessDialog(
    private val onDismiss: () -> Unit = {}
) : DialogFragment() {

    private var _binding: DialogAccountSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAccountSuccessBinding.inflate(layoutInflater)

        binding.btnOk.setOnClickListener {
            dismiss()
            onDismiss()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
            onDismiss()
        }

        return Dialog(requireContext()).apply {
            setContentView(binding.root)
            window?.setBackgroundDrawableResource(R.color.transparent)
            setCancelable(false)
        }
    }

    // 🔥 THIS IS THE IMPORTANT PART (size fix)
    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

