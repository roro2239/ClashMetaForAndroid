package com.github.kr328.clash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun ClashMiuixDialog(
    show: Boolean = true,
    title: String,
    message: String? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismissButton: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val navigationOwner = rememberNavigationEventDispatcherOwner(parent = null)

    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides navigationOwner,
    ) {
        WindowDialog(
            show = show,
            title = title,
            onDismissRequest = onDismissRequest,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (message != null) {
                    Text(
                        text = message,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }

                content()

                if (confirmText != null || dismissText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (dismissText != null && onDismissButton != null) {
                            TextButton(
                                text = dismissText,
                                onClick = onDismissButton,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (confirmText != null && onConfirm != null) {
                            TextButton(
                                text = confirmText,
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClashMiuixMenuItem(
    title: String,
    onClick: () -> Unit,
    titleColor: Color = Color.Unspecified,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = titleColor,
            )
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
