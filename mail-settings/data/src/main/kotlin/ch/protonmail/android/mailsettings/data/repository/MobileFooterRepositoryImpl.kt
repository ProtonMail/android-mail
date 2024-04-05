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

package ch.protonmail.android.mailsettings.data.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsettings.data.repository.local.MobileFooterLocalDataSource
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.model.MobileFooterPreference
import ch.protonmail.android.mailsettings.domain.repository.MobileFooterRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MobileFooterRepositoryImpl @Inject constructor(
    private val mobileFooterLocalDataSource: MobileFooterLocalDataSource
) : MobileFooterRepository {

    override suspend fun getMobileFooter(userId: UserId): Either<DataError, MobileFooter> = either {
        val preference = mobileFooterLocalDataSource.observeMobileFooterPreference(userId)
            .firstOrNull()
            ?.getOrNull()
            ?: return DataError.Local.NoDataCached.left()

        return MobileFooter.PaidUserMobileFooter(preference.value, preference.enabled).right()
    }

    override suspend fun updateMobileFooter(
        userId: UserId,
        preference: MobileFooterPreference
    ): Either<DataError, Unit> = either {
        mobileFooterLocalDataSource.updateMobileFooter(userId, preference).onLeft {
            raise(DataError.Local.Unknown)
        }
    }
}
