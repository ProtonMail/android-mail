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

package ch.protonmail.android.mailcommon.presentation.previewdata

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.contentDescription
import ch.protonmail.android.mailcommon.presentation.model.description
import ch.protonmail.android.mailcommon.presentation.model.iconDrawable
import kotlinx.collections.immutable.toImmutableList

object BottomActionBarsPreviewData {

    val Data = BottomBarState.Data.Shown(
        listOf(
            ActionUiModel(
                Action.MarkUnread,
                Action.MarkUnread.iconDrawable(),
                Action.MarkUnread.description(),
                Action.MarkUnread.contentDescription()
            ),
            ActionUiModel(
                Action.Archive,
                Action.Archive.iconDrawable(),
                Action.Archive.description(),
                Action.Archive.contentDescription()
            ),
            ActionUiModel(
                Action.Trash,
                Action.Trash.iconDrawable(),
                Action.Trash.description(),
                Action.Trash.contentDescription()
            ),
            ActionUiModel(
                Action.Move,
                Action.Move.iconDrawable(),
                Action.Move.description(),
                Action.Move.contentDescription()
            )
        ).toImmutableList()
    )

    val Loading = BottomBarState.Loading

    val Error = BottomBarState.Error.FailedLoadingActions
}

class BottomActionBarPreviewProvider : PreviewParameterProvider<BottomBarState> {

    override val values = sequenceOf(
        BottomActionBarsPreviewData.Data,
        BottomActionBarsPreviewData.Loading,
        BottomActionBarsPreviewData.Error
    )
}
