package org.fossify.phone.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import org.fossify.commons.extensions.toast
import org.fossify.commons.extensions.updateTextColors
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.commons.helpers.REQUEST_CODE_SET_DEFAULT_CALLER_ID
import org.fossify.commons.helpers.isQPlus
import org.fossify.phone.R
import org.fossify.phone.adapters.CallScreeningRulesAdapter
import org.fossify.phone.databinding.ActivityManageCallScreeningRulesBinding
import org.fossify.phone.dialogs.CallScreeningRuleDialog
import org.fossify.phone.extensions.config
import org.fossify.phone.helpers.CallScreeningRules
import org.fossify.phone.helpers.ScreeningRule

class ManageCallScreeningRulesActivity : SimpleActivity() {
    private val binding by viewBinding(ActivityManageCallScreeningRulesBinding::inflate)

    private val rules = mutableListOf<ScreeningRule>()
    private var askedToBecomeCallerIdApp = false

    // One adapter for the whole activity: it owns an ItemTouchHelper attached to the list, and a
    // replacement adapter would leave the previous one attached as well.
    private lateinit var rulesAdapter: CallScreeningRulesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            setupEdgeToEdge(padBottomSystem = listOf(callScreeningRulesList))
            setupMaterialScrollListener(callScreeningRulesList, callScreeningRulesAppbar)
            callScreeningRulesAdd.setOnClickListener { showRuleDialog(null) }
        }

        rules.addAll(CallScreeningRules.load(config))
        rulesAdapter = CallScreeningRulesAdapter(this, rules, binding.callScreeningRulesList, ::saveRules) { clickedRule ->
            showRuleDialog(clickedRule as ScreeningRule)
        }
        binding.callScreeningRulesList.adapter = rulesAdapter
        requestCallerIdRoleIfNeeded()

        updateTextColors(binding.callScreeningRulesHolder)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.callScreeningRulesAppbar, NavigationIcon.Arrow)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        // the rules stay either way, they just do nothing until Android grants the screening role
        if (requestCode == REQUEST_CODE_SET_DEFAULT_CALLER_ID && resultCode != Activity.RESULT_OK) {
            toast(R.string.must_make_default_caller_id_app, length = Toast.LENGTH_LONG)
        }
    }

    private fun showRuleDialog(existingRule: ScreeningRule?) {
        CallScreeningRuleDialog(this, existingRule) { editedRule ->
            // identity, not equality: two rules may well carry the same action and pattern
            val existingPosition = rules.indexOfFirst { it === existingRule }
            if (existingPosition == -1) {
                rules.add(editedRule)
            } else {
                rules[existingPosition] = editedRule
            }

            saveRules()
            rulesAdapter.refreshRules()
        }
    }

    private fun saveRules() {
        CallScreeningRules.save(config, rules)
        requestCallerIdRoleIfNeeded()
    }

    private fun requestCallerIdRoleIfNeeded() {
        if (rules.isEmpty() || askedToBecomeCallerIdApp || !isQPlus()) {
            return
        }

        askedToBecomeCallerIdApp = true
        setDefaultCallerIdApp()
    }
}
