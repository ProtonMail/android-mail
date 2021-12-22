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
package ch.protonmail.android.uitest.robot.mailbox.search

import ch.protonmail.android.uitest.robot.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.mailbox.messagedetail.MessageRobot

/**
 * [SearchRobot] class contains actions and verifications for Search functionality.
 */
@Suppress("unused", "ExpressionBodySyntax")
class SearchRobot {

    fun searchMessageText(subject: String): SearchRobot {
        return this
    }

    fun clickSearchedMessageBySubject(subject: String): MessageRobot {
        return MessageRobot()
    }

    fun clickSearchedDraftBySubject(subject: String): ComposerRobot {
        return ComposerRobot()
    }

    fun navigateUpToInbox(): InboxRobot {
        return InboxRobot()
    }

    fun clickSearchedMessageBySubjectPart(subject: String): MessageRobot {
        return MessageRobot()
    }

    /**
     * Contains all the validations that can be performed by [InboxRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun searchedMessageFound() {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun noSearchResults() {}
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
