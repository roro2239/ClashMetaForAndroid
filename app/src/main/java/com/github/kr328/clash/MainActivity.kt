package com.github.kr328.clash

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.github.kr328.clash.service.util.pendingDir
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.QRResult.QRError
import io.github.g00fy2.quickie.QRResult.QRMissingPermission
import io.github.g00fy2.quickie.QRResult.QRSuccess
import io.github.g00fy2.quickie.QRResult.QRUserCanceled
import io.github.g00fy2.quickie.ScanQRCode
import java.io.FileNotFoundException
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
    private val scanLauncher = registerForActivityResult(ScanQRCode(), ::scanResultHandler)

    override suspend fun main() {
        val design = MainComposeDesign(this)

        setContentDesign(design)

        val proxyReloadLock = Semaphore(10)
        var proxyNames = emptyList<String>()

        suspend fun reloadProxyNamesAndGroups() {
            proxyNames = loadProxyNames()
            design.patchProxyGroups(proxyNames)
            proxyNames.indices.forEach { index ->
                design.reloadProxyGroup(index, proxyNames, proxyReloadLock)
            }
        }

        design.patchOverrideMode(loadOverrideMode())
        design.fetchHome()
        design.fetchProfiles()
        reloadProxyNamesAndGroups()

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
                        Event.ActivityStart,
                        Event.ServiceRecreated,
                        Event.ClashStop,
                        Event.ClashStart,
                        Event.ProfileLoaded,
                        Event.ProfileChanged -> {
                            design.patchOverrideMode(loadOverrideMode())
                            reloadProxyNamesAndGroups()
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
                        MainComposeDesign.Request.PickProfileFile -> {
                            val uri = startActivityForResult(
                                ActivityResultContracts.GetContent(),
                                "*/*",
                            )

                            if (uri != null) {
                                design.patchPickedProfileFile(uri, queryDisplayName(uri))
                            }
                        }
                        MainComposeDesign.Request.LaunchProfileScanner ->
                            scanLauncher.launch(null)
                        is MainComposeDesign.Request.CreateProfileUrl ->
                            createUrlProfile(design, it)
                        is MainComposeDesign.Request.CreateProfileFile ->
                            createFileProfile(design, it)
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

    private suspend fun createUrlProfile(
        design: MainComposeDesign,
        request: MainComposeDesign.Request.CreateProfileUrl,
    ) {
        if (!request.url.isValidProfileUrl()) {
            design.showToast(DesignR.string.invalid_url, ToastDuration.Long)
            return
        }

        withProfile {
            val uuid = create(Profile.Type.Url, request.name, request.url, null)

            if (request.interval > 0) {
                patch(uuid, request.name, request.url, request.interval, null)
            }
            commit(uuid) {}
        }
        design.fetchProfiles()
    }

    private suspend fun createFileProfile(
        design: MainComposeDesign,
        request: MainComposeDesign.Request.CreateProfileFile,
    ) {
        val uuid = withProfile {
            create(Profile.Type.File, request.name, "", null)
        }

        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(request.uri).use { input ->
                if (input == null) {
                    throw FileNotFoundException(request.uri.toString())
                }

                val target = pendingDir
                    .resolve(uuid.toString())
                    .resolve("config.yaml")

                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        withProfile {
            commit(uuid) {}
        }
        design.fetchProfiles()
    }

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val name = cursor.getString(index)
                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank {
            getString(DesignR.string.new_profile)
        } ?: getString(DesignR.string.new_profile)
    }

    private fun String.isValidProfileUrl(): Boolean {
        val scheme = Uri.parse(this).scheme

        return scheme == "http" || scheme == "https"
    }

    private fun scanResultHandler(result: QRResult) {
        launch {
            when (result) {
                is QRSuccess -> {
                    val url = result.content.rawValue
                        ?: result.content.rawBytes?.let { String(it) }.orEmpty()

                    design?.patchScannedProfileUrl(url)
                }

                QRUserCanceled -> {}
                QRMissingPermission -> design?.showToast(
                    DesignR.string.import_from_qr_no_permission,
                    ToastDuration.Long,
                )
                is QRError -> design?.showToast(
                    DesignR.string.import_from_qr_exception,
                    ToastDuration.Long,
                )
            }
        }
    }

    private suspend fun MainComposeDesign.fetchHome() {
        val active = withProfile { queryActive() }
        if (active == null || !active.imported) {
            patchHome(
                MainComposeDesign.HomeState(
                    clashRunning = clashRunning,
                    forwarded = homeState.forwarded,
                )
            )
            return
        }

        val state = withClash { queryTunnelState() }
        val providers = withClash { queryProviders() }

        patchHome(
            MainComposeDesign.HomeState(
                clashRunning = clashRunning,
                forwarded = homeState.forwarded,
                mode = state.mode.toText(),
                profileName = active.name,
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
        val active = withProfile { queryActive() }
        if (active == null || !active.imported) return emptyList()

        return withClash {
            queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
        }
    }

    private suspend fun loadOverrideMode(): TunnelState.Mode? {
        return withClash {
            queryOverride(Clash.OverrideSlot.Persist).mode
        }
    }

    private suspend fun patchMode(mode: TunnelState.Mode?) {
        withClash {
            val persist = queryOverride(Clash.OverrideSlot.Persist)
            val session = queryOverride(Clash.OverrideSlot.Session)

            persist.mode = mode
            session.mode = mode

            patchOverride(Clash.OverrideSlot.Persist, persist)
            patchOverride(Clash.OverrideSlot.Session, session)
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
