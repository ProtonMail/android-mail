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

package ch.protonmail.android.benchmark.common

import android.widget.EditText
import androidx.benchmark.macro.MacrobenchmarkScope
import ch.protonmail.android.benchmark.BuildConfig
import ch.protonmail.android.benchmark.common.extensions.findUiObjectByClassWithParent
import ch.protonmail.android.benchmark.common.extensions.findUiObjectByResource
import ch.protonmail.android.benchmark.common.extensions.findUiObjectByText
import ch.protonmail.android.benchmark.common.extensions.waitUntilGone
import ch.protonmail.android.benchmark.common.identifiers.ResourceIdentifiers
import ch.protonmail.android.benchmark.common.identifiers.TextIdentifiers

internal fun MacrobenchmarkScope.performLogin(
    username: String = BuildConfig.DEFAULT_LOGIN,
    password: String = BuildConfig.DEFAULT_PASSWORD
) {
    // To be refactored with the Robot pattern.
    with(device) {
        findUiObjectByResource(ResourceIdentifiers.SignInButton).click()

        findUiObjectByClassWithParent(EditText::class.java, ResourceIdentifiers.UsernameInput)
            .setText(username)

        findUiObjectByClassWithParent(EditText::class.java, ResourceIdentifiers.PasswordInput)
            .setText(password)

        findUiObjectByResource(ResourceIdentifiers.PerformSignInButton).click()

        // Permission handling needs to be done here for now since the Login root view is still displayed underneath.
        runCatching {
            // TBC if this breaks as it depends on platform specific ids, which might change depending on the device.
            device.findUiObjectByResource(ResourceIdentifiers.AllowPermission).click()
        }

        waitUntilGone(ResourceIdentifiers.LoginScreenRootView, BenchmarkConfig.WaitForLoginToDisappearTimeout)
    }
}

internal fun MacrobenchmarkScope.skipOnboarding() {
    val expectedOnboardingPages = 3

    with(device) {
        repeat(expectedOnboardingPages) {
            findUiObjectByText(TextIdentifiers.OnboardingScreenButtonText).click()
        }

        findUiObjectByText(TextIdentifiers.OnboardingCompleteButtonText).click()
    }
}
