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

package ch.protonmail.android.uitest.robot.detail.section

import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.uitest.robot.ComposeSectionRobot
import ch.protonmail.android.uitest.robot.detail.ConversationDetailRobot
import ch.protonmail.android.uitest.robot.detail.MessageDetailRobot
import ch.protonmail.android.uitest.robot.detail.model.DetailScreenTopBarEntryModel

@AttachTo(targets = [ConversationDetailRobot::class, MessageDetailRobot::class])
internal class DetailTopBarSection : ComposeSectionRobot() {

    private val topBarDetailModel = DetailScreenTopBarEntryModel(composeTestRule)

    fun tapBackButton() {
        topBarDetailModel.tapBack()
    }

    @VerifiesOuter
    inner class Verify {

        fun hasSubject(subject: String) = apply {
            topBarDetailModel.hasSubject(subject)
        }
    }
}
