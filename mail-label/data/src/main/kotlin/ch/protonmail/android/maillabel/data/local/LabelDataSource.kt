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

package ch.protonmail.android.maillabel.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalSystemLabel
import ch.protonmail.android.mailcommon.domain.model.DataError
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.SidebarCustomFolder
import uniffi.mail_uniffi.SidebarCustomLabel
import uniffi.mail_uniffi.SidebarSystemLabel
import uniffi.mail_uniffi.SystemLabel

interface LabelDataSource {

    fun observeSystemLabels(userId: UserId): Flow<List<SidebarSystemLabel>>

    fun observeMessageLabels(userId: UserId): Flow<List<SidebarCustomLabel>>

    fun observeMessageFolders(userId: UserId): Flow<List<SidebarCustomFolder>>

    suspend fun getAllMailLabelId(userId: UserId): Either<DataError, LocalLabelId>

    suspend fun resolveSystemLabelByLocalId(userId: UserId, labelId: LocalLabelId): Either<DataError, LocalSystemLabel>

    suspend fun resolveLocalIdBySystemLabel(userId: UserId, systemLabel: SystemLabel): Either<DataError, LocalLabelId>
}
