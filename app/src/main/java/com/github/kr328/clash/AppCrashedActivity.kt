package com.github.kr328.clash

import com.github.kr328.clash.common.compat.versionCodeCompat
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.log.SystemLogcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.github.kr328.clash.design.R as DesignR

class AppCrashedActivity : BaseActivity<SimpleComposeDesign>() {
    override suspend fun main() {
        val design = SimpleComposeDesign(this, title = getString(DesignR.string.application_name))

        setContentDesign(design)

        val packageInfo = withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0)
        }

        Log.i("App version: versionName = ${packageInfo.versionName} versionCode = ${packageInfo.versionCodeCompat}")

        val logs = withContext(Dispatchers.IO) {
            SystemLogcat.dumpCrash()
        }

        design.patchBodyText(logs)

        while (isActive) {
            events.receive()
        }
    }
}
