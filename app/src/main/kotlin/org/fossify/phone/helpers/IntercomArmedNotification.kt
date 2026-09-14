package org.fossify.phone.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.text.format.DateFormat
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.helpers.PERMISSION_POST_NOTIFICATIONS
import org.fossify.phone.R
import org.fossify.phone.activities.MainActivity
import org.fossify.phone.extensions.config
import org.fossify.phone.receivers.IntercomActionReceiver
import java.util.Date

/**
 * The ongoing notification shown while [IntercomAutoOpen] is armed.
 *
 * The door opening by itself is easy to forget about, so the armed window is
 * kept visible for as long as it lasts, with one action that closes it again.
 * The notification also times itself out at the deadline, since nothing runs at
 * that moment to take it down.
 *
 * Nothing calls this directly except [IntercomAutoOpen.refreshSurfaces], which
 * is the one place that brings the visible state in line with the prefs.
 */
object IntercomArmedNotification {
    /** Deliberately apart from CallNotificationManager's own notification id. */
    private const val INTERCOM_NOTIFICATION_ID = 43
    private const val CHANNEL_ID = "intercom_auto_open"
    private const val OPEN_APP_CODE = 0
    private const val DISARM_CODE = 1
    private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** Shows, updates or hides the notification, whichever the prefs call for. */
    fun update(context: Context) {
        if (IntercomAutoOpen.isArmed(context.config)) {
            show(context)
        } else {
            cancel(context)
        }
    }

    fun cancel(context: Context) {
        context.notificationManager.cancel(INTERCOM_NOTIFICATION_ID)
    }

    private fun show(context: Context) {
        if (!context.hasPermission(PERMISSION_POST_NOTIFICATIONS)) {
            return
        }

        createNotificationChannel(context)
        context.notificationManager.notify(INTERCOM_NOTIFICATION_ID, buildNotification(context))
    }

    private fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.intercom_notification_channel)
        NotificationChannel(CHANNEL_ID, name, IMPORTANCE_LOW).apply {
            setSound(null, null)
            context.notificationManager.createNotificationChannel(this)
        }
    }

    private fun buildNotification(context: Context): Notification {
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_intercom_vector)
            .setContentTitle(context.getString(R.string.intercom_notification_title))
            .setContentText(armedText(context))
            .setContentIntent(openAppPendingIntent(context))
            .addAction(disarmAction(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        val millisLeft = context.config.intercomAutoOpenUntil - System.currentTimeMillis()
        if (millisLeft > 0) {
            builder.setTimeoutAfter(millisLeft)
        }

        return builder.build()
    }

    /** The openings left and the deadline, worded as the Intercom tab words them. */
    private fun armedText(context: Context): String {
        val config = context.config
        val untilText = DateFormat.getTimeFormat(context).format(Date(config.intercomAutoOpenUntil))
        return context.getString(R.string.intercom_auto_open_armed, config.intercomAutoOpenRemaining, untilText)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val openAppIntent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, OPEN_APP_CODE, openAppIntent, PENDING_INTENT_FLAGS)
    }

    private fun disarmAction(context: Context): Notification.Action {
        val disarmIntent = Intent(context, IntercomActionReceiver::class.java)
        disarmIntent.action = INTERCOM_DISARM
        val disarmPendingIntent = PendingIntent.getBroadcast(context, DISARM_CODE, disarmIntent, PENDING_INTENT_FLAGS)
        val icon = Icon.createWithResource(context, R.drawable.ic_intercom_vector)
        return Notification.Action.Builder(icon, context.getString(R.string.intercom_disarm), disarmPendingIntent).build()
    }
}
