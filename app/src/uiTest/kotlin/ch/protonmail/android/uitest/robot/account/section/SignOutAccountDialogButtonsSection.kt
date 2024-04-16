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

package ch.protonmail.android.uitest.robot.account.section

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.account.SignOutAccountDialogRobot
import ch.protonmail.android.uitest.util.getTestString
import ch.protonmail.android.test.R as testR

@AttachTo(targets = [SignOutAccountDialogRobot::class], identifier = "buttonsSection")
internal class SignOutAccountDialogButtonsSection : ComposeSectionRobot() {

    private val signOutButton = composeTestRule.onAllNodesWithText(
        getTestString(testR.string.test_sign_out_dialog_confirm)
    ).onLast()

    private val noButton = composeTestRule.onAllNodesWithText(
        getTestString(testR.string.test_sign_out_dialog_cancel)
    ).onLast()

    fun tapSignOut() {
        signOutButton.performClick()
    }

    fun tapCancel() {
        noButton.performClick()
    }
}
