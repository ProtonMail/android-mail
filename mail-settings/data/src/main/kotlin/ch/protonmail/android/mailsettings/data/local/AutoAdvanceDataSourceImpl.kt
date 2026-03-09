/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.NextMessageOnMove
import javax.inject.Inject

// Here we're not intentionally using the MailSettingsRepository
// since we need props that are not exposed by the model provided the legacy proton-libs.
class AutoAdvanceDataSourceImpl @Inject constructor(
    private val mailSettingsDataSource: MailSettingsDataSource
) : AutoAdvanceDataSource {


    override suspend fun getAutoAdvance(userId: UserId): Either<DataError, Boolean> {
        return mailSettingsDataSource.observeMailSettings(userId)
            .firstOrNull()
            ?.let { it.nextMessageOnMove == NextMessageOnMove.ENABLED_EXPLICIT }
            ?.right()
            ?: DataError.Local.NoDataCached.left()
    }
}
