package org.fossify.phone.helpers

import android.content.Context
import android.content.Intent
import android.telecom.Call
import org.fossify.phone.BuildConfig
import org.fossify.phone.extensions.getStateCompat

/**
 * Tells a PineTime running the InfiniTime fork whether a call is in progress,
 * so the watch can open and close its in-call screen.
 *
 * Gadgetbridge cannot do this on modern Android: it derives outgoing calls
 * from the NEW_OUTGOING_CALL broadcast, which was removed in Android 11, so it
 * only ever reports incoming calls. As the default dialer this app holds the
 * InCallService and therefore sees every call, including outgoing and VoIP
 * ones.
 *
 * The write goes out through Gadgetbridge's BLE Intent API (it owns the BLE
 * link) to the watch's call-state characteristic: 1 = call in progress,
 * 0 = no call. Ringing deliberately does not count as in progress, because the
 * watch already shows its own incoming-call screen then.
 *
 * Protocol reference: pinetime-hacks doc/DESIGN-intercom-keytones.md.
 */
object WatchCallState {
    private const val ACTION_CHARACTERISTIC_WRITE =
        "nodomain.freeyourgadget.gadgetbridge.ble_api.commands.CHARACTERISTIC_WRITE"
    private const val EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS"
    private const val EXTRA_CHARACTERISTIC_UUID = "EXTRA_CHARACTERISTIC_UUID"
    private const val EXTRA_PAYLOAD = "EXTRA_PAYLOAD"
    private const val CALL_STATE_CHARACTERISTIC_UUID = "00080002-78fc-48fe-8e23-433b3a1942d0"

    private var lastReported: Boolean? = null

    /** Reports the state implied by [call], if it changed. */
    fun onCallStateChanged(context: Context, call: Call?) {
        val inProgress = when (call.getStateCompat()) {
            Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING -> true
            else -> false
        }
        report(context, inProgress)
    }

    /** Reports that no call is left. */
    fun onNoCall(context: Context) = report(context, false)

    private fun report(context: Context, inProgress: Boolean) {
        if (inProgress == lastReported) {
            return
        }
        if (BuildConfig.WATCH_MAC.isEmpty()) {
            return
        }
        lastReported = inProgress
        val intent = Intent(ACTION_CHARACTERISTIC_WRITE).apply {
            setPackage(BuildConfig.GADGETBRIDGE_PACKAGE)
            putExtra(EXTRA_DEVICE_ADDRESS, BuildConfig.WATCH_MAC)
            putExtra(EXTRA_CHARACTERISTIC_UUID, CALL_STATE_CHARACTERISTIC_UUID)
            putExtra(EXTRA_PAYLOAD, if (inProgress) "01" else "00")
        }
        context.sendBroadcast(intent)
    }
}
