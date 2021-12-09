/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.navigation.ui.AppNavGraph
import ch.protonmail.android.navigation.viewmodel.LauncherViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.accountmanager.presentation.view.AccountPrimaryView
import me.proton.core.accountmanager.presentation.viewmodel.AccountSwitcherViewModel
import me.proton.core.accountmanager.presentation.viewmodel.AccountSwitcherViewModel.Action

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val launcherViewModel: LauncherViewModel by viewModels()
    private val accountSwitcherViewModel: AccountSwitcherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ProtonTheme_Mail)
        super.onCreate(savedInstanceState)
        launcherViewModel.register(this)
        setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            ) {
                AppNavGraph(::onAccountViewAdded, ::navigateToLogin)
            }
        }
    }

    private fun navigateToLogin() {
        launcherViewModel.addAccount()
    }

    private fun onAccountViewAdded(accountView: AccountPrimaryView) =
        accountView.setup(accountSwitcherViewModel)

    private fun AccountPrimaryView.setup(viewModel: AccountSwitcherViewModel) {
        setViewModel(viewModel)
        viewModel.onAction().flowWithLifecycle(lifecycle).onEach {
            when (it) {
                is Action.Add -> launcherViewModel.signIn()
                is Action.SignIn -> launcherViewModel.signIn(it.account.userId)
                is Action.SignOut -> launcherViewModel.signOut(it.account.userId)
                is Action.Remove -> launcherViewModel.remove(it.account.userId)
                is Action.SetPrimary -> launcherViewModel.switch(it.account.userId)
            }
        }.launchIn(lifecycleScope)
    }
}
