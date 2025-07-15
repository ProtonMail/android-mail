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
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.arch.mapSuccessValueOrNull
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import timber.log.Timber
import javax.inject.Inject

class GetDraftLabelId @Inject constructor(
    private val mailSettingsRepository: MailSettingsRepository,
    private val observePrimaryUserId: ObservePrimaryUserId
) {

    suspend operator fun invoke(): Either<DataError, MailLabelId> = Either.catch {
        val userId = observePrimaryUserId.invoke().filterNotNull().first()
        mailSettingsRepository.getMailSettingsFlow(userId).mapSuccessValueOrNull()
    }.mapLeft {
        val error = when (it) {
            is UnknownHostException -> NetworkError.NoNetwork
            is SocketTimeoutException -> NetworkError.Unreachable
            else -> {
                Timber.e("Unknown error while getting labels: $it")
                NetworkError.Unknown
            }
        }
        DataError.Remote.Http(error)
    }.map { settingsFlow ->
        val showMoved = settingsFlow.firstOrNull()?.showMoved
        if (showMoved?.enum == ShowMoved.Both) {
            MailLabelId.System.AllDrafts
        } else {
            MailLabelId.System.Drafts
        }
    }
}
