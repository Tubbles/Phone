package org.fossify.phone.services

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.fossify.commons.helpers.isQPlus
import org.fossify.phone.R
import org.fossify.phone.extensions.config
import org.fossify.phone.helpers.IntercomAutoOpen

/**
 * The Quick Settings tile that flips the intercom auto-open mode in one tap.
 *
 * Arming from the notification shade is the point: the door is usually opened
 * for someone who is already on their way, and the shade is reachable from the
 * lock screen, so the mode can be turned on without unlocking the phone.
 *
 * The tile carries no logic of its own. It flips the mode through
 * [IntercomAutoOpen.toggle] and reads its state back out of the prefs, so the
 * tile, the widget, the notification and the Intercom tab can never disagree.
 *
 * [IntercomAutoOpen.refreshSurfaces] pokes the tile with
 * [TileService.requestListeningState], which is the only way to make Quick
 * Settings redraw a tile whose state changed somewhere else.
 */
class IntercomTileService : TileService() {
    override fun onStartListening() {
        refreshTile()
    }

    override fun onClick() {
        IntercomAutoOpen.toggle(this)
        refreshTile()
    }

    /** Draws the current armed state, or nothing at all if we are not listening. */
    private fun refreshTile() {
        val tile = qsTile ?: return
        tile.state = if (IntercomAutoOpen.isArmed(config)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.intercom_tile_label)
        if (isQPlus()) {
            tile.subtitle = IntercomAutoOpen.shortStatusText(this)
        }
        tile.updateTile()
    }
}
