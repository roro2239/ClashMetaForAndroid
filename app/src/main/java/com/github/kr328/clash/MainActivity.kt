package com.github.kr328.clash

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import com.github.kr328.clash.design.R as DesignR

class MainActivity : BaseActivity<MainComposeDesign>() {
    override suspend fun main() {
        val design = MainComposeDesign(this)

        setContentDesign(design)

        var proxyNames = loadProxyNames()
        val proxyReloadLock = Semaphore(10)

        design.patchOverrideMode(loadOverrideMode())
        design.patchProxyGroups(proxyNames)
        design.fetchHome()
        design.fetchProfiles()
        proxyNames.indices.forEach { index ->
            design.reloadProxyGroup(index, proxyNames, proxyReloadLock)
        }

        val trafficTicker = ticker(TimeUnit.SECONDS.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart,
                        Event.ServiceRecreated,
                        Event.ClashStop,
                        Event.ClashStart,
                        Event.ProfileLoaded,
                        Event.ProfileChanged -> design.fetchHome()
                        else -> Unit
                    }

                    when (it) {
                        Event.ActivityStart, Event.ProfileChanged -> design.fetchProfiles()
                        else -> Unit
                    }

                    when (it) {
                        Event.ClashStart, Event.ProfileLoaded -> {
                            val newProxyNames = loadProxyNames()

                            if (newProxyNames != proxyNames) {
                                proxyNames = newProxyNames
                                design.patchOverrideMode(loadOverrideMode())
                                design.patchProxyGroups(proxyNames)
                                proxyNames.indices.forEach { index ->
                                    design.reloadProxyGroup(index, proxyNames, proxyReloadLock)
                                }
                            }
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        MainComposeDesign.Request.ToggleStatus -> {
                            if (clashRunning) {
                                stopClashService()
                            } else {
                                design.startClash()
                            }
                        }
                        MainComposeDesign.Request.OpenProviders ->
                            startActivity(ProvidersActivity::class.intent)
                        MainComposeDesign.Request.OpenLogs -> {
                            if (LogcatService.running) {
                                startActivity(LogcatActivity::class.intent)
                            } else {
                                startActivity(LogsActivity::class.intent)
                            }
                        }
                        MainComposeDesign.Request.OpenHelp ->
                            startActivity(HelpActivity::class.intent)
                        MainComposeDesign.Request.OpenAbout ->
                            design.showAbout(queryAppVersionName())
                        MainComposeDesign.Request.CreateProfile ->
                            startActivity(NewProfileActivity::class.intent)
                        MainComposeDesign.Request.UpdateAllProfiles ->
                            updateAllProfiles(design)
                        MainComposeDesign.Request.StartAppSettings ->
                            startActivity(AppSettingsActivity::class.intent)
                        MainComposeDesign.Request.StartNetworkSettings ->
                            startActivity(NetworkSettingsActivity::class.intent)
                        MainComposeDesign.Request.StartOverrideSettings ->
                            startActivity(OverrideSettingsActivity::class.intent)
                        MainComposeDesign.Request.StartMetaFeatureSettings ->
                            startActivity(MetaFeatureSettingsActivity::class.intent)
                        is MainComposeDesign.Request.SelectProxy -> {
                            withClash {
                                patchSelector(proxyNames[it.groupIndex], it.name)
                            }

                            design.patchProxySelection(it.groupIndex, it.name)
                        }
                        is MainComposeDesign.Request.UrlTest -> {
                            design.patchProxyTesting(it.groupIndex, true)

                            launch {
                                withClash {
                                    healthCheck(proxyNames[it.groupIndex])
                                }

                                design.reloadProxyGroup(it.groupIndex, proxyNames, proxyReloadLock)
                            }
                        }
                        is MainComposeDesign.Request.PatchMode -> {
                            patchMode(it.mode)
                            design.patchOverrideMode(it.mode)
                        }
                        is MainComposeDesign.Request.UpdateProfile ->
                            withProfile { update(it.profile.uuid) }
                        is MainComposeDesign.Request.ActiveProfile -> {
                            withProfile {
                                if (it.profile.imported) {
                                    setActive(it.profile)
                                } else {
                                    design.requestSave(it.profile)
                                }
                            }
                        }
                        is MainComposeDesign.Request.EditProfile ->
                            startActivity(PropertiesActivity::class.intent.setUUID(it.profile.uuid))
                        is MainComposeDesign.Request.DuplicateProfile -> {
                            val uuid = withProfile { clone(it.profile.uuid) }

                            startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                        }
                        is MainComposeDesign.Request.DeleteProfile ->
                            withProfile { delete(it.profile.uuid) }
                    }
                }
                if (clashRunning) {
                    trafficTicker.onReceive {
                        design.fetchTraffic()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (design?.returnHome() == true) return

        super.onBackPressed()
    }

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        if (uuid == null) return

        launch {
            val name = withProfile { queryByUUID(uuid)?.name }

            design?.showToast(
                getString(DesignR.string.toast_profile_updated_complete, name),
                ToastDuration.Long,
            )
            design?.fetchProfiles()
        }
    }

    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        if (uuid == null) return

        launch {
            val name = withProfile { queryByUUID(uuid)?.name }

            design?.showToast(
                getString(DesignR.string.toast_profile_updated_failed, name, reason),
                ToastDuration.Long,
            ) {
                setAction(DesignR.string.edit) {
                    startActivity(PropertiesActivity::class.intent.setUUID(uuid))
                }
            }
            design?.fetchProfiles()
        }
    }

