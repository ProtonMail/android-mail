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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.usecase

import android.content.Context
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.IsPaidUser
import ch.protonmail.android.mailsettings.domain.model.MobileFooter
import ch.protonmail.android.mailsettings.domain.repository.MobileFooterRepository
import ch.protonmail.android.mailsettings.presentation.R
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetMobileFooter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val isPaidUser: IsPaidUser,
    private val mobileFooterRepository: MobileFooterRepository
) {

    suspend operator fun invoke(userId: UserId): Either<DataError, MobileFooter> = either {
        val isPaidUser = isPaidUser(userId).getOrElse {
            raise(DataError.Local.Unknown)
        }

        if (!isPaidUser) {
            return@either MobileFooter.FreeUserMobileFooter(
                context.getString(R.string.mail_settings_identity_mobile_footer_default_free)
            )
        }

        mobileFooterRepository.getMobileFooter(userId).getOrElse {
            raise(DataError.Local.NoDataCached)
        }
    }
}
