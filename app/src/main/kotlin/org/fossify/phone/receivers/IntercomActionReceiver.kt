package org.fossify.phone.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.phone.helpers.INTERCOM_DISARM
import org.fossify.phone.helpers.INTERCOM_REFRESH
import org.fossify.phone.helpers.INTERCOM_TOGGLE
import org.fossify.phone.helpers.IntercomAutoOpen

/**
 * Switches the intercom auto-open mode from outside the app's own UI.
 *
 * It is the target of the action on the armed notification, and of the
 * home-screen widget, which both send one of the actions below instead of
 * carrying their own copy of the logic. The Quick Settings tile is a service
 * and calls [IntercomAutoOpen] directly.
 *
 * [INTERCOM_REFRESH] changes nothing: it is the alarm that fires at the
 * deadline so the surfaces stop claiming the mode is on.
 */
class IntercomActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTERCOM_DISARM -> IntercomAutoOpen.disarm(context)
            INTERCOM_TOGGLE -> IntercomAutoOpen.toggle(context)
            INTERCOM_REFRESH -> IntercomAutoOpen.refreshSurfaces(context)
        }
    }
}
