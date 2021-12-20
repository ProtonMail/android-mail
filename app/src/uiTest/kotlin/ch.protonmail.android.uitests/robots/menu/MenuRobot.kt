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
package ch.protonmail.android.uitests.robots.menu

import androidx.annotation.IdRes
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.mailbox.archive.ArchiveRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.labelfolder.LabelFolderRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.robots.mailbox.trash.TrashRobot
import ch.protonmail.android.uitests.robots.manageaccounts.AccountPanelRobot
import ch.protonmail.android.uitests.robots.reportbugs.ReportBugsRobot
import ch.protonmail.android.uitests.robots.settings.SettingsRobot

/**
 * [MenuRobot] class contains actions and verifications for menu functionality.
 */
@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
class MenuRobot {

    fun archive(): ArchiveRobot {
        return ArchiveRobot()
    }

    fun settings(): SettingsRobot {
        return SettingsRobot()
    }

    fun drafts(): DraftsRobot {
        return DraftsRobot()
    }

    fun inbox(): InboxRobot {
        return InboxRobot()
    }

    fun sent(): SentRobot {
        return SentRobot()
    }

    fun contacts(): ContactsRobot {
        return ContactsRobot()
    }

    fun reportBugs(): ReportBugsRobot {
        return ReportBugsRobot()
    }

    fun logout(): MenuRobot {
        return this
    }

    fun trash(): TrashRobot {
        return TrashRobot()
    }

    fun closeMenuWithSwipe(): MenuRobot {
        return this
    }

    fun labelOrFolder(withName: String): LabelFolderRobot {
        selectMenuLabelOrFolder(withName)
        return LabelFolderRobot()
    }

    fun accountsList(): AccountPanelRobot {
        return AccountPanelRobot()
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun selectMenuItem(@IdRes menuItemName: String) {}

    @SuppressWarnings("EmptyFunctionBlock")
    private fun selectMenuLabelOrFolder(@IdRes labelOrFolderName: String) {}

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun menuOpened() {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun menuClosed() {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
