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

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.R
import me.proton.android.account.entry.AccountEntryGate
import me.proton.android.account.welcome.ui.model.WelcomeScreenConfig
import me.proton.android.core.designsystem.theme.ProtonTheme
import javax.inject.Inject

class NewAccountWelcomeProvider @Inject constructor() : AccountWelcomeProvider {

    @Composable
    override fun Welcome(
        onAddAccount: () -> Unit,
        signedInContent: @Composable () -> Unit
    ) {
        ProtonTheme {
            AccountEntryGate(
                welcomeConfig = WelcomeScreenConfig(
                    headerImageRes = R.drawable.header_mail,
                    logoRes = R.drawable.logo_mail,
                    subtitle = stringResource(R.string.app_welcome_subtitle),
                ),
                signedInContent = signedInContent
            )
        }
    }
}
