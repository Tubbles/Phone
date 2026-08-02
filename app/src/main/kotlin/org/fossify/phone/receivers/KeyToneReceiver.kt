package org.fossify.phone.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.phone.helpers.CallManager

/**
 * Plays a DTMF key on the active call when a PineTime watch sends one.
 *
 * The frame originates on the watch's KeyTones BLE characteristic, travels
 * through Gadgetbridge's BLE Intent API, and is re-broadcast explicitly to
 * this package by the ClockSync hub app (Gadgetbridge can only target one
 * package). Payload is one hex-encoded ASCII byte: '0'-'9', '*' or '#'.
 * With no active call, CallManager.keypad is a safe no-op.
 *
 * Protocol reference: pinetime-hacks doc/DESIGN-intercom-keytones.md.
 */
class KeyToneReceiver : BroadcastReceiver() {
    companion object {
        private const val KEYTONES_CHARACTERISTIC_UUID = "00080001-78fc-48fe-8e23-433b3a1942d0"
        private const val EXTRA_CHARACTERISTIC = "EXTRA_CHARACTERISTIC"
        private const val EXTRA_PAYLOAD = "EXTRA_PAYLOAD"
        private val VALID_KEYS = "0123456789*#".toSet()
    }

    override fun onReceive(context: Context, intent: Intent) {
        val characteristic = intent.getStringExtra(EXTRA_CHARACTERISTIC) ?: return
        if (!characteristic.equals(KEYTONES_CHARACTERISTIC_UUID, ignoreCase = true)) {
            return
        }
        val payloadHex = intent.getStringExtra(EXTRA_PAYLOAD) ?: return
        if (payloadHex.length != 2) {
            return
        }
        val key = payloadHex.toIntOrNull(16)?.toChar() ?: return
        if (key !in VALID_KEYS) {
            return
        }
        CallManager.keypad(key)
    }
}