    private suspend fun MainComposeDesign.fetchHome() {
        val state = withClash { queryTunnelState() }
        val providers = withClash { queryProviders() }
        val active = withProfile { queryActive() }

        patchHome(
            MainComposeDesign.HomeState(
                clashRunning = clashRunning,
                forwarded = homeState.forwarded,
                mode = state.mode.toText(),
                profileName = active?.name,
                hasProviders = providers.isNotEmpty(),
            )
        )
    }

    private suspend fun MainComposeDesign.fetchTraffic() {
        val forwarded = withClash { queryTrafficTotal() }.trafficTotal()

        patchHome(homeState.copy(forwarded = forwarded))
    }

    private suspend fun MainComposeDesign.fetchProfiles() {
        patchProfiles(withProfile { queryAll() })
    }

    private fun MainComposeDesign.reloadProxyGroup(
        index: Int,
        names: List<String>,
        reloadLock: Semaphore,
    ) {
        launch {
            val group = reloadLock.withPermit {
                withClash {
                    queryProxyGroup(names[index], uiStore.proxySort)
                }
            }

            patchProxyGroup(
                index = index,
                proxies = group.proxies,
                selectable = group.type == "Selector",
                now = group.now,
            )
        }
    }

    private suspend fun MainComposeDesign.startClash() {
        val active = withProfile { queryActive() }

        if (active == null || !active.imported) {
            showToast(DesignR.string.no_profile_selected, ToastDuration.Long) {
                setAction(DesignR.string.profiles) {
                    selectPage(2)
                }
            }

            return
        }

        val vpnRequest = startClashService()

        try {
            if (vpnRequest != null) {
                val result = startActivityForResult(
                    ActivityResultContracts.StartActivityForResult(),
                    vpnRequest,
                )

                if (result.resultCode == RESULT_OK) {
                    startClashService()
                }
            }
        } catch (e: Exception) {
            showToast(DesignR.string.unable_to_start_vpn, ToastDuration.Long)
        }
    }

    private suspend fun MainComposeDesign.requestSave(profile: Profile) {
        showToast(DesignR.string.active_unsaved_tips, ToastDuration.Long) {
            setAction(DesignR.string.edit) {
                startActivity(PropertiesActivity::class.intent.setUUID(profile.uuid))
            }
        }
    }

    private suspend fun updateAllProfiles(design: MainComposeDesign) {
        withProfile {
            try {
                queryAll().forEach { profile ->
                    if (profile.imported && profile.type != Profile.Type.File) {
                        update(profile.uuid)
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    design.patchAllProfilesUpdating(false)
                }
            }
        }
    }

    private suspend fun loadProxyNames(): List<String> {
        if (!clashRunning) return emptyList()

        return withClash {
            queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
        }
    }

    private suspend fun loadOverrideMode(): TunnelState.Mode? {
        if (!clashRunning) return null

        return withClash {
            queryOverride(Clash.OverrideSlot.Session).mode
        }
    }

    private suspend fun patchMode(mode: TunnelState.Mode?) {
        withClash {
            val override = queryOverride(Clash.OverrideSlot.Session)

            override.mode = mode

            patchOverride(Clash.OverrideSlot.Session, override)
        }
    }

    private fun TunnelState.Mode.toText(): String {
        return when (this) {
            TunnelState.Mode.Direct -> getString(DesignR.string.direct_mode)
            TunnelState.Mode.Global -> getString(DesignR.string.global_mode)
            TunnelState.Mode.Rule -> getString(DesignR.string.rule_mode)
            else -> getString(DesignR.string.rule_mode)
        }
    }

    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName +
                "\n" +
                Bridge.nativeCoreVersion().replace("_", "-")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(RequestPermission()) {}
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setupShortcuts()
    }

    private fun setupShortcuts() {
        if (uiStore.hideAppIcon) return

        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
            Intent.FLAG_ACTIVITY_NO_ANIMATION

        val toggle = ShortcutInfoCompat.Builder(this, "toggle_clash")
            .setShortLabel(getString(DesignR.string.shortcut_toggle_short))
            .setLongLabel(getString(DesignR.string.shortcut_toggle_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_all))
            .setIntent(
                Intent(Intents.ACTION_TOGGLE_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(0)
            .build()

        val start = ShortcutInfoCompat.Builder(this, "start_clash")
            .setShortLabel(getString(DesignR.string.shortcut_start_short))
            .setLongLabel(getString(DesignR.string.shortcut_start_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_on))
            .setIntent(
                Intent(Intents.ACTION_START_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(1)
            .build()

        val stop = ShortcutInfoCompat.Builder(this, "stop_clash")
            .setShortLabel(getString(DesignR.string.shortcut_stop_short))
            .setLongLabel(getString(DesignR.string.shortcut_stop_long))
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_toggle_off))
            .setIntent(
                Intent(Intents.ACTION_STOP_CLASH)
                    .setClassName(this, ExternalControlActivity::class.java.name)
                    .addFlags(flags)
            )
            .setRank(2)
            .build()

        ShortcutManagerCompat.setDynamicShortcuts(this, listOf(toggle, start, stop))
    }
}
