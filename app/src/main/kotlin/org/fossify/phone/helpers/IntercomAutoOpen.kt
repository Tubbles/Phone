package org.fossify.phone.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.telecom.Call
import android.telecom.VideoProfile
import android.telephony.PhoneNumberUtils
import android.text.format.DateFormat
import org.fossify.commons.extensions.normalizePhoneNumber
import org.fossify.commons.extensions.toast
import org.fossify.phone.R
import org.fossify.phone.extensions.config
import org.fossify.phone.extensions.getStateCompat
import org.fossify.phone.extensions.isOutgoing
import org.fossify.phone.receivers.IntercomActionReceiver
import org.fossify.phone.receivers.IntercomWidgetProvider
import org.fossify.phone.services.IntercomTileService
import java.util.Date

/**
 * Opens the front door by itself while the user has armed auto-open.
 *
 * The intercom by the door calls the phone, and the door unlocks when a DTMF
 * key is played into the call. Normally the user answers and presses that key
 * by hand. When auto-open is armed (a number of remaining openings plus a
 * deadline, both kept in [Config]), an incoming call is instead let ring for a
 * moment, answered, and fed the door key until the intercom hangs up.
 *
 * This has to live in the dialer: only the default dialer holds the
 * InCallService, and therefore only it owns the [Call] object needed to answer
 * a call and to inject DTMF into it. No other app on the phone can do either.
 *
 * Only the intercom's own number is treated this way: an incoming call whose
 * number does not match the one in [Config] is left alone, and a hidden or
 * missing number never matches. A call arriving while another call is already
 * up is left alone too, so answering it can never put that other call on hold.
 *
 * One opening is counted when the call goes active, since that is the moment a
 * door opening is actually being attempted. A call that is declined or that
 * gives up while still ringing costs nothing.
 *
 * Note that the user answering first, on the phone or from the watch, does not
 * cancel anything: the call still goes active, so the tone sequence still runs
 * and the door still opens. That is intended, since arming auto-open is a
 * statement that the next caller should be let in either way.
 *
 * Feature reference: pinetime-hacks doc/DESIGN-intercom-keytones.md.
 */
object IntercomAutoOpen {
    private const val MILLIS_PER_SECOND = 1000L
    private const val MILLIS_PER_HOUR = 60 * 60 * MILLIS_PER_SECOND
    private const val EXPIRY_REFRESH_CODE = 3
    private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /** The static settings, read once when a call is picked up for tracking. */
    private data class IntercomTiming(
        val key: Char,
        val ringMillis: Long,
        val answerDelayMillis: Long,
        val toneLengthMillis: Long,
        val toneRepeatMillis: Long,
        val hangUpMillis: Long
    )

    private val handler = Handler(Looper.getMainLooper())
    private var trackedCall: Call? = null
    private var trackedTiming: IntercomTiming? = null
    private var toneSequenceStarted = false

    /** True while there are openings left and the deadline has not passed. */
    fun isArmed(config: Config, nowMillis: Long = System.currentTimeMillis()): Boolean =
        config.intercomAutoOpenRemaining > 0 && nowMillis < config.intercomAutoOpenUntil

    /** True when [callerNumber] is the intercom calling, by its stored number. */
    fun isIntercomCaller(intercomNumber: String, callerNumber: String?): Boolean {
        if (intercomNumber.isBlank() || callerNumber == null) {
            return false
        }

        return PhoneNumberUtils.compare(intercomNumber.normalizePhoneNumber(), callerNumber.normalizePhoneNumber())
    }

    /** True while there is an intercom number to match incoming calls against. */
    fun canArm(config: Config): Boolean = config.intercomNumber.isNotBlank()

    /** Arms auto-open for [openings] more calls, expiring [hours] from now. */
    fun arm(context: Context, openings: Int, hours: Int) {
        val config = context.config
        config.intercomAutoOpenRemaining = openings
        config.intercomAutoOpenUntil = System.currentTimeMillis() + hours * MILLIS_PER_HOUR
        config.intercomLastOpenings = openings
        config.intercomLastHours = hours
        refreshSurfaces(context)
    }

    /** Disarms auto-open, leaving the static settings alone. */
    fun disarm(context: Context) {
        val config = context.config
        config.intercomAutoOpenRemaining = 0
        config.intercomAutoOpenUntil = 0L
        refreshSurfaces(context)
    }

    /**
     * Flips auto-open in one tap, reusing the openings and hours from last time.
     * This is the primitive behind every one-tap surface: the notification
     * action, the Quick Settings tile and the home-screen widget.
     */
    fun toggle(context: Context) {
        val config = context.config
        when {
            isArmed(config) -> disarm(context)
            canArm(config) -> arm(context, config.intercomLastOpenings, config.intercomLastHours)
            else -> context.toast(R.string.intercom_number_missing)
        }
    }

    /** The armed state in a few words, for the tile subtitle and the widget. */
    fun shortStatusText(context: Context): String {
        val config = context.config
        if (!isArmed(config)) {
            return context.getString(R.string.intercom_short_off)
        }

        val untilText = DateFormat.getTimeFormat(context).format(Date(config.intercomAutoOpenUntil))
        return context.getString(R.string.intercom_short_armed, config.intercomAutoOpenRemaining, untilText)
    }

