package org.fossify.phone.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.helpers.PERMISSION_POST_NOTIFICATIONS
import org.fossify.phone.R
import org.fossify.phone.activities.ManageCallScreeningRulesActivity

/**
 * The notification posted whenever a [ScreeningRule] blocks an incoming call.
 *
 * A blocked call otherwise leaves no trace at all: the screening response skips the call log and
 * the system's own missed call notification, so this is the only place the number shows up. It
 * names the rule that caught it too, since a pattern that is a little too wide is the thing one
 * actually wants to notice, and a tap opens the rule list to fix it.
 *
 * Only the canonical number is shown. Looking up a contact name would be the wrong way around:
 * these are the calls nobody in the contact list made.
 */
object BlockedCallNotification {
    private const val CHANNEL_ID = "blocked_calls"

    /** Deliberately apart from the request codes IntercomArmedNotification hands out. */
    private const val OPEN_RULES_CODE = 2
    private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun show(context: Context, canonicalNumber: String, rule: ScreeningRule) {
        if (!context.hasPermission(PERMISSION_POST_NOTIFICATIONS)) {
            return
        }

        createNotificationChannel(context)
        val notification = buildNotification(context, canonicalNumber, rule)
        context.notificationManager.notify(notificationId(), notification)
    }

    /** One id per blocked call, so a number never silently replaces the one blocked before it. */
    private fun notificationId(): Int {
        return System.currentTimeMillis().toInt()
    }

    private fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.blocked_call_channel)
        NotificationChannel(CHANNEL_ID, name, IMPORTANCE_DEFAULT).apply {
            setSound(null, null)
            context.notificationManager.createNotificationChannel(this)
        }
    }

    private fun buildNotification(context: Context, canonicalNumber: String, rule: ScreeningRule): Notification {
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_block_vector)
            .setContentTitle(context.getString(R.string.blocked_call_title))
            .setContentText(context.getString(R.string.blocked_call_text, canonicalNumber, rule.pattern))
            .setContentIntent(openRulesPendingIntent(context))
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()
    }

    private fun openRulesPendingIntent(context: Context): PendingIntent {
        val openRulesIntent = Intent(context, ManageCallScreeningRulesActivity::class.java)
        return PendingIntent.getActivity(context, OPEN_RULES_CODE, openRulesIntent, PENDING_INTENT_FLAGS)
    }
}
