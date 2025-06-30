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

package ch.protonmail.android.mailupselling.domain.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetInstalledProtonApps @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    operator fun invoke(): Set<InstalledProtonApp> = InstalledProtonApp.entries.filter { it.isInstalled() }.toSet()

    private fun InstalledProtonApp.isInstalled() =
        appContext.packageManager.getLaunchIntentForPackage(packageName()) != null
}

private fun InstalledProtonApp.packageName() = when (this) {
    InstalledProtonApp.VPN -> "ch.protonvpn.android"
    InstalledProtonApp.Drive -> "me.proton.android.drive"
    InstalledProtonApp.Calendar -> "me.proton.android.calendar"
    InstalledProtonApp.Pass -> "proton.android.pass"
    InstalledProtonApp.Wallet -> "me.proton.wallet.android"
}

enum class InstalledProtonApp {
    VPN, Drive, Calendar, Pass, Wallet
}
