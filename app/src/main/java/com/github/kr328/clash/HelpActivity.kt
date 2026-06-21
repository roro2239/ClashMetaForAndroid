package com.github.kr328.clash

import android.content.Intent
import kotlinx.coroutines.isActive
import com.github.kr328.clash.design.R as DesignR

class HelpActivity : BaseActivity<SimpleComposeDesign>() {
    override suspend fun main() {
        val design = SimpleComposeDesign(
            this,
            title = getString(DesignR.string.help),
            entries = listOf(
                SimpleComposeDesign.Entry.Tip(getString(DesignR.string.tips_help)),
                SimpleComposeDesign.Entry.Category(getString(DesignR.string.document)),
                SimpleComposeDesign.Entry.Link(
                    title = getString(DesignR.string.clash_wiki),
                    summary = getString(DesignR.string.clash_wiki_url),
                    url = getString(DesignR.string.clash_wiki_url),
                ),
                SimpleComposeDesign.Entry.Link(
                    title = getString(DesignR.string.clash_meta_wiki),
                    summary = getString(DesignR.string.clash_meta_wiki_url),
                    url = getString(DesignR.string.clash_meta_wiki_url),
                ),
                SimpleComposeDesign.Entry.Category(getString(DesignR.string.sources)),
                SimpleComposeDesign.Entry.Link(
                    title = getString(DesignR.string.clash_meta_core),
                    summary = getString(DesignR.string.clash_meta_core_url),
                    url = getString(DesignR.string.clash_meta_core_url),
                ),
                SimpleComposeDesign.Entry.Link(
                    title = getString(DesignR.string.clash_meta_for_android),
                    summary = getString(DesignR.string.meta_github_url),
                    url = getString(DesignR.string.meta_github_url),
                ),
            ),
        )

        setContentDesign(design)

        while (isActive) {
            when (val request = design.requests.receive()) {
                is SimpleComposeDesign.Request.OpenUrl ->
                    startActivity(Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse(request.url)))
            }
        }
    }
}
