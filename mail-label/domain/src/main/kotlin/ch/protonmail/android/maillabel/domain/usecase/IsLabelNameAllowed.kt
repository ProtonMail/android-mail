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

package ch.protonmail.android.maillabel.domain.usecase

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.util.kotlin.equalsNoCase
import timber.log.Timber
import javax.inject.Inject

class IsLabelNameAllowed @Inject constructor(
    private val getLabels: GetLabels
) {

    suspend operator fun invoke(
        userId: UserId,
        name: String,
        parentId: LabelId? = null
    ): Either<DataError, Boolean> = Either.catch {
        if (parentId != null) {
            noSubFolderWithSameName(userId, name, parentId)
        } else {
            noSystemFolderWithSameName(name) &&
                noLabelWithSameName(userId, name) &&
                noFolderWithSameName(userId, name)
        }
    }.mapLeft {
        val error = when (it) {
            is UnknownHostException -> NetworkError.NoNetwork
            is SocketTimeoutException -> NetworkError.Unreachable
            else -> {
                Timber.e("Unknown error while checking label name validity: $it")
                NetworkError.Unknown
            }
        }

        DataError.Remote.Http(error)
    }

    private suspend fun noSubFolderWithSameName(
        userId: UserId,
        name: String,
        parentId: LabelId?
    ): Boolean {
        return getLabels(userId, LabelType.MessageFolder).getOrNull().orEmpty().filter {
            it.parentId == parentId
        }.none {
            it.name.equalsNoCase(name)
        }
    }

    private fun noSystemFolderWithSameName(name: String) = FORBIDDEN_LABEL_NAME.none { it.equalsNoCase(name) }

    private suspend fun noLabelWithSameName(userId: UserId, name: String) =
        getLabels(userId, LabelType.MessageLabel).getOrNull().orEmpty().none { it.name.equalsNoCase(name) }

    private suspend fun noFolderWithSameName(userId: UserId, name: String) =
        getLabels(userId, LabelType.MessageFolder).getOrNull().orEmpty().none { it.name.equalsNoCase(name) }

    private companion object {

        val FORBIDDEN_LABEL_NAME = listOf(
            "inbox", "drafts", "sent", "starred", "archive", "spam", "trash", "outbox", "scheduled", "snoozed"
        )
    }
}
