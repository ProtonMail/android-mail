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
import me.proton.core.domain.entity.UserId

interface AccountApiProvider {

    /**
     * Registers the activity result launchers and their listeners.
     *
     * Must be called synchronously from [AppCompatActivity.onCreate], before the activity is STARTED.
     */
    fun register(context: AppCompatActivity)

    /**
     * Registers the observers reacting to account state changes.
     *
     * Unlike [register], this can be deferred (eg. until the legacy migration completed).
     */
    suspend fun registerUserSessionObservers(context: AppCompatActivity)

    fun unregister()
    fun startAddAccount()
    fun startSignIn(username: String?)
    fun startSignUp()
    fun startSettings(userId: UserId)
    fun startSecurityKeys()

    suspend fun disableAccount(userId: UserId)
    suspend fun deleteAccount(userId: UserId)
    suspend fun switchAccount(userId: UserId)
}
