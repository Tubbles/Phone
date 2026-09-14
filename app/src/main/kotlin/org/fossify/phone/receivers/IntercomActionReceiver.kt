package org.fossify.phone.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.phone.helpers.INTERCOM_DISARM
import org.fossify.phone.helpers.INTERCOM_TOGGLE
import org.fossify.phone.helpers.IntercomAutoOpen

/**
 * Switches the intercom auto-open mode from outside the app's own UI.
 *
 * It is the target of the action on the armed notification, and of the Quick
 * Settings tile and the home-screen widget, which all send one of the two
 * actions below instead of carrying their own copy of the logic.
 */
class IntercomActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            INTERCOM_DISARM -> IntercomAutoOpen.disarm(context)
            INTERCOM_TOGGLE -> IntercomAutoOpen.toggle(context)
        }
    }
}
