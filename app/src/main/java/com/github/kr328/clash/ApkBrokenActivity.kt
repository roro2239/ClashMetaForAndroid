package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.isActive
import com.github.kr328.clash.design.R as DesignR

class ApkBrokenActivity : BaseActivity<SimpleComposeDesign>() {
    override suspend fun main() {
        val design = SimpleComposeDesign(
            this,
            title = getString(DesignR.string.application_name),
            entries = listOf(
                SimpleComposeDesign.Entry.Tip(getString(DesignR.string.application_broken_tips)),
                SimpleComposeDesign.Entry.Category(getString(DesignR.string.reinstall)),
                SimpleComposeDesign.Entry.Link(
                    title = getString(DesignR.string.github_releases),
                    summary = getString(DesignR.string.meta_github_url),
                    url = getString(DesignR.string.meta_github_url),
                ),
            ),
        )

        setContentDesign(design)

        while (isActive) {
            when (val request = design.requests.receive()) {
                is SimpleComposeDesign.Request.OpenUrl ->
                    startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(request.url)))
            }
        }
    }
}
