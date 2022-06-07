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
package ch.protonmail.android.uitest.robot.settings.account.labelsandfolders

/**
 * [FoldersManagerRobot] class contains actions and verifications for Folders functionality.
 */
@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
class FoldersManagerRobot {

    fun addFolder(name: String): FoldersManagerRobot {
        folderName(name)
            .saveFolder()
        return this
    }

    fun deleteFolder(name: String): FoldersManagerRobot {
        selectFolderCheckbox(name)
            .clickDeleteSelectedButton()
            .confirmDeletion()
        return this
    }

    fun editFolder(name: String, newName: String, colorPosition: Int): FoldersManagerRobot {
        selectFolder(name)
            .updateFolderName(newName)
            .saveFolder()
        return this
    }

    fun navigateUpToLabelsAndFolders(): LabelsAndFoldersRobot {
        return LabelsAndFoldersRobot()
    }

    private fun clickDeleteSelectedButton(): DeleteSelectedFoldersDialogRobot {
        return DeleteSelectedFoldersDialogRobot()
    }

    private fun clickFolder(name: String): FoldersManagerRobot {
        return this
    }

    private fun folderName(name: String): FoldersManagerRobot {
        return this
    }

    private fun updateFolderName(name: String): FoldersManagerRobot {
        return this
    }

    private fun saveFolder(): FoldersManagerRobot {
        return this
    }

    private fun selectFolder(name: String): FoldersManagerRobot {
        return this
    }

    private fun selectFolderCheckbox(name: String): FoldersManagerRobot {
        return this
    }

    class DeleteSelectedFoldersDialogRobot {

        fun confirmDeletion(): FoldersManagerRobot {
            return FoldersManagerRobot()
        }
    }

    /**
     * Contains all the validations that can be performed by [FoldersManagerRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun folderWithNameShown(name: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun folderWithNameDoesNotExist(name: String) {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
