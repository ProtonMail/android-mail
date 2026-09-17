/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */
package ch.protonmail.android.mailsidebar.presentation.common

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.design.compose.component.ProtonListItem
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonSidebarTheme
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodySmallHint
import ch.protonmail.android.mailsidebar.presentation.R
import kotlinx.coroutines.launch

@Composable
fun ProtonSidebar(
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    content: (@Composable @ExtensionFunctionType ColumnScope.() -> Unit)
) {
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ProtonSidebarTheme {
        Surface(
            color = ProtonTheme.colors.sidebarBackground,
            contentColor = ProtonTheme.colors.sidebarTextNorm,
            modifier = modifier.fillMaxSize()
        ) {
            val state = rememberScrollState()

            Column(
                modifier = Modifier.verticalScroll(enabled = true, state = state),
                content = content
            )
        }
    }
}

@Composable
fun ProtonSidebarSettingsItem(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    ProtonSidebarItem(
        text = R.string.presentation_menu_item_title_settings,
        icon = R.drawable.ic_proton_cog_wheel,
        modifier = modifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = false
    )
}

@Composable
fun ProtonSidebarSubscriptionItem(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    ProtonSidebarItem(
        text = R.string.presentation_menu_item_title_subscription,
        icon = R.drawable.ic_proton_pencil,
        modifier = modifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = false
    )
}

@Composable
fun ProtonSidebarReportBugItem(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    ProtonSidebarItem(
        text = R.string.drawer_title_bug_report,
        icon = R.drawable.ic_proton_exclamation_circle,
        modifier = modifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = false
    )
}

@Composable
fun ProtonSidebarApplicationLogsItem(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit = {}
) {
    ProtonSidebarItem(
        text = R.string.drawer_title_application_logs,
        icon = R.drawable.ic_proton_code,
        modifier = modifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = false
    )
}

@Composable
fun ProtonSidebarItem(
    @DrawableRes icon: Int,
    @StringRes text: Int,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    isClickable: Boolean = true,
    isSelected: Boolean = false,
    textColor: Color = if (isSelected) ProtonTheme.colors.textNorm else Color.Unspecified,
    iconTint: Color = if (isSelected) ProtonTheme.colors.iconNorm else ProtonTheme.colors.iconWeak,
    count: String = "",
    onClick: () -> Unit = {}
) {
    ProtonListItem(
        icon = icon,
        text = text,
        modifier = modifier,
        iconModifier = iconModifier,
        textModifier = textModifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = isSelected,
        textColor = textColor,
        iconTint = iconTint,
        count = count
    )
}

@Composable
fun ProtonSidebarItem(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    isClickable: Boolean = true,
    isSelected: Boolean = false,
    textColor: Color = Color.Unspecified,
    iconTint: Color = ProtonTheme.colors.iconWeak,
    count: String = "",
    onClick: () -> Unit = {}
) {
    ProtonListItem(
        icon = icon,
        text = text,
        modifier = modifier,
        iconModifier = iconModifier,
        textModifier = textModifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = isSelected,
        textColor = textColor,
        iconTint = iconTint,
        count = count
    )
}

@Composable
fun ProtonSidebarItem(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    content: (@Composable @ExtensionFunctionType RowScope.() -> Unit)
) {
    ProtonListItem(
        modifier = modifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = isSelected,
        content = content
    )
}

@Composable
fun ProtonSidebarAppVersionItem(
    name: String,
    version: String,
    sdkVersion: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ProtonDimens.Spacing.ExtraLarge,
                end = ProtonDimens.Spacing.ExtraLarge,
                bottom = ProtonDimens.Spacing.ExtraLarge
            ),
        text = "$name $version - $sdkVersion",
        textAlign = TextAlign.Center,
        color = ProtonTheme.colors.textWeak,
        style = ProtonTheme.typography.bodySmallHint,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Preview(
    name = "Sidebar in light mode",
    uiMode = UI_MODE_NIGHT_NO,
    showBackground = true
)
@Preview(
    name = "Sidebar in dark mode",
    uiMode = UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun PreviewProtonSidebar() {
    ProtonTheme {
        ProtonSidebar(
            drawerState = DrawerState(DrawerValue.Open) { true }
        ) {
            ProtonSidebarItem { Text(text = "Inbox") }
            ProtonSidebarItem { Text(text = "Drafts") }
            ProtonSidebarItem { Text(text = "Sent") }
            ProtonSidebarItem(isSelected = true) { Text(text = "Trash (active)") }
            ProtonSidebarItem { Text(text = "All mail") }

            HorizontalDivider()

            ProtonSidebarItem { Text(text = "More", color = ProtonTheme.colors.textHint) }
            ProtonSidebarSettingsItem()
            ProtonSidebarSubscriptionItem()
            ProtonSidebarReportBugItem()

            ProtonSidebarAppVersionItem(name = "App Name", version = "0.0.7", sdkVersion = "0.7.0")
        }
    }
}
