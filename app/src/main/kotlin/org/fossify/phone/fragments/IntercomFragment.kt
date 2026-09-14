package org.fossify.phone.fragments

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.AttributeSet
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.toast
import org.fossify.phone.R
import org.fossify.phone.activities.SettingsActivity
import org.fossify.phone.databinding.FragmentIntercomBinding
import org.fossify.phone.extensions.config
import org.fossify.phone.helpers.IntercomAutoOpen
import java.util.Date

/**
 * Arms and disarms the intercom auto-open mode driven by [IntercomAutoOpen].
 *
 * Arming takes a number of openings and a number of hours, which together make
 * up the window during which incoming calls are answered and fed the door key.
 * While the window is open the inputs are hidden, since the only thing left to
 * do is to close it again.
 */
class IntercomFragment(
    context: Context, attributeSet: AttributeSet,
) : MyViewPagerFragment<MyViewPagerFragment.IntercomInnerBinding>(context, attributeSet) {

    private lateinit var binding: FragmentIntercomBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentIntercomBinding.bind(this)
        innerBinding = IntercomInnerBinding(binding)
    }

    override fun setupFragment() {
        binding.intercomOpenings.setText(context.config.intercomLastOpenings.toString())
        binding.intercomHours.setText(context.config.intercomLastHours.toString())
        binding.intercomToggle.setOnClickListener {
            toggleAutoOpen()
        }

        refreshItems()
    }

    override fun setupColors(textColor: Int, primaryColor: Int, properPrimaryColor: Int) {
        binding.intercomStatus.setTextColor(textColor)
        binding.intercomNumber.setTextColor(textColor)
        binding.intercomLastOpened.setTextColor(textColor)
        binding.intercomOpeningsLabel.setTextColor(textColor)
        binding.intercomHoursLabel.setTextColor(textColor)
    }

    override fun onSearchClosed() {}

    override fun onSearchQueryChanged(text: String) {}

    fun refreshItems() {
        IntercomAutoOpen.refreshSurfaces(context)
        refreshIntercomNumber()
        refreshLastOpened()
        if (IntercomAutoOpen.isArmed(context.config)) {
            showArmedState()
        } else {
            showDisarmedState()
        }
    }

    /** Shows which number auto-open answers, or that none is set yet. */
    private fun refreshIntercomNumber() {
        val intercomNumber = context.config.intercomNumber
        if (intercomNumber.isBlank()) {
            binding.intercomNumber.setText(R.string.intercom_number_not_set)
        } else {
            binding.intercomNumber.text = context.getString(R.string.intercom_number_line, intercomNumber)
        }
    }

    /** Shows when the door was opened last, once that has ever happened. */
    private fun refreshLastOpened() {
        val lastOpenedAt = context.config.intercomLastOpenedAt
        if (lastOpenedAt > 0) {
            binding.intercomLastOpened.text = context.getString(R.string.intercom_last_opened, formatLastOpened(lastOpenedAt))
            binding.intercomLastOpened.beVisible()
        } else {
            binding.intercomLastOpened.beGone()
        }
    }

    private fun formatLastOpened(lastOpenedAt: Long) = DateUtils.formatDateTime(
        context,
        lastOpenedAt,
        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL
    )

    private fun showArmedState() {
        val config = context.config
        val untilText = DateFormat.getTimeFormat(context).format(Date(config.intercomAutoOpenUntil))
        binding.intercomStatus.text = context.getString(R.string.intercom_auto_open_armed, config.intercomAutoOpenRemaining, untilText)
        binding.intercomToggle.setText(R.string.intercom_disarm)
        binding.intercomOpeningsRow.beGone()
        binding.intercomHoursRow.beGone()
    }

    private fun showDisarmedState() {
        binding.intercomStatus.setText(R.string.intercom_auto_open_off)
        binding.intercomToggle.setText(R.string.intercom_arm)
        binding.intercomOpeningsRow.beVisible()
        binding.intercomHoursRow.beVisible()
    }

    private fun toggleAutoOpen() {
        if (IntercomAutoOpen.isArmed(context.config)) {
            IntercomAutoOpen.disarm(context)
        } else if (!armFromInputs()) {
            return
        }

        refreshItems()
    }

    /** Arms auto-open from the two inputs, or complains and does nothing. */
    private fun armFromInputs(): Boolean {
        if (!IntercomAutoOpen.canArm(context.config)) {
            context.toast(R.string.intercom_number_missing)
            context.startActivity(Intent(context, SettingsActivity::class.java))
            return false
        }

        val openings = binding.intercomOpenings.text.toString().toIntOrNull()
        val hours = binding.intercomHours.text.toString().toIntOrNull()
        if (openings == null || openings < 1 || hours == null || hours < 1) {
            context.toast(R.string.intercom_invalid_inputs)
            return false
        }

        IntercomAutoOpen.arm(context, openings, hours)
        return true
    }
}
