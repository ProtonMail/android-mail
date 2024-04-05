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

package ch.protonmail.android.uitest.e2e.onboarding

import ch.protonmail.android.di.ServerProofModule
import ch.protonmail.android.networkmocks.mockwebserver.combineWith
import ch.protonmail.android.test.annotations.suite.RegressionTest
import ch.protonmail.android.uitest.MockedNetworkTest
import ch.protonmail.android.uitest.helpers.core.TestId
import ch.protonmail.android.uitest.helpers.core.navigation.Destination
import ch.protonmail.android.uitest.helpers.core.navigation.navigator
import ch.protonmail.android.uitest.helpers.login.LoginTestUserTypes
import ch.protonmail.android.uitest.helpers.network.mockNetworkDispatcher
import ch.protonmail.android.uitest.robot.onboarding.onboardingRobot
import ch.protonmail.android.uitest.robot.onboarding.section.topBarSection
import ch.protonmail.android.uitest.robot.onboarding.section.bottomSection
import ch.protonmail.android.uitest.robot.onboarding.section.middleSection
import ch.protonmail.android.uitest.robot.onboarding.section.verify
import ch.protonmail.android.uitest.robot.onboarding.verify
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.mockk
import me.proton.core.auth.domain.usecase.ValidateServerProof
import org.junit.Test

@RegressionTest
@HiltAndroidTest
@UninstallModules(ServerProofModule::class)
internal class OnboardingMainTests : MockedNetworkTest(
    showOnboarding = true,
    loginType = LoginTestUserTypes.Paid.FancyCapybara
) {

    @JvmField
    @BindValue
    val serverProofValidation: ValidateServerProof = mockk(relaxUnitFun = true)

    @Test
    @TestId("256675")
    fun checkOnboardingScreenShownAtFirstStartup() {
        mockWebServer.dispatcher combineWith mockNetworkDispatcher()

        navigator {
            navigateTo(Destination.Onboarding)
        }

        onboardingRobot {
            verify { isShown() }

            topBarSection {
                verify { isCloseButtonShown() }
            }

            middleSection {
                verify { isOnboardingImageShown() }
            }

            bottomSection {
                verify { isBottomButtonShown() }
            }
        }
    }
}
