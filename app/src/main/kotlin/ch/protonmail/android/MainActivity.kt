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
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.databinding.ActivityMainBinding
import ch.protonmail.android.feature.account.AccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.accountmanager.presentation.view.AccountPrimaryView
import me.proton.core.accountmanager.presentation.viewmodel.AccountSwitcherViewModel
import me.proton.core.accountmanager.presentation.viewmodel.AccountSwitcherViewModel.Action
import me.proton.core.presentation.ui.ProtonViewBindingActivity

@AndroidEntryPoint
class MainActivity : ProtonViewBindingActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val accountViewModel: AccountViewModel by viewModels()
    private val accountSwitcherViewModel: AccountSwitcherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ProtonTheme_Mail)
        super.onCreate(savedInstanceState)
        accountViewModel.setup(this)
        binding.accountPrimaryView.setup(accountSwitcherViewModel)
    }

    private fun AccountViewModel.setup(context: FragmentActivity) {
        register(context)
        state.flowWithLifecycle(lifecycle, Lifecycle.State.CREATED).onEach {
            when (it) {
                AccountViewModel.State.Processing -> Unit
                AccountViewModel.State.AccountNeeded -> accountViewModel.addAccount()
                AccountViewModel.State.PrimaryExist -> Unit
                AccountViewModel.State.StepNeeded -> Unit
            }
        }.launchIn(lifecycleScope)
    }

    private fun AccountPrimaryView.setup(viewModel: AccountSwitcherViewModel) {
        setViewModel(viewModel)
        viewModel.onAction().flowWithLifecycle(lifecycle).onEach {
            when (it) {
                is Action.Add -> accountViewModel.signIn()
                is Action.SignIn -> accountViewModel.signIn(it.account.userId)
                is Action.SignOut -> accountViewModel.signOut(it.account.userId)
                is Action.Remove -> accountViewModel.remove(it.account.userId)
                is Action.SetPrimary -> accountViewModel.switch(it.account.userId)
            }
        }.launchIn(lifecycleScope)
    }
}
