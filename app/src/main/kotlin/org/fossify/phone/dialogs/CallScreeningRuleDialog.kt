package org.fossify.phone.dialogs

import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.beGoneIf
import org.fossify.commons.extensions.getAlertDialogBuilder
import org.fossify.commons.extensions.onTextChangeListener
import org.fossify.commons.extensions.setupDialogStuff
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.value
import org.fossify.phone.R
import org.fossify.phone.activities.SimpleActivity
import org.fossify.phone.databinding.DialogCallScreeningRuleBinding
import org.fossify.phone.helpers.CallScreeningRules
import org.fossify.phone.helpers.ScreeningRule

class CallScreeningRuleDialog(
    private val activity: SimpleActivity,
    private val existing: ScreeningRule?,
    private val callback: (rule: ScreeningRule) -> Unit
) {
    private val binding = DialogCallScreeningRuleBinding.inflate(activity.layoutInflater)

    init {
        binding.apply {
            callScreeningRuleBlock.isChecked = existing?.block != false
            callScreeningRuleAllow.isChecked = existing?.block == false
            callScreeningRulePattern.setText(existing?.pattern.orEmpty())
            callScreeningRulePattern.onTextChangeListener { updateTestResult() }
            callScreeningRuleTestNumber.onTextChangeListener { updateTestResult() }
        }

        updateTestResult()

        val titleId = if (existing == null) R.string.call_screening_add_rule else R.string.edit
        activity.getAlertDialogBuilder().setPositiveButton(R.string.ok, null).setNegativeButton(R.string.cancel, null).apply {
            activity.setupDialogStuff(binding.root, this, titleId) { alertDialog ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    confirmRule(alertDialog)
                }
            }
        }
    }

    private fun confirmRule(alertDialog: AlertDialog) {
        val pattern = binding.callScreeningRulePattern.value.trim()
        val patternError = CallScreeningRules.isValidPattern(pattern)
        if (patternError != null) {
            activity.toast(patternError)
            return
        }

        callback(ScreeningRule(block = binding.callScreeningRuleBlock.isChecked, pattern = pattern))
        alertDialog.dismiss()
    }

    private fun updateTestResult() {
        binding.apply {
            val pattern = callScreeningRulePattern.value.trim()
            val patternError = CallScreeningRules.isValidPattern(pattern)
            val testNumber = callScreeningRuleTestNumber.value.trim()

            callScreeningRuleTestResult.beGoneIf(patternError == null && testNumber.isEmpty())
            callScreeningRuleTestResult.text = when {
                patternError != null -> patternError
                testNumber.isEmpty() -> ""
                else -> describeMatch(pattern, testNumber)
            }
        }
    }

    // The test number goes through the very same canonicalization the screening service uses, so
    // the user sees what the rule is actually matched against.
    private fun describeMatch(pattern: String, testNumber: String): String {
        val canonicalNumber = CallScreeningRules.canonicalize(activity, testNumber)
        val matches = CallScreeningRules.evaluate(listOf(ScreeningRule(block = true, pattern = pattern)), canonicalNumber) == true
        val resultId = if (matches) R.string.call_screening_test_matches else R.string.call_screening_test_no_match
        return activity.getString(resultId, canonicalNumber)
    }
}
