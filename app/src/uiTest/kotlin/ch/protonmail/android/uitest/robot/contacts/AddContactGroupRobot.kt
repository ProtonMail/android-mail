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

/**
 * [AddContactGroupRobot] class contains actions and verifications for Add/Edit Contact Groups.
 */
@Suppress("unused", "ExpressionBodySyntax")
class AddContactGroupRobot {

    fun editNameAndSave(name: String): GroupDetailsRobot {
        groupName(name)
            .done()
        return GroupDetailsRobot()
    }

    fun manageAddresses(): ManageAddressesRobot {
        return ManageAddressesRobot()
    }

    fun groupName(name: String): AddContactGroupRobot {
        return this
    }

    fun done(): ContactsRobot {
        return ContactsRobot()
    }
}
