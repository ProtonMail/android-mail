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
import ch.protonmail.android.uitest.robot.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
@Suppress("unused", "ExpressionBodySyntax")
class ContactsRobot(
    private val composeTestRule: ComposeContentTestRule
) {

    fun addContact(): AddContactRobot {
        return AddContactRobot(composeTestRule)
    }

    fun addGroup(): AddContactGroupRobot {
        return AddContactGroupRobot(composeTestRule)
    }

    fun openOptionsMenu(): ContactsMoreOptions {
        return ContactsMoreOptions(composeTestRule)
    }

    fun groupsView(): ContactsGroupView {
        return ContactsGroupView(composeTestRule)
    }

    fun contactsView(): ContactsView {
        return ContactsView()
    }

    fun navigateUpToInbox(): InboxRobot {
        return InboxRobot(composeTestRule)
    }

    fun clickContactByEmail(email: String): ContactDetailsRobot {
        return ContactDetailsRobot(composeTestRule)
    }

    inner class ContactsView {

        fun clickSendMessageToContact(contactEmail: String): ComposerRobot {
            return ComposerRobot(composeTestRule)
        }
    }

    class ContactsGroupView(
        private val composeTestRule: ComposeContentTestRule
    ) {

        fun navigateUpToInbox(): InboxRobot {
            return InboxRobot(composeTestRule)
        }

        fun clickGroup(withName: String): GroupDetailsRobot {
            return GroupDetailsRobot(composeTestRule)
        }

        fun clickGroupWithMembersCount(name: String, membersCount: String): GroupDetailsRobot {
            return GroupDetailsRobot(composeTestRule)
        }

        fun clickSendMessageToGroup(groupName: String): ComposerRobot {
            return ComposerRobot(composeTestRule)
        }

        fun openOptionsMenu(): ContactsGroupView {
            return ContactsGroupView(composeTestRule)
        }

        fun refreshGroups(): ContactsGroupView {
            return ContactsGroupView(composeTestRule)
        }

        class Verify {

            @SuppressWarnings("EmptyFunctionBlock")
            fun groupWithMembersCountExists(name: String, membersCount: String) {
            }

            @SuppressWarnings("EmptyFunctionBlock")
            fun groupDoesNotExists(groupName: String, groupMembersCount: String) {
            }
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    class ContactsMoreOptions(
        private val composeTestRule: ComposeContentTestRule
    ) {

        fun refreshContacts(): ContactsRobot {
            return ContactsRobot(composeTestRule)
        }
    }

    /**
     * Contains all the validations that can be performed by [ContactsRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun contactsOpened() {
        }

        @SuppressWarnings("EmptyFunctionBlock")
        fun contactExists(name: String, email: String) {
        }

        @SuppressWarnings("EmptyFunctionBlock")
        fun contactDoesNotExists(name: String, email: String) {
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
