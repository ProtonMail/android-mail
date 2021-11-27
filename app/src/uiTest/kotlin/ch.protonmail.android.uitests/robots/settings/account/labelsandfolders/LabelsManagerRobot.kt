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
package ch.protonmail.android.uitests.robots.settings.account.labelsandfolders

/**
 * [LabelsManagerRobot] class contains actions and verifications for Labels functionality.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class LabelsManagerRobot {

    fun addLabel(name: String): LabelsManagerRobot {
        return labelName(name)
            .saveNewLabel()
    }

    fun editLabel(name: String, newName: String, colorPosition: Int): LabelsManagerRobot {
        selectLabel(name)
            .updateLabelName(newName)
            .saveNewLabel()
        return this
    }

    fun deleteLabel(name: String): LabelsManagerRobot {
        selectFolderCheckbox(name)
            .clickDeleteSelectedButton()
            .confirmDeletion()
        return this
    }

    private fun clickDeleteSelectedButton(): FoldersManagerRobot.DeleteSelectedFoldersDialogRobot {
        return FoldersManagerRobot.DeleteSelectedFoldersDialogRobot()
    }

    private fun selectFolderCheckbox(name: String): LabelsManagerRobot {
        return this
    }


    private fun selectLabel(name: String): LabelsManagerRobot {
        return this
    }

    private fun updateLabelName(name: String): LabelsManagerRobot {
        return this
    }

    private fun labelName(name: String): LabelsManagerRobot {
        return this
    }

    private fun saveNewLabel(): LabelsManagerRobot {
        return this
    }

    /**
     * Contains all the validations that can be performed by [LabelsManagerRobot].
     */
    class Verify {

        fun labelWithNameShown(name: String) {}

        fun labelWithNameDoesNotExist(name: String) {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
