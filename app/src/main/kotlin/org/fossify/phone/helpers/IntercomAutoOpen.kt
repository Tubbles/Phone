package org.fossify.phone.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.VideoProfile
import org.fossify.phone.extensions.config
import org.fossify.phone.extensions.getStateCompat
import org.fossify.phone.extensions.isOutgoing

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

    /** The static settings, read once when a call is picked up for tracking. */
    private data class IntercomTiming(
        val key: Char,
        val ringMillis: Long,
        val answerDelayMillis: Long,
        val toneLengthMillis: Long,
        val toneRepeatMillis: Long
    )

    private val handler = Handler(Looper.getMainLooper())
    private var trackedCall: Call? = null
    private var trackedTiming: IntercomTiming? = null
    private var toneSequenceStarted = false

    /** True while there are openings left and the deadline has not passed. */
    fun isArmed(config: Config, nowMillis: Long = System.currentTimeMillis()): Boolean =
        config.intercomAutoOpenRemaining > 0 && nowMillis < config.intercomAutoOpenUntil

    /** Arms auto-open for [openings] more calls, expiring [hours] from now. */
    fun arm(config: Config, openings: Int, hours: Int) {
        config.intercomAutoOpenRemaining = openings
        config.intercomAutoOpenUntil = System.currentTimeMillis() + hours * MILLIS_PER_HOUR
    }

    /** Disarms auto-open, leaving the static settings alone. */
    fun disarm(config: Config) {
        config.intercomAutoOpenRemaining = 0
        config.intercomAutoOpenUntil = 0L
    }

    /** Starts tracking [call] if it is an incoming call and auto-open is armed. */
    fun onCallAdded(context: Context, call: Call) {
        if (call.isOutgoing() || call.getStateCompat() != Call.STATE_RINGING) {
            return
        }
        if (trackedCall != null) {
            return
        }
        val config = context.config
        if (!isArmed(config)) {
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
        toneRepeatMillis = config.intercomToneRepeatSeconds * MILLIS_PER_SECOND
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
        countOneOpening(context.config)
        handler.postDelayed({ playTone(call, timing) }, timing.answerDelayMillis)
    }

    private fun countOneOpening(config: Config) {
        config.intercomAutoOpenRemaining = maxOf(0, config.intercomAutoOpenRemaining - 1)
    }

    /**
     * Plays the door key and schedules the next attempt. The intercom is
     * expected to hang up once the door is open, which stops the loop, so there
     * is deliberately no repeat limit.
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
