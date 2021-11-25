/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.navigation.launcher

import androidx.lifecycle.ViewModel
import ch.protonmail.android.navigation.launcher.PrimaryAccountState.SignedIn
import ch.protonmail.android.navigation.launcher.PrimaryAccountState.SigningIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val accountManager: AccountManager
) : ViewModel() {

    fun viewState(): Flow<LauncherViewState> =
        accountManager.getPrimaryAccount()
            .mapLatest { account ->
                account?.userId?.let { userId ->
                    LauncherViewState(SignedIn(userId))
                } ?: LauncherViewState(SigningIn)
            }
            .distinctUntilChanged()
}