    /**
     * Brings everything that shows the armed state in line with the prefs: the
     * ongoing notification, the home-screen widget and the Quick Settings tile.
     * Quick Settings only redraws a tile it is listening to, so the tile is
     * poked into a listening state rather than written to from here.
     *
     * Call this once per change. [arm] and [disarm] already end in it, so the
     * one-tap surfaces get their redraw by going through those.
     */
    fun refreshSurfaces(context: Context) {
        IntercomArmedNotification.update(context)
        IntercomWidgetProvider.updateAll(context)
        TileService.requestListeningState(context, ComponentName(context, IntercomTileService::class.java))
        scheduleExpiryRefresh(context)
    }

    /**
     * Wakes us up a second past the deadline, so the tile and the widget stop
     * claiming the mode is on the moment it ends. Nothing else runs at that
     * time: the notification takes itself down with setTimeoutAfter, but the
     * other two only change when something redraws them.
     *
     * A plain, inexact alarm is deliberate, since it needs no exact-alarm
     * permission. Doze can hold it back, which costs nothing but a stale line
     * of text on a surface the user is not looking at.
     */
    private fun scheduleExpiryRefresh(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val refreshIntent = Intent(context, IntercomActionReceiver::class.java)
        refreshIntent.action = INTERCOM_REFRESH
        val refreshPendingIntent =
            PendingIntent.getBroadcast(context, EXPIRY_REFRESH_CODE, refreshIntent, PENDING_INTENT_FLAGS)

        val config = context.config
        if (isArmed(config)) {
            alarmManager.set(AlarmManager.RTC, config.intercomAutoOpenUntil + MILLIS_PER_SECOND, refreshPendingIntent)
        } else {
            alarmManager.cancel(refreshPendingIntent)
        }
    }

    /** Starts tracking [call] if it is the armed-for intercom calling in. */
    fun onCallAdded(context: Context, call: Call) {
        if (call.isOutgoing() || call.getStateCompat() != Call.STATE_RINGING) {
            return
        }
        // CallService.onCallAdded registers the call with CallManager before calling here, so a
        // SingleCall state means this call is the only one and answering it holds nothing else.
        if (CallManager.getPhoneState() !is SingleCall) {
            return
        }
        if (trackedCall != null) {
            return
        }
        val config = context.config
        if (!isArmed(config)) {
            return
        }
        val callerNumber = call.details.handle?.schemeSpecificPart
        if (!isIntercomCaller(config.intercomNumber, callerNumber)) {
            return
        }
        val timing = readTiming(config)
        trackedCall = call
        trackedTiming = timing
        toneSequenceStarted = false
        handler.postDelayed({ answerCall(call) }, timing.ringMillis)
    }

    /** Drives the tracked call from ringing to opened, and cleans up after it. */
    fun onCallStateChanged(context: Context, call: Call, state: Int) {
        if (call != trackedCall) {
            return
        }
        when (state) {
            Call.STATE_ACTIVE -> startToneSequence(context, call)
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> forgetCall()
        }
    }

    /** Stops everything if the tracked call is gone. */
    fun onCallRemoved(call: Call) {
        if (call == trackedCall) {
            forgetCall()
        }
    }

    private fun readTiming(config: Config) = IntercomTiming(
        key = config.intercomKey.firstOrNull() ?: INTERCOM_DEFAULT_KEY.first(),
        ringMillis = config.intercomRingSeconds * MILLIS_PER_SECOND,
        answerDelayMillis = config.intercomAnswerDelaySeconds * MILLIS_PER_SECOND,
        toneLengthMillis = config.intercomToneLengthMs.toLong(),
        toneRepeatMillis = config.intercomToneRepeatSeconds * MILLIS_PER_SECOND,
        hangUpMillis = config.intercomHangUpSeconds * MILLIS_PER_SECOND
    )

    private fun answerCall(call: Call) {
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    /**
     * Runs once per call, on the first transition to active. A call can go
     * active again after being put on hold and taken back, and the flag keeps
     * that from restarting the sequence and counting a second opening.
     */
    private fun startToneSequence(context: Context, call: Call) {
        val timing = trackedTiming ?: return
        if (toneSequenceStarted) {
            return
        }
        toneSequenceStarted = true
        handler.removeCallbacksAndMessages(null)
        countOneOpening(context)
        handler.postDelayed({ playTone(call, timing) }, timing.answerDelayMillis)
        if (timing.hangUpMillis > 0) {
            handler.postDelayed({ call.disconnect() }, timing.answerDelayMillis + timing.hangUpMillis)
        }
    }

    private fun countOneOpening(context: Context) {
        val config = context.config
        config.intercomAutoOpenRemaining = maxOf(0, config.intercomAutoOpenRemaining - 1)
        config.intercomLastOpenedAt = System.currentTimeMillis()
        refreshSurfaces(context)
    }

    /**
     * Plays the door key and schedules the next attempt. The loop ends when the
     * intercom hangs up, which it is expected to do once the door is open, or
     * when the hang-up timeout fires and drops the call from this side.
     */
    private fun playTone(call: Call, timing: IntercomTiming) {
        call.playDtmfTone(timing.key)
        handler.postDelayed({ call.stopDtmfTone() }, timing.toneLengthMillis)
        handler.postDelayed({ playTone(call, timing) }, timing.toneRepeatMillis)
    }

    private fun forgetCall() {
        handler.removeCallbacksAndMessages(null)
        trackedCall = null
        trackedTiming = null
        toneSequenceStarted = false
    }
}
