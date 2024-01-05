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

package ch.protonmail.android.maillabel.presentation.sidebar

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction.Select
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarSystemLabelTestTags.BaseTag
import me.proton.core.compose.component.ProtonSidebarLazy
import me.proton.core.compose.theme.ProtonTheme

fun LazyListScope.sidebarSystemLabelItems(
    items: List<MailLabelUiModel.System>,
    onLabelAction: (SidebarLabelAction) -> Unit
) {
    items(items = items, key = { it.id.labelId.id }) {
        SidebarSystemLabel(
            modifier = Modifier.testTag("$BaseTag#${it.id.labelId.id}"),
            item = it,
            onLabelAction = onLabelAction
        )
    }
}

@Composable
private fun SidebarSystemLabel(
    modifier: Modifier = Modifier,
    item: MailLabelUiModel.System,
    onLabelAction: (SidebarLabelAction) -> Unit
) {
    SidebarItemWithCounter(
        modifier = modifier,
        icon = painterResource(item.icon),
        text = stringResource(item.text.value),
        count = item.count,
        isSelected = item.isSelected,
        onClick = { onLabelAction(Select(item.id)) }
    )
}

@SuppressLint("VisibleForTests")
@Preview(
    name = "Sidebar System Labels in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Sidebar System Labels in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewSidebarSystemLabelItems() {
    ProtonTheme {
        ProtonSidebarLazy {
            sidebarSystemLabelItems(
                items = MailLabelsUiModel.PreviewForTesting.systems,
                onLabelAction = {}
            )
        }
    }
}

object SidebarSystemLabelTestTags {

    const val BaseTag = "SidebarSystemLabel"
}
