package org.fossify.phone.services

import android.telecom.Call
import android.telecom.CallScreeningService
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.extensions.getMyContactsCursor
import org.fossify.commons.extensions.isNumberBlocked
import org.fossify.commons.helpers.ContactLookupResult
import org.fossify.commons.helpers.SimpleContactsHelper
import org.fossify.phone.extensions.config
import org.fossify.phone.helpers.CallScreeningRules
import org.fossify.phone.helpers.NO_CALL_SCREENING_RULES

class SimpleCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        val screeningDecision = screenWithRules(number)
        when {
            number != null && isNumberBlocked(number) -> {
                respondToCall(callDetails, isBlocked = true)
            }

            screeningDecision != null -> {
                respondToCall(callDetails, isBlocked = screeningDecision)
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

    // Null when there are no rules, no number, or no rule matched. Loading is skipped entirely for
    // the users who never added a rule, so screening stays as cheap as it was before.
    private fun screenWithRules(number: String?): Boolean? {
        if (number == null || config.callScreeningRules == NO_CALL_SCREENING_RULES) {
            return null
        }

        val rules = CallScreeningRules.load(config)
        return CallScreeningRules.evaluate(rules, CallScreeningRules.canonicalize(this, number))
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
