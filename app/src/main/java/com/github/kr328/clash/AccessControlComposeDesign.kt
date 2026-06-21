package com.github.kr328.clash

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.Design
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.store.UiStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            PageTheme {
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
    private fun PageTheme(content: @Composable () -> Unit) {
        val colors = if (androidx.compose.foundation.isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }

        MaterialTheme(colorScheme = colors, content = content)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PageContent() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(com.github.kr328.clash.design.R.string.access_control_packages)) },
                    navigationIcon = {
                        IconButton(onClick = { (context as? Activity)?.onBackPressed() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(com.github.kr328.clash.design.R.string.close),
                            )
                        }
                    },
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
                )
            },
        ) { innerPadding ->
            AppsList(
                data = apps,
                modifier = Modifier.fillMaxSize(),
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AppItem(app: AppInfo, version: Int) {
        val checked = app.packageName in selected

        Card(
            onClick = {
                if (checked) {
                    selected.remove(app.packageName)
                } else {
                    selected.add(app.packageName)
                }
                selectedVersion = version + 1
            },
        ) {
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.Apps, contentDescription = null)
                },
                headlineContent = {
                    Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                supportingContent = {
                    Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = null,
                    )
                },
            )
        }
    }

    @Composable
    private fun AccessControlMenu() {
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.select_all)) },
                onClick = { sendMenuRequest(Request.SelectAll) },
            )
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.select_none)) },
                onClick = { sendMenuRequest(Request.SelectNone) },
            )
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.select_invert)) },
                onClick = { sendMenuRequest(Request.SelectInvert) },
            )
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.system_apps)) },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                trailingIcon = {
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
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.reverse)) },
                trailingIcon = {
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
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.import_from_clipboard)) },
                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                onClick = { sendMenuRequest(Request.Import) },
            )
            DropdownMenuItem(
                text = { Text(context.getString(com.github.kr328.clash.design.R.string.export_to_clipboard)) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = { sendMenuRequest(Request.Export) },
            )
        }
    }

    @Composable
    private fun SortItem(sort: AppInfoSort, title: Int) {
        DropdownMenuItem(
            text = { Text(context.getString(title)) },
            trailingIcon = {
                Checkbox(
                    checked = uiStore.accessControlSort == sort,
                    onCheckedChange = null,
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

        AlertDialog(
            onDismissRequest = { closeSearch() },
            confirmButton = {
                TextButton(onClick = { closeSearch() }) {
                    Text(context.getString(com.github.kr328.clash.design.R.string.close))
                }
            },
            title = { Text(context.getString(com.github.kr328.clash.design.R.string.search)) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        TextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            singleLine = true,
                            label = { Text(context.getString(com.github.kr328.clash.design.R.string.search)) },
                        )
                    }
                    items(filtered, key = AppInfo::packageName) { app ->
                        AppItem(app, selectedVersion)
                    }
                }
            },
        )
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
