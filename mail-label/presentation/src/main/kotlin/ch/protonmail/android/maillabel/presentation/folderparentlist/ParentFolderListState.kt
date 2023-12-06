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

package ch.protonmail.android.maillabel.presentation.folderparentlist

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.model.ParentFolderUiModel
import me.proton.core.label.domain.entity.LabelId

sealed interface ParentFolderListState {

    data class Loading(
        val errorLoading: Effect<TextUiModel> = Effect.empty()
    ) : ParentFolderListState

    sealed interface ListLoaded : ParentFolderListState {

        val labelId: LabelId?
        val parentLabelId: LabelId?

        data class Data(
            override val labelId: LabelId?,
            override val parentLabelId: LabelId?,
            val useFolderColor: Boolean,
            val inheritParentFolderColor: Boolean,
            val folders: List<ParentFolderUiModel>
        ) : ListLoaded

        data class Empty(
            override val labelId: LabelId?,
            override val parentLabelId: LabelId?
        ) : ListLoaded
    }
}
