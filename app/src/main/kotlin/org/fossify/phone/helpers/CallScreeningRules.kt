package org.fossify.phone.helpers

import android.content.Context
import android.net.Uri
import android.telephony.PhoneNumberUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.fossify.phone.extensions.config

data class ScreeningRule(val block: Boolean, val pattern: String)

/**
 * Regular expression screening of incoming calls.
 *
 * Every rule is matched against the whole caller number in its canonical E.164 form (+46701234567),
 * and the last rule that matches decides, so a broad rule can be narrowed by a later one. A number
 * no rule matches gets no verdict at all and is left to the rest of the screening service.
 *
 * See doc/DESIGN-call-screening-rules.md in the pinetime-hacks repository.
 */
object CallScreeningRules {
    fun load(config: Config): MutableList<ScreeningRule> {
        val ruleListType = object : TypeToken<List<ScreeningRule>>() {}.type
        return Gson().fromJson<MutableList<ScreeningRule>>(config.callScreeningRules, ruleListType) ?: mutableListOf()
    }

    fun save(config: Config, rules: List<ScreeningRule>) {
        config.callScreeningRules = Gson().toJson(rules)
    }

    /**
     * The form every rule is matched against. Percent escapes and a "tel:" scheme are stripped the
     * same way the custom SIM lookup in Config does it, so a handle like "tel:%2B46701234567"
     * still reaches the rules as "+46701234567".
     */
    fun canonicalize(context: Context, number: String): String {
        val decoded = Uri.decode(number).removePrefix("tel:")
        val formatted = PhoneNumberUtils.formatNumberToE164(decoded, context.config.regionHint)
        return formatted ?: PhoneNumberUtils.normalizeNumber(decoded)
    }

    /** Null when no rule matched, true to block the call, false to let it through. */
    fun evaluate(rules: List<ScreeningRule>, canonicalNumber: String): Boolean? {
        var decision: Boolean? = null
        for (rule in rules) {
            val regex = compile(rule.pattern) ?: continue
            if (regex.matches(canonicalNumber)) {
                decision = rule.block
            }
        }

        return decision
    }

    /** Null when the pattern compiles, otherwise the message to show the user. */
    fun isValidPattern(pattern: String): String? {
        return try {
            Regex(pattern)
            null
        } catch (exception: IllegalArgumentException) {
            // PatternSyntaxException is an IllegalArgumentException, so this catches both
            exception.message ?: exception.toString()
        }
    }

    private fun compile(pattern: String): Regex? {
        return try {
            Regex(pattern)
        } catch (exception: IllegalArgumentException) {
            null
        }
    }
}
