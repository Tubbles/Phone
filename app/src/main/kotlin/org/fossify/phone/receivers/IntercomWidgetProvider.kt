package org.fossify.phone.receivers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.extensions.setText
import org.fossify.phone.R
import org.fossify.phone.extensions.config
import org.fossify.phone.helpers.INTERCOM_TOGGLE
import org.fossify.phone.helpers.IntercomAutoOpen

/**
 * The home-screen widget that flips the intercom auto-open mode in one tap.
 *
 * It is the Quick Settings tile's sibling for people who live on the home
 * screen instead of in the shade, and it carries no logic of its own either:
 * the tap sends [INTERCOM_TOGGLE] to [IntercomActionReceiver], and the text is
 * read back out of the prefs by [IntercomAutoOpen.shortStatusText].
 *
 * Android only redraws a widget on its own every half hour, so
 * [IntercomAutoOpen.refreshSurfaces] calls [updateAll] whenever the mode
 * changes.
 */
class IntercomWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context))
        }
    }

    companion object {
        private const val TOGGLE_CODE = 2
        private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        /** Redraws every instance the user has placed, if there are any. */
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, IntercomWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) {
                return
            }

            appWidgetIds.forEach { appWidgetId ->
                appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context))
            }
        }

        /** The whole widget, coloured with the user's widget colours. */
        private fun buildRemoteViews(context: Context): RemoteViews {
            val config = context.config
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_intercom)
            remoteViews.applyColorFilter(R.id.widget_intercom_background, config.widgetBgColor)
            remoteViews.applyColorFilter(R.id.widget_intercom_icon, config.widgetTextColor)
            remoteViews.setText(R.id.widget_intercom_status, IntercomAutoOpen.shortStatusText(context))
            remoteViews.setTextColor(R.id.widget_intercom_status, config.widgetTextColor)
            remoteViews.setOnClickPendingIntent(R.id.widget_intercom_holder, togglePendingIntent(context))
            return remoteViews
        }

        private fun togglePendingIntent(context: Context): PendingIntent {
            val toggleIntent = Intent(context, IntercomActionReceiver::class.java)
            toggleIntent.action = INTERCOM_TOGGLE
            return PendingIntent.getBroadcast(context, TOGGLE_CODE, toggleIntent, PENDING_INTENT_FLAGS)
        }
    }
}
