package com.github.kr328.clash

import android.Manifest.permission.INTERNET
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.util.toAppInfo
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class AccessControlActivity : BaseActivity<AccessControlComposeDesign>() {
    override suspend fun main() {
        val service = ServiceStore(this)

        val allowPackages = withContext(Dispatchers.IO) {
            service.accessControlAllowPackages.toMutableSet()
        }
        val denyPackages = withContext(Dispatchers.IO) {
            service.accessControlDenyPackages.toMutableSet()
        }

        defer {
            withContext(Dispatchers.IO) {
                val changed = allowPackages != service.accessControlAllowPackages ||
                        denyPackages != service.accessControlDenyPackages
                service.accessControlAllowPackages = allowPackages
                service.accessControlDenyPackages = denyPackages
                if (clashRunning && changed) {
                    stopClashService()
                    while (clashRunning) {
                        delay(200)
                    }
                    startClashService()
                }
            }
        }

        migrateLegacyAccessControl(service, allowPackages, denyPackages)

        val design = AccessControlComposeDesign(this, uiStore, allowPackages, denyPackages)

        setContentDesign(design)

        design.requests.send(AccessControlComposeDesign.Request.ReloadApps)

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        AccessControlComposeDesign.Request.ReloadApps -> {
                            design.patchApps(loadApps(design))
                        }

                        AccessControlComposeDesign.Request.RuleChanged ->
                            design.patchApps(loadApps(design))
                    }
                }
            }
        }
    }

    private fun migrateLegacyAccessControl(
        service: ServiceStore,
        allowPackages: MutableSet<String>,
        denyPackages: MutableSet<String>,
    ) {
        if (allowPackages.isNotEmpty() || denyPackages.isNotEmpty()) return

        when (service.accessControlMode) {
            AccessControlMode.AcceptAll -> Unit
            AccessControlMode.AcceptSelected -> allowPackages.addAll(service.accessControlPackages)
            AccessControlMode.DenySelected -> denyPackages.addAll(service.accessControlPackages)
        }
    }

    private suspend fun loadApps(design: AccessControlComposeDesign): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val sort = uiStore.accessControlSort
            val systemApp = uiStore.accessControlSystemApp

            val base = compareByDescending<AppInfo> {
                design.appRule(it.packageName) != AccessControlComposeDesign.AppRule.Default
            }
            val comparator = base.then(sort)

            val pm = packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

            packages.asSequence()
                .filter {
                    it.packageName != packageName
                }
                .filter {
                    it.applicationInfo != null
                }
                .filter {
                    it.requestedPermissions?.contains(INTERNET) == true || it.applicationInfo!!.uid < android.os.Process.FIRST_APPLICATION_UID
                }
                .filter {
                    systemApp || !it.isSystemApp
                }
                .map {
                    it.toAppInfo(pm)
                }
                .sortedWith(comparator)
                .toList()
        }

    private val PackageInfo.isSystemApp: Boolean
        get() {
            return applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
        }
}
