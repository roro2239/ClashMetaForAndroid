package com.github.kr328.clash

import android.content.Context
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixMenuItem
import com.github.kr328.clash.ui.ClashMiuixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

class AccessControlComposeDesign(
    context: Context,
    private val uiStore: UiStore,
    private val appRules: MutableMap<String, AppRule>,
) : Design<AccessControlComposeDesign.Request>(context) {
    enum class Request {
        ReloadApps,
        RuleChanged,
    }

    enum class AppRule {
        Reject,
        Default,
        Rule,
        Global,
        Direct,
    }

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    private var selectedVersion by mutableIntStateOf(0)
    private var menuExpanded by mutableStateOf(false)
    private var keyword by mutableStateOf("")
    private var choosingApp by mutableStateOf<AppInfo?>(null)

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent(
                    innerPadding = PaddingValues(0.dp),
                    nestedScrollConnection = null,
                )
            }
        }
    }

    @Composable
    fun PageContent(
        innerPadding: PaddingValues,
        nestedScrollConnection: NestedScrollConnection?,
    ) {
        val filteredApps = if (keyword.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(keyword, ignoreCase = true) ||
                        it.packageName.contains(keyword, ignoreCase = true)
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TextField(
                value = keyword,
                onValueChange = { keyword = it },
                singleLine = true,
                label = context.getString(com.github.kr328.clash.design.R.string.search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        end = 20.dp,
                        bottom = 8.dp,
                    ),
            )

            AppsList(
                data = filteredApps,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 4.dp,
                    end = 20.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp,
                ),
            )
        }

        AccessControlMenu()
        RuleDialog()
    }

    @Composable
    fun MenuAction() {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "菜单",
            )
        }
    }

    suspend fun patchApps(apps: List<AppInfo>) {
        withContext(Dispatchers.Main) {
            this@AccessControlComposeDesign.apps = apps
        }
    }

    suspend fun rebindAll() {
        withContext(Dispatchers.Main) {
            selectedVersion++
        }
    }

    fun appRule(packageName: String): AppRule {
        return appRules[packageName] ?: AppRule.Default
    }

    fun rules(): Map<String, AppRule> = appRules.toMap()

    fun replaceRules(rules: Map<String, AppRule>) {
        appRules.clear()
        appRules.putAll(rules)
        selectedVersion++
    }

    private fun setRule(packageName: String, rule: AppRule) {
        when (rule) {
            AppRule.Default -> appRules.remove(packageName)
            else -> appRules[packageName] = rule
        }
        selectedVersion++
        requests.trySend(Request.RuleChanged)
    }

    @Composable
    private fun AppsList(
        data: List<AppInfo>,
        modifier: Modifier,
        contentPadding: PaddingValues,
    ) {
        LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(data, key = AppInfo::packageName) { app ->
                AppItem(app, selectedVersion)
            }
        }
    }

    @Composable
    private fun AppItem(app: AppInfo, version: Int) {
        val rule = appRule(app.packageName)

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { choosingApp = app },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = app.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName,
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = ruleText(rule),
                    color = if (rule == AppRule.Default) {
                        MiuixTheme.colorScheme.onSurfaceContainerVariant
                    } else {
                        MiuixTheme.colorScheme.primary
                    },
                    fontWeight = if (rule == AppRule.Default) FontWeight.Normal else FontWeight.SemiBold,
                )
            }
        }
    }

    @Composable
    private fun AppIcon(app: AppInfo) {
        Box(modifier = Modifier.size(44.dp)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { view ->
                    view.setImageDrawable(app.icon)
                },
            )
        }
    }

    @Composable
    private fun AccessControlMenu() {
        if (!menuExpanded) return

        ClashMiuixDialog(
            title = "菜单",
            onDismissRequest = { menuExpanded = false },
        ) {
            SystemAppItem()
            SortItem(AppInfoSort.Label, com.github.kr328.clash.design.R.string.name)
            SortItem(AppInfoSort.PackageName, com.github.kr328.clash.design.R.string.package_name)
            SortItem(AppInfoSort.InstallTime, com.github.kr328.clash.design.R.string.install_time)
            SortItem(AppInfoSort.UpdateTime, com.github.kr328.clash.design.R.string.update_time)
        }
    }

    @Composable
    private fun SystemAppItem() {
        ClashMiuixMenuItem(
            title = context.getString(com.github.kr328.clash.design.R.string.system_apps),
            trailingContent = {
                Switch(
                    checked = uiStore.accessControlSystemApp,
                    onCheckedChange = {
                        uiStore.accessControlSystemApp = it
                        sendMenuRequest(Request.ReloadApps)
                    },
                )
            },
            onClick = {
                uiStore.accessControlSystemApp = !uiStore.accessControlSystemApp
                sendMenuRequest(Request.ReloadApps)
            },
        )
    }

    @Composable
    private fun SortItem(sort: AppInfoSort, title: Int) {
        ClashMiuixMenuItem(
            title = context.getString(title),
            trailingContent = {
                Checkbox(
                    state = if (uiStore.accessControlSort == sort) ToggleableState.On else ToggleableState.Off,
                    onClick = null,
                )
            },
            onClick = {
                uiStore.accessControlSort = sort
                sendMenuRequest(Request.ReloadApps)
            },
        )
    }

    @Composable
    private fun RuleDialog() {
        val app = choosingApp ?: return

        ClashMiuixDialog(
            title = app.label,
            dismissText = context.getString(com.github.kr328.clash.design.R.string.cancel),
            onDismissButton = { choosingApp = null },
            onDismissRequest = { choosingApp = null },
        ) {
            AppRule.entries.forEach { rule ->
                ClashMiuixMenuItem(
                    title = ruleText(rule),
                    trailingContent = {
                        Checkbox(
                            state = if (appRule(app.packageName) == rule) ToggleableState.On else ToggleableState.Off,
                            onClick = null,
                        )
                    },
                    onClick = {
                        setRule(app.packageName, rule)
                        choosingApp = null
                    },
                )
            }
        }
    }

    private fun sendMenuRequest(request: Request) {
        menuExpanded = false
        requests.trySend(request)
    }

    private fun ruleText(rule: AppRule): String {
        return when (rule) {
            AppRule.Reject -> "拒绝"
            AppRule.Default -> "默认"
            AppRule.Rule -> "规则"
            AppRule.Global -> "全局"
            AppRule.Direct -> "直连"
        }
    }
}
