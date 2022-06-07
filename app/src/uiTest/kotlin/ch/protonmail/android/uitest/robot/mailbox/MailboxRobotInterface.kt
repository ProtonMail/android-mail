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
@file:Suppress("UNCHECKED_CAST")

package ch.protonmail.android.uitest.robot.mailbox

import ch.protonmail.android.uitest.robot.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitest.robot.mailbox.search.SearchRobot
import ch.protonmail.android.uitest.robot.menu.MenuRobot

@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
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

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageExists(messageSubject: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun draftWithAttachmentSaved(draftSubject: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageDeleted(subject: String, date: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun multipleMessagesDeleted(subjectMessageOne: String, subjectMessageTwo: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageWithSubjectExists(subject: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageWithSubjectHasRepliedFlag(subject: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageWithSubjectHasRepliedAllFlag(subject: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageWithSubjectHasForwardedFlag(subject: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageWithSubjectAndRecipientExists(subject: String, to: String) {}
    }

    private class SetLongClickMessage : (String, String) -> Unit {

        @SuppressWarnings("EmptyFunctionBlock")
        override fun invoke(subject: String, date: String) {}
    }

    private class SetSwipeLeftMessage : (String, String) -> Unit {

        @SuppressWarnings("EmptyFunctionBlock")
        override fun invoke(subject: String, date: String) {}
    }

    private class SetDeleteWithSwipeMessage : (String, String) -> Unit {

        @SuppressWarnings("EmptyFunctionBlock")
        override fun invoke(subject: String, date: String) {}
    }

    class SetSelectMessage : (String, String) -> Unit {

        @SuppressWarnings("EmptyFunctionBlock")
        override fun invoke(subject: String, date: String) {}
    }
}
