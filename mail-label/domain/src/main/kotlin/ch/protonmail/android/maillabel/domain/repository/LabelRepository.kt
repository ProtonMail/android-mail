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

package ch.protonmail.android.maillabel.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.maillabel.domain.model.LabelWithSystemLabelId
import ch.protonmail.android.maillabel.domain.model.NewLabel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface LabelRepository {

    fun observeCustomLabels(userId: UserId): Flow<List<Label>>

    fun observeCustomFolders(userId: UserId): Flow<List<Label>>

    fun observeSystemLabels(userId: UserId): Flow<List<LabelWithSystemLabelId>>

    /**
     * Get a [Label] for [userId], by [type] and [labelId].
     */
    suspend fun getLabel(
        userId: UserId,
        type: LabelType,
        labelId: LabelId,
        refresh: Boolean = false
    ): Label?

    /**
     * Returns the local label id of AllMail or AlmostAllMail depending on user's settings
     */
    suspend fun getAllMailLocalLabelId(userId: UserId): LabelId?

    /**
     * Create a new [Label] for [userId], remotely, then locally if success.
     */
    suspend fun createLabel(userId: UserId, label: NewLabel)

    /**
     * Update [Label] for [userId], locally, then remotely in background.
     */
    suspend fun updateLabel(userId: UserId, label: Label)

    /**
     * Update [isExpanded] for [userId], locally, then remotely in background.
     */
    suspend fun updateLabelIsExpanded(
        userId: UserId,
        type: LabelType,
        labelId: LabelId,
        isExpanded: Boolean
    )

    /**
     * Delete label for [userId] by [labelId], locally, then remotely in background.
     */
    suspend fun deleteLabel(
        userId: UserId,
        type: LabelType,
        labelId: LabelId
    )

    suspend fun resolveSystemLabel(userId: UserId, labelId: LabelId): Either<DataError, SystemLabelId>

    suspend fun resolveLocalIdBySystemLabel(userId: UserId, labelId: SystemLabelId): Either<DataError, LabelId>

    suspend fun markCategoryLabelSeen(userId: UserId, categoryLabelId: CategoryLabelId): Either<DataError, Unit>
}
