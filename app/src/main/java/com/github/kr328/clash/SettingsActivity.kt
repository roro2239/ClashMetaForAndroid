package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class SettingsActivity : BaseActivity<SettingsComposeDesign>() {
    override suspend fun main() {
        val design = SettingsComposeDesign(this)

        setContentDesign(design)

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                design.requests.onReceive {
                    when (it) {
                        SettingsComposeDesign.Request.StartApp ->
                            startActivity(AppSettingsActivity::class.intent)
                        SettingsComposeDesign.Request.StartNetwork ->
                            startActivity(NetworkSettingsActivity::class.intent)
                        SettingsComposeDesign.Request.StartOverride ->
                            startActivity(OverrideSettingsActivity::class.intent)
                        SettingsComposeDesign.Request.StartMetaFeature ->
                            startActivity(MetaFeatureSettingsActivity::class.intent)
                    }
                }
            }
        }
    }
}
