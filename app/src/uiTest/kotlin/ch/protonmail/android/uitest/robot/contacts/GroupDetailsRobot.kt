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
package ch.protonmail.android.uitest.robot.contacts

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import ch.protonmail.android.uitest.robot.contacts.ContactsRobot.ContactsGroupView

/**
 * [GroupDetailsRobot] class contains actions and verifications for Contacts functionality.
 */
@Suppress("unused", "ExpressionBodySyntax")
open class GroupDetailsRobot(
    private val composeTestRule: ComposeContentTestRule
) {

    fun edit(): AddContactGroupRobot {
        return AddContactGroupRobot(composeTestRule)
    }

    fun deleteGroup(): ContactsGroupView {
        return delete()
            .confirmDeletion()
    }

    fun navigateUp(): ContactsGroupView {
        return ContactsGroupView(composeTestRule)
    }

    private fun delete(): GroupDetailsRobot {
        return this
    }

    private fun confirmDeletion(): ContactsGroupView {
        return ContactsGroupView(composeTestRule)
    }

    /**
     * Contains all the validations that can be performed by [GroupDetailsRobot].
     */
    class Verify

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
