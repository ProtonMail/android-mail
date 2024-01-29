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

package ch.protonmail.android.benchmark.common.identifiers

import ch.protonmail.android.benchmark.common.BenchmarkConfig

internal object ResourceIdentifiers {

    private const val PackageName = BenchmarkConfig.PackageName

    const val SignInButton = "$PackageName:id/sign_in"
    const val UsernameInput = "$PackageName:id/usernameInput"
    const val PasswordInput = "$PackageName:id/passwordInput"
    const val PerformSignInButton = "$PackageName:id/signInButton"
    const val AllowPermission = "com.android.permissioncontroller:id/permission_allow_button"
    const val LoginScreenRootView = "$PackageName:id/scrollContent"
}
