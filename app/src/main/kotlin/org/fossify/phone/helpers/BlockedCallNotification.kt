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
import org.fossify.phone.activities.MainActivity

/**
 * The notification posted whenever a [ScreeningRule] or the hidden number switch blocks an
 * incoming call.
 *
 * The screening response keeps the call in the call log, so a blocked call does show up in the
 * recents tab afterwards, but it skips the system's own missed call notification and so passes by
 * unnoticed. This names the number and, for a rule block, the rule that caught it, since a pattern
 * that is a little too wide is the thing one actually wants to notice. A tap opens the recents tab,
 * where the blocked call sits with the rest of the history.
 *
 * Only the canonical number is shown. Looking up a contact name would be the wrong way around:
 * these are the calls nobody in the contact list made.
 */
object BlockedCallNotification {
    private const val CHANNEL_ID = "blocked_calls"

    /** Deliberately apart from the request codes IntercomArmedNotification hands out. */
    private const val OPEN_RECENTS_CODE = 2
    private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun show(context: Context, canonicalNumber: String, rule: ScreeningRule) {
        notifyBlockedCall(context, context.getString(R.string.blocked_call_text, canonicalNumber, rule.pattern))
    }

    fun showHidden(context: Context) {
        notifyBlockedCall(context, context.getString(R.string.blocked_call_hidden_text))
    }

    private fun notifyBlockedCall(context: Context, text: String) {
        if (!context.hasPermission(PERMISSION_POST_NOTIFICATIONS)) {
            return
        }

        createNotificationChannel(context)
        val notification = buildNotification(context, text)
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

    private fun buildNotification(context: Context, text: String): Notification {
        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_block_vector)
            .setContentTitle(context.getString(R.string.blocked_call_title))
            .setContentText(text)
            .setContentIntent(openRecentsPendingIntent(context))
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()
    }

    /** ACTION_VIEW is what MainActivity reads to open the call history tab rather than the default one. */
    private fun openRecentsPendingIntent(context: Context): PendingIntent {
        val openRecentsIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(context, OPEN_RECENTS_CODE, openRecentsIntent, PENDING_INTENT_FLAGS)
    }
}
