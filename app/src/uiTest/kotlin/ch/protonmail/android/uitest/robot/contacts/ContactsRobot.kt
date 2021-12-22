/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.uitest.robot.contacts

import ch.protonmail.android.uitest.robot.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot

/**
 * [ContactsRobot] class contains actions and verifications for Contacts functionality.
 */
@Suppress("unused", "ExpressionBodySyntax")
class ContactsRobot {

    fun addContact(): AddContactRobot {
        return AddContactRobot()
    }

    fun addGroup(): AddContactGroupRobot {
        return AddContactGroupRobot()
    }

    fun openOptionsMenu(): ContactsMoreOptions {
        return ContactsMoreOptions()
    }

    fun groupsView(): ContactsGroupView {
        return ContactsGroupView()
    }

    fun contactsView(): ContactsView {
        return ContactsView()
    }

    fun navigateUpToInbox(): InboxRobot {
        return InboxRobot()
    }

    fun clickContactByEmail(email: String): ContactDetailsRobot {
        return ContactDetailsRobot()
    }

    inner class ContactsView {

        fun clickSendMessageToContact(contactEmail: String): ComposerRobot {
            return ComposerRobot()
        }
    }

    class ContactsGroupView {

        fun navigateUpToInbox(): InboxRobot {
            return InboxRobot()
        }

        fun clickGroup(withName: String): GroupDetailsRobot {
            return GroupDetailsRobot()
        }

        fun clickGroupWithMembersCount(name: String, membersCount: String): GroupDetailsRobot {
            return GroupDetailsRobot()
        }

        fun clickSendMessageToGroup(groupName: String): ComposerRobot {
            return ComposerRobot()
        }

        fun openOptionsMenu(): ContactsGroupView {
            return ContactsGroupView()
        }

        fun refreshGroups(): ContactsGroupView {
            return ContactsGroupView()
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

    class ContactsMoreOptions {

        fun refreshContacts(): ContactsRobot {
            return ContactsRobot()
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
