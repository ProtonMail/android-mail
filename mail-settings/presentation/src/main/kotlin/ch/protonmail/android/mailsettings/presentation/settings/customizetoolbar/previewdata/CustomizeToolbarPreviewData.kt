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

package ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.previewdata

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.CustomizeToolbarState
import ch.protonmail.android.mailsettings.presentation.settings.customizetoolbar.ToolbarActionUiModel

internal object CustomizeToolbarPreviewData {

    val ActionEnabled = action(1)
    val ActionDisabled = action(1).copy(enabled = false)

    private val SelectedActions = (0..3).map { action(it) }
    private val UnselectedActions = (10..13).map { action(it) }

    private val Page = CustomizeToolbarState.Data.Page(
        TextUiModel.Text("This toolbar is visible when reading a message."),
        SelectedActions, UnselectedActions
    )
    private val PageDisabledAdd = CustomizeToolbarState.Data.Page(
        TextUiModel.Text("This toolbar is visible when reading a message."),
        SelectedActions,
        UnselectedActions.map {
            it.copy(enabled = false)
        }
    )
    private val PageDisabledRemove = CustomizeToolbarState.Data.Page(
        TextUiModel.Text("This toolbar is visible when reading a message."),
        SelectedActions.map { it.copy(enabled = false) },
        UnselectedActions
    )

    private val TabTitles = listOf(TextUiModel.Text("Message"), TextUiModel.Text("Mailbox"))

    val Normal = CustomizeToolbarState.Data(
        pages = listOf(Page, Page),
        tabs = TabTitles,
        selectedTabIdx = 0
    )
    val DisabledAdd = CustomizeToolbarState.Data(
        pages = listOf(Page, PageDisabledAdd),
        tabs = TabTitles,
        selectedTabIdx = 1
    )
    val DisabledARemove = CustomizeToolbarState.Data(
        pages = listOf(PageDisabledRemove, Page),
        tabs = TabTitles,
        selectedTabIdx = 0
    )

    fun action(id: Int) = ToolbarActionUiModel(
        id = id.toString(),
        description = TextUiModel.Text("Action $id"),
        icon = R.drawable.ic_proton_reply,
        enabled = true
    )
}

internal data class CustomizeToolbarPreview(
    val uiModel: CustomizeToolbarState
)

internal data class ToolbarActionPreview(
    val uiModel: ToolbarActionUiModel
)

internal class CustomizeToolbarPreviewProvider : PreviewParameterProvider<CustomizeToolbarPreview> {

    override val values = sequenceOf(
        CustomizeToolbarPreview(CustomizeToolbarState.NotLoggedIn),
        CustomizeToolbarPreview(CustomizeToolbarState.Loading),
        CustomizeToolbarPreview(CustomizeToolbarPreviewData.Normal),
        CustomizeToolbarPreview(CustomizeToolbarPreviewData.DisabledAdd),
        CustomizeToolbarPreview(CustomizeToolbarPreviewData.DisabledARemove)
    )
}

internal class SelectedToolbarActionPreviewProvider : PreviewParameterProvider<ToolbarActionPreview> {

    override val values = sequenceOf(
        ToolbarActionPreview(CustomizeToolbarPreviewData.ActionEnabled),
        ToolbarActionPreview(CustomizeToolbarPreviewData.ActionDisabled)
    )
}
