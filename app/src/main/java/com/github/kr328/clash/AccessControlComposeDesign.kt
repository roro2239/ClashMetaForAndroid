package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.ui.ClashMiuixDialog
import com.github.kr328.clash.ui.ClashMiuixMenuItem
import com.github.kr328.clash.ui.ClashMiuixPageScaffold
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

class AccessControlComposeDesign(
    context: Context,
    private val uiStore: UiStore,
    private val selected: MutableSet<String>,
) : Design<AccessControlComposeDesign.Request>(context) {
    enum class Request {
        ReloadApps,
        SelectAll,
        SelectNone,
        SelectInvert,
        Import,
        Export,
    }

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    private var selectedVersion by mutableIntStateOf(0)
    private var menuExpanded by mutableStateOf(false)
    private var searchVisible by mutableStateOf(false)
    private var keyword by mutableStateOf("")

    override val root = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            ClashMiuixTheme {
                PageContent()
            }
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

    @Composable
    private fun PageContent() {
        ClashMiuixPageScaffold(
            title = context.getString(com.github.kr328.clash.design.R.string.access_control_packages),
            backContentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
            onBack = { (context as? Activity)?.onBackPressed() },
            actions = {
                IconButton(onClick = { searchVisible = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.search),
                    )
                }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = context.getString(com.github.kr328.clash.design.R.string.properties),
                    )
                }
                AccessControlMenu()
            },
        ) { innerPadding, nestedScrollConnection ->
            AppsList(
                data = apps,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
            )
        }

        if (searchVisible) {
            SearchDialog()
        }
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
        val checked = app.packageName in selected

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (checked) {
                    selected.remove(app.packageName)
                } else {
                    selected.add(app.packageName)
                }
                selectedVersion = version + 1
            },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                )
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Checkbox(
                    state = if (checked) ToggleableState.On else ToggleableState.Off,
                    onClick = null,
                )
            }
        }
    }

    @Composable
    private fun AccessControlMenu() {
        if (!menuExpanded) return

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.properties),
            onDismissRequest = { menuExpanded = false },
        ) {
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.select_all),
                onClick = { sendMenuRequest(Request.SelectAll) },
            )
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.select_none),
                onClick = { sendMenuRequest(Request.SelectNone) },
            )
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.select_invert),
                onClick = { sendMenuRequest(Request.SelectInvert) },
            )
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.system_apps),
                leadingContent = { Icon(Icons.Default.FilterList, contentDescription = null) },
                trailingContent = {
                    Switch(
                        checked = uiStore.accessControlSystemApp,
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    uiStore.accessControlSystemApp = !uiStore.accessControlSystemApp
                    sendMenuRequest(Request.ReloadApps)
                },
            )
            SortItem(AppInfoSort.Label, com.github.kr328.clash.design.R.string.name)
            SortItem(AppInfoSort.PackageName, com.github.kr328.clash.design.R.string.package_name)
            SortItem(AppInfoSort.InstallTime, com.github.kr328.clash.design.R.string.install_time)
            SortItem(AppInfoSort.UpdateTime, com.github.kr328.clash.design.R.string.update_time)
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.reverse),
                trailingContent = {
                    Switch(
                        checked = uiStore.accessControlReverse,
                        onCheckedChange = null,
                    )
                },
                onClick = {
                    uiStore.accessControlReverse = !uiStore.accessControlReverse
                    sendMenuRequest(Request.ReloadApps)
                },
            )
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.import_from_clipboard),
                leadingContent = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                onClick = { sendMenuRequest(Request.Import) },
            )
            ClashMiuixMenuItem(
                title = context.getString(com.github.kr328.clash.design.R.string.export_to_clipboard),
                leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = { sendMenuRequest(Request.Export) },
            )
        }
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
    private fun SearchDialog() {
        val filtered = if (keyword.isBlank()) {
            emptyList()
        } else {
            apps.filter {
                it.label.contains(keyword, ignoreCase = true) ||
                        it.packageName.contains(keyword, ignoreCase = true)
            }
        }

        ClashMiuixDialog(
            title = context.getString(com.github.kr328.clash.design.R.string.search),
            confirmText = context.getString(com.github.kr328.clash.design.R.string.close),
            onConfirm = { closeSearch() },
            onDismissRequest = { closeSearch() },
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    TextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        singleLine = true,
                        label = context.getString(com.github.kr328.clash.design.R.string.search),
                    )
                }
                items(filtered, key = AppInfo::packageName) { app ->
                    AppItem(app, selectedVersion)
                }
            }
        }
    }

    private fun sendMenuRequest(request: Request) {
        menuExpanded = false
        requests.trySend(request)
    }

    private fun closeSearch() {
        searchVisible = false
        keyword = ""
        selectedVersion++
    }
}
