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

package ch.protonmail.android.maillabel.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.Sidebar
import uniffi.mail_uniffi.SidebarAllCustomFoldersResult
import uniffi.mail_uniffi.SidebarCustomFolder
import uniffi.mail_uniffi.SidebarCustomLabel
import uniffi.mail_uniffi.SidebarCustomLabelsResult
import uniffi.mail_uniffi.SidebarSystemLabel
import uniffi.mail_uniffi.SidebarSystemLabelsResult
import uniffi.mail_uniffi.SidebarWatchLabelsResult
import uniffi.mail_uniffi.WatchHandle

class SidebarWrapper(private val sidebar: Sidebar) {

    suspend fun watchLabels(callback: LiveQueryCallback): Either<DataError, WatchHandle> =
        when (val result = sidebar.watchLabels(callback)) {
            is SidebarWatchLabelsResult.Error -> result.v1.toDataError().left()
            is SidebarWatchLabelsResult.Ok -> result.v1.right()
        }

    suspend fun systemLabels(): Either<DataError, List<SidebarSystemLabel>> =
        when (val result = sidebar.systemLabels()) {
            is SidebarSystemLabelsResult.Error -> result.v1.toDataError().left()
            is SidebarSystemLabelsResult.Ok -> result.v1.right()
        }

    suspend fun customLabels(): Either<DataError, List<SidebarCustomLabel>> =
        when (val result = sidebar.customLabels()) {
            is SidebarCustomLabelsResult.Error -> result.v1.toDataError().left()
            is SidebarCustomLabelsResult.Ok -> result.v1.right()
        }

    suspend fun allCustomFolders(): Either<DataError, List<SidebarCustomFolder>> =
        when (val result = sidebar.allCustomFolders()) {
            is SidebarAllCustomFoldersResult.Error -> result.v1.toDataError().left()
            is SidebarAllCustomFoldersResult.Ok -> result.v1.right()
        }

    fun destroy() {
        sidebar.destroy()
    }
}
