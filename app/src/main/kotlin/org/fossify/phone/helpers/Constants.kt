package org.fossify.phone.helpers

import org.fossify.commons.helpers.TAB_CALL_HISTORY
import org.fossify.commons.helpers.TAB_CONTACTS
import org.fossify.commons.helpers.TAB_FAVORITES

// shared prefs
const val SPEED_DIAL = "speed_dial"
const val REMEMBER_SIM_PREFIX = "remember_sim_"
const val GROUP_SUBSEQUENT_CALLS = "group_subsequent_calls"
const val OPEN_DIAL_PAD_AT_LAUNCH = "open_dial_pad_at_launch"
const val DISABLE_PROXIMITY_SENSOR = "disable_proximity_sensor"
const val DISABLE_SWIPE_TO_ANSWER = "disable_swipe_to_answer"
const val SHOW_TABS = "show_tabs"
const val FAVORITES_CONTACTS_ORDER = "favorites_contacts_order"
const val FAVORITES_CUSTOM_ORDER_SELECTED = "favorites_custom_order_selected"
const val WAS_OVERLAY_SNACKBAR_CONFIRMED = "was_overlay_snackbar_confirmed"
const val DIALPAD_VIBRATION = "dialpad_vibration"
const val DIALPAD_BEEPS = "dialpad_beeps"
const val HIDE_DIALPAD_NUMBERS = "hide_dialpad_numbers"
const val ALWAYS_SHOW_FULLSCREEN = "always_show_fullscreen"
const val INTERCOM_NUMBER = "intercom_number"
const val INTERCOM_KEY = "intercom_key"
const val INTERCOM_RING_SECONDS = "intercom_ring_seconds"
const val INTERCOM_ANSWER_DELAY_SECONDS = "intercom_answer_delay_seconds"
const val INTERCOM_TONE_LENGTH_MS = "intercom_tone_length_ms"
const val INTERCOM_TONE_REPEAT_SECONDS = "intercom_tone_repeat_seconds"
const val INTERCOM_AUTO_OPEN_REMAINING = "intercom_auto_open_remaining"
const val INTERCOM_AUTO_OPEN_UNTIL = "intercom_auto_open_until"

// Local to this app, deliberately above every TAB_* bit the commons library defines
const val TAB_INTERCOM = 128

const val ALL_TABS_MASK = TAB_CONTACTS or TAB_FAVORITES or TAB_CALL_HISTORY or TAB_INTERCOM

val tabsList = arrayListOf(TAB_CONTACTS, TAB_FAVORITES, TAB_CALL_HISTORY, TAB_INTERCOM)

private const val PATH = "org.fossify.phone.action."
const val ACCEPT_CALL = PATH + "ACCEPT_CALL"
const val DECLINE_CALL = PATH + "DECLINE_CALL"

const val DIALPAD_TONE_LENGTH_MS = 150L // The length of DTMF tones in milliseconds

// Defaults for the intercom auto-open feature, shared by Config and the settings UI
const val INTERCOM_DEFAULT_KEY = "5" // The DTMF key the intercom listens for to open the door
const val INTERCOM_DEFAULT_RING_SECONDS = 2 // Let it ring this long before answering
const val INTERCOM_DEFAULT_ANSWER_DELAY_SECONDS = 2 // Wait this long after answering before the first tone
const val INTERCOM_DEFAULT_TONE_LENGTH_MS = 500 // How long each tone is held down
const val INTERCOM_DEFAULT_TONE_REPEAT_SECONDS = 4 // Retry interval while the call is still up
const val INTERCOM_ALLOWED_KEYS = "0123456789*#" // The DTMF characters the door key may be picked from
