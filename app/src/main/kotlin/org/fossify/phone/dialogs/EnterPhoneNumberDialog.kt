package org.fossify.phone.dialogs

import android.text.InputType
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.*
import org.fossify.phone.R
import org.fossify.phone.activities.SimpleActivity
import org.fossify.phone.databinding.DialogEnterNumberBinding

class EnterPhoneNumberDialog(val activity: SimpleActivity, titleId: Int, currentValue: String, callback: (value: String) -> Unit) {

    init {
        val binding = DialogEnterNumberBinding.inflate(activity.layoutInflater).apply {
            enterNumberValue.inputType = InputType.TYPE_CLASS_PHONE
            enterNumberValue.setText(currentValue)
            enterNumberValue.selectAll()
        }

        activity.getAlertDialogBuilder().setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null).apply {
            activity.setupDialogStuff(binding.root, this, titleId) { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    callback(binding.enterNumberValue.value.trim())
                    alertDialog.dismiss()
                }
            }
        }
    }
}
