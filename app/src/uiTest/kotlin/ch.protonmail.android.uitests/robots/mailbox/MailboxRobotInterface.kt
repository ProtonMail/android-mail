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
@file:Suppress("UNCHECKED_CAST")

package ch.protonmail.android.uitests.robots.mailbox

import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.search.SearchRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot

@Suppress("unused", "MemberVisibilityCanBePrivate")
interface MailboxRobotInterface {

    fun swipeLeftMessageAtPosition(position: Int): Any {
        return Any()
    }

    fun longClickMessageOnPosition(position: Int): Any {
        return Any()
    }

    fun deleteMessageWithSwipe(position: Int): Any {
        return Any()
    }

    fun searchBar(): SearchRobot {
        return SearchRobot()
    }

    fun compose(): ComposerRobot {
        return ComposerRobot()
    }

    fun menuDrawer(): MenuRobot {
        return MenuRobot()
    }

    fun clickMessageByPosition(position: Int): MessageRobot {
        return MessageRobot()
    }

    fun clickMessageBySubject(subject: String): MessageRobot {
        return MessageRobot()
    }

    fun clickFirstMatchedMessageBySubject(subject: String): MessageRobot {
        return MessageRobot()
    }

    fun refreshMessageList(): Any {
        return Any()
    }

    fun mailboxLayoutShown() {}

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    @Suppress("ClassName")
    open class verify {

        fun messageExists(messageSubject: String) {}

        fun draftWithAttachmentSaved(draftSubject: String) {}

        fun messageDeleted(subject: String, date: String) {}

        fun multipleMessagesDeleted(subjectMessageOne: String, subjectMessageTwo: String) {}

        fun messageWithSubjectExists(subject: String) {}

        fun messageWithSubjectHasRepliedFlag(subject: String) {}

        fun messageWithSubjectHasRepliedAllFlag(subject: String) {}

        fun messageWithSubjectHasForwardedFlag(subject: String) {}

        fun messageWithSubjectAndRecipientExists(subject: String, to: String) {}
    }

    private class SetLongClickMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {}
    }

    private class SetSwipeLeftMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {}
    }

    private class SetDeleteWithSwipeMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {}
    }

    class SetSelectMessage : (String, String) -> Unit {

        override fun invoke(subject: String, date: String) {}
    }
}
