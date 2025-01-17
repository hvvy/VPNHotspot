package be.mygod.vpnhotspot.manage

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.IBinder
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import be.mygod.vpnhotspot.LocalOnlyHotspotService
import be.mygod.vpnhotspot.R
import be.mygod.vpnhotspot.util.KillableTileService
import be.mygod.vpnhotspot.util.stopAndUnbind

@RequiresApi(26)
class LocalOnlyHotspotTileService : KillableTileService() {
    private val tile by lazy { Icon.createWithResource(application, R.drawable.ic_action_perm_scan_wifi) }

    private var binder: LocalOnlyHotspotService.Binder? = null

    override fun onStartListening() {
        super.onStartListening()
        bindService(Intent(this, LocalOnlyHotspotService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onStopListening() {
        stopAndUnbind(this)
        super.onStopListening()
    }

    override fun onClick() {
        val binder = binder
        when {
            binder == null -> tapPending = true
            binder.iface == null -> ContextCompat.startForegroundService(this,
                    Intent(this, LocalOnlyHotspotService::class.java))
            else -> binder.stop()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        binder = service as LocalOnlyHotspotService.Binder
        service.ifaceChanged[this] = {
            qsTile?.run {
                icon = tile
                if (it.isNullOrEmpty()) {
                    state = Tile.STATE_INACTIVE
                    label = getText(R.string.tethering_temp_hotspot)
                } else {
                    state = Tile.STATE_ACTIVE
                    label = service.configuration?.SSID ?: getText(R.string.tethering_temp_hotspot)
                }
                updateTile()
            }
        }
        super.onServiceConnected(name, service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        binder?.ifaceChanged?.remove(this)
        binder = null
    }
}
