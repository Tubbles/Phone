package org.fossify.phone.services

import android.telecom.Call
import android.telecom.CallScreeningService
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.phone.extensions.config
import org.fossify.phone.helpers.BlockedCallNotification
import org.fossify.phone.helpers.CallScreeningRules
import org.fossify.phone.helpers.NO_CALL_SCREENING_RULES
import org.fossify.phone.helpers.ScreeningRule

class SimpleCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        val screeningDecision = screenWithRules(number)
        when {
            number != null && isNumberBlocked(number) -> {
                respondToCall(callDetails, isBlocked = true)
            }

            screeningDecision != null -> {
                if (screeningDecision.rule.block) {
                    BlockedCallNotification.show(this, screeningDecision.canonicalNumber, screeningDecision.rule)
                }

                respondToCall(callDetails, isBlocked = screeningDecision.rule.block)
            }

            number != null && baseConfig.blockUnknownNumbers -> {
                val privateCursor = getMyContactsCursor(favoritesOnly = false, withPhoneNumbersOnly = true)
                val result = SimpleContactsHelper(this).existsSync(number, privateCursor)
                respondToCall(callDetails, isBlocked = result == ContactLookupResult.NotFound)
            }

            number == null && baseConfig.blockHiddenNumbers -> {
                respondToCall(callDetails, isBlocked = true)
            }

            else -> {
                respondToCall(callDetails, isBlocked = false)
            }
        }
    }

    // The rule that decided together with the number it decided on, so the notification can name
    // both without canonicalizing the number a second time.
    private data class RuleDecision(val canonicalNumber: String, val rule: ScreeningRule)

    // Null when there are no rules, no number, or no rule matched. Loading is skipped entirely for
    // the users who never added a rule, so screening stays as cheap as it was before.
    private fun screenWithRules(number: String?): RuleDecision? {
        if (number == null || config.callScreeningRules == NO_CALL_SCREENING_RULES) {
            return null
        }

        val rules = CallScreeningRules.load(config)
        val canonicalNumber = CallScreeningRules.canonicalize(this, number)
        val decidingRule = CallScreeningRules.evaluateWithRule(rules, canonicalNumber) ?: return null
        return RuleDecision(canonicalNumber, decidingRule)
    }

    private fun respondToCall(callDetails: Call.Details, isBlocked: Boolean) {
        val response = CallResponse.Builder()
            .setDisallowCall(isBlocked)
            .setRejectCall(isBlocked)
            .setSkipCallLog(isBlocked)
            .setSkipNotification(isBlocked)
            .build()

        respondToCall(callDetails, response)
    }
}
