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

package me.proton.android.core.accountmanager.data.usecase

import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.accountmanager.domain.model.CoreAccountAvatarItem
import me.proton.android.core.accountmanager.domain.usecase.GetAccountAvatarItem
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetAccountResult
import javax.inject.Inject

class GetAccountAvatarItemImpl @Inject constructor(
    private val mailSession: MailSession
) : GetAccountAvatarItem {

    override suspend operator fun invoke(userId: CoreUserId?): CoreAccountAvatarItem? {
        userId ?: return null
        val account = when (val result = mailSession.getAccount(userId.id)) {
            is MailSessionGetAccountResult.Error -> null
            is MailSessionGetAccountResult.Ok -> result.v1
        }
        return CoreAccountAvatarItem(
            initials = account?.details()?.avatarInformation?.text,
            color = account?.details()?.avatarInformation?.color
        )
    }
}
