/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.api

import androidx.appcompat.app.AppCompatActivity
import me.proton.android.account.api.ProtonAccountApi
import me.proton.android.account.types.ProtonUserId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class NewAccountApiProvider @Inject constructor(
    private val accountApi: ProtonAccountApi
) : AccountApiProvider {

    override fun register(context: AppCompatActivity) {
        // No-op
    }

    override suspend fun registerUserSessionObservers(context: AppCompatActivity) {
        // No-op
    }

    override fun unregister() {
        // No-op
    }

    override fun startAddAccount() {
        // No-op
    }

    override fun startSignIn(username: String?) {
        accountApi.launchSignIn()
    }

    override fun startSignUp() {
        accountApi.launchSignUp()
    }

    override fun startSettings(userId: UserId) {
        TODO("Not yet implemented")
    }

    override fun startSecurityKeys() {
        TODO("Not yet implemented")
    }

    override suspend fun disableAccount(userId: UserId) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAccount(userId: UserId) {
        accountApi.removeAccount(ProtonUserId(userId.id))
    }

    override suspend fun switchAccount(userId: UserId) {
        accountApi.setCurrentAccount(ProtonUserId(userId.id))
    }
}
