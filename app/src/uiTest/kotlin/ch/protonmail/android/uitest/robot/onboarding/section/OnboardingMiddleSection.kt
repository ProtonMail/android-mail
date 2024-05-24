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

package ch.protonmail.android.uitest.robot.onboarding.section

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import ch.protonmail.android.mailonboarding.presentation.OnboardingScreenTestTags
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.onboarding.OnboardingRobot
import ch.protonmail.android.uitest.util.child

@AttachTo(targets = [OnboardingRobot::class], identifier = "middleSection")
internal class OnboardingMiddleSection : ComposeSectionRobot() {

    private val parent = composeTestRule.onNodeWithTag(OnboardingScreenTestTags.RootItem)
    private val onboardingImage = parent.child {
        hasTestTag(OnboardingScreenTestTags.OnboardingImage)
    }

    @VerifiesOuter
    inner class Verify {

        fun isOnboardingImageShown() {
            onboardingImage.isDisplayed()
        }
    }
}