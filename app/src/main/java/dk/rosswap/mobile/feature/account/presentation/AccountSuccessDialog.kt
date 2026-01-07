package dk.rosswap.mobile.feature.account.presentation

import android.R
import android.app.Dialog
import android.os.Bundle
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}