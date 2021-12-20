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
package ch.protonmail.android.uitests.robots.contacts


/**
 * [GroupDetailsRobot] class contains actions and verifications for Contacts functionality.
 */
@Suppress("unused", "ExpressionBodySyntax")
open class GroupDetailsRobot {

    fun edit(): AddContactGroupRobot {
        return AddContactGroupRobot()
    }

    fun deleteGroup(): ContactsRobot.ContactsGroupView {
        return delete()
            .confirmDeletion()
    }

    fun navigateUp(): ContactsRobot.ContactsGroupView {
        return ContactsRobot.ContactsGroupView()
    }

    private fun delete(): GroupDetailsRobot {
        return this
    }

    private fun confirmDeletion(): ContactsRobot.ContactsGroupView {
        return ContactsRobot.ContactsGroupView()
    }

    /**
     * Contains all the validations that can be performed by [GroupDetailsRobot].
     */
    class Verify

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
