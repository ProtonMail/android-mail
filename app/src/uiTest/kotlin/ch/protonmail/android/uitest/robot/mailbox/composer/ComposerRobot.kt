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
package ch.protonmail.android.uitest.robot.mailbox.composer

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import ch.protonmail.android.R
import ch.protonmail.android.uitest.robot.contacts.ContactsRobot
import ch.protonmail.android.uitest.robot.mailbox.composer.ComposerRobot.MessageExpirationRobot
import ch.protonmail.android.uitest.robot.mailbox.composer.ComposerRobot.MessagePasswordRobot
import ch.protonmail.android.uitest.robot.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitest.robot.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitest.robot.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitest.robot.mailbox.sent.SentRobot

/**
 * [ComposerRobot] class contains actions and verifications for email composer functionality.
 * Inner classes: [MessagePasswordRobot], [MessageExpirationRobot].
 */
@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
class ComposerRobot(
    private val composeTestRule: ComposeContentTestRule
) {

    fun sendAndLaunchApp(to: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .subject(subject)
            .body(body)
            .sendAndLaunchApp()

    fun sendMessage(to: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .subject(subject)
            .body(body)
            .send()

    fun sendMessageToContact(subject: String, body: String): ContactsRobot =
        subject(subject)
            .body(body)
            .sendToContact()

    fun sendMessageToGroup(subject: String, body: String): ContactsRobot.ContactsGroupView {
        subject(subject)
            .body(body)
            .sendToContact()
        return ContactsRobot.ContactsGroupView(composeTestRule)
    }

    fun forwardMessage(to: String, body: String): MessageRobot =
        recipients(to)
            .body(body)
            .forward()

    fun changeSubjectAndForwardMessage(to: String, subject: String): MessageRobot =
        recipients(to)
            .updateSubject(subject)
            .forward()

    fun sendMessageTOandCC(to: String, cc: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .showAdditionalRows()
            .ccRecipients(cc)
            .subject(subject)
            .body(body)
            .send()

    fun sendMessageTOandCCandBCC(to: String, cc: String, bcc: String, subject: String, body: String): InboxRobot =
        recipients(to)
            .showAdditionalRows()
            .ccRecipients(cc)
            .bccRecipients(bcc)
            .subject(subject)
            .body(body)
            .send()

    fun sendMessageWithPassword(to: String, subject: String, body: String, password: String, hint: String): InboxRobot =
        composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .send()

    fun sendMessageExpiryTimeInDays(to: String, subject: String, body: String, days: Int): InboxRobot =
        composeMessage(to, subject, body)
            .messageExpiration()
            .setExpirationInDays(days)
            .send()

    fun sendMessageExpiryTimeInDaysWithConfirmation(to: String, subject: String, body: String, days: Int): InboxRobot =
        composeMessage(to, subject, body)
            .messageExpiration()
            .setExpirationInDays(days)
            .sendWithNotSupportedExpiryConfirmation()
            .sendAnyway()

    @SuppressWarnings("LongParameterList")
    fun sendMessageEOAndExpiryTime(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot {
        return composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .send()
    }

    @SuppressWarnings("LongParameterList")
    fun sendMessageEOAndExpiryTimeWithConfirmation(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot {
        return composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .sendWithNotSupportedExpiryConfirmation()
            .sendAnyway()
    }

    @SuppressWarnings("LongParameterList")
    fun sendMessageEOAndExpiryTimeWithAttachment(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot =
        composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .hideExpirationView()
            .attachments()
            .addImageCaptureAttachment(R.drawable.ic_launcher_background)
            .send()

    @SuppressWarnings("LongParameterList")
    fun sendMessageEOAndExpiryTimeWithAttachmentAndConfirmation(
        to: String,
        subject: String,
        body: String,
        days: Int,
        password: String,
        hint: String
    ): InboxRobot =
        composeMessage(to, subject, body)
            .setMessagePassword()
            .definePasswordWithHint(password, hint)
            .messageExpiration()
            .setExpirationInDays(days)
            .hideExpirationView()
            .attachments()
            .addImageCaptureAttachment(R.drawable.ic_launcher_background)
            .sendWithNotSupportedExpiryConfirmation()
            .sendAnyway()

    fun sendMessageCameraCaptureAttachment(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addImageCaptureAttachment(R.drawable.ic_launcher_background)
            .send()

    fun sendMessageWithFileAttachment(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addFileAttachment(R.drawable.ic_launcher_background)
            .send()

    fun sendMessageTwoImageCaptureAttachments(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addTwoImageCaptureAttachments(
                R.drawable.ic_launcher_background,
                R.drawable.ic_launcher_foreground
            )
            .send()

    fun addAndRemoveAttachmentAndSend(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addFileAttachment(R.drawable.ic_launcher_background)
            .removeLastAttachment()
            .send()

    fun draftToSubjectBody(to: String, messageSubject: String, body: String): ComposerRobot =
        recipients(to)
            .subject(messageSubject)
            .body("TestData.messageBody")

    fun draftToBody(to: String, body: String): ComposerRobot =
        recipients(to)
            .body(body)

    fun draftSubjectBody(messageSubject: String, body: String): ComposerRobot =
        subject(messageSubject)
            .body(body)

    fun draftSubjectBodyAttachment(to: String, messageSubject: String, body: String): ComposerRobot {
        return draftToSubjectBody(to, messageSubject, body)
            .attachments()
            .addImageCaptureAttachment(R.drawable.ic_launcher_background)
    }

    fun editBodyAndReply(newBody: String): MessageRobot =
        body(newBody).reply()

    fun clickUpButton(): ComposerRobot {

        return this
    }

    fun confirmDraftSaving(): InboxRobot {
        clickPositiveDialogButton()
        return InboxRobot(composeTestRule)
    }

    fun confirmDraftSavingFromDrafts(): DraftsRobot {
        clickPositiveDialogButton()
        return DraftsRobot()
    }

    fun confirmDraftSavingFromSent(): SentRobot {
        clickPositiveDialogButton()
        return SentRobot()
    }

    private fun composeMessage(to: String, subject: String, body: String): ComposerRobot =
        recipients(to)
            .subject(subject)
            .body(body)

    fun recipients(email: String): ComposerRobot {
        return this
    }

    fun changeSenderTo(email: String): ComposerRobot = clickFromField().selectSender(email)

    @SuppressWarnings("EmptyFunctionBlock")
    private fun clickPositiveDialogButton() {}

    private fun clickFromField(): ComposerRobot {
        return this
    }

    private fun selectSender(email: String): ComposerRobot {
        return this
    }

    private fun ccRecipients(email: String): ComposerRobot {
        return this
    }

    private fun bccRecipients(email: String): ComposerRobot {
        return this
    }

    private fun subject(text: String): ComposerRobot {
        return this
    }

    fun updateSubject(text: String): ComposerRobot {
        return this
    }

    private fun body(text: String): ComposerRobot {
        return this
    }

    private fun showAdditionalRows(): ComposerRobot {
        return this
    }

    private fun setMessagePassword(): MessagePasswordRobot {
        return MessagePasswordRobot(composeTestRule)
    }

    private fun messageExpiration(): MessageExpirationRobot {
        return MessageExpirationRobot(composeTestRule)
    }

    private fun hideExpirationView(): ComposerRobot {
        return this
    }

    fun attachments(): MessageAttachmentsRobot {
        return MessageAttachmentsRobot(composeTestRule)
    }

    fun removeLastAttachment(): ComposerRobot {
        return ComposerRobot(composeTestRule)
    }

    fun removeOneOfTwoAttachments(): ComposerRobot {
        return ComposerRobot(composeTestRule)
    }

    fun send(): InboxRobot {
        waitForConditionAndSend()
        return InboxRobot(composeTestRule)
    }

    private fun sendAndLaunchApp(): InboxRobot {
        return InboxRobot(composeTestRule)
    }

    private fun sendWithNotSupportedExpiryConfirmation(): NotSupportedExpirationRobot {
        return NotSupportedExpirationRobot(composeTestRule)
    }

    private fun sendToContact(): ContactsRobot {
        waitForConditionAndSend()
        return ContactsRobot(composeTestRule)
    }

    private fun reply(): MessageRobot {
        waitForConditionAndSend()
        return MessageRobot(composeTestRule)
    }

    private fun forward(): MessageRobot {
        waitForConditionAndSend()
        return MessageRobot(composeTestRule)
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun waitForConditionAndSend() {}

    /**
     * Class represents Message Password dialog.
     */
    class MessagePasswordRobot(
        private val composeTestRule: ComposeContentTestRule
    ) {

        fun definePasswordWithHint(password: String, hint: String): ComposerRobot {
            return definePassword(password)
                .confirmPassword(password)
                .defineHint(hint)
                .applyPassword()
        }

        private fun definePassword(password: String): MessagePasswordRobot {
            return this
        }

        private fun confirmPassword(password: String): MessagePasswordRobot {
            return this
        }

        private fun defineHint(hint: String): MessagePasswordRobot {
            return this
        }

        private fun applyPassword(): ComposerRobot {
            return ComposerRobot(composeTestRule)
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class MessageExpirationRobot(
        private val composeTestRule: ComposeContentTestRule
    ) {

        fun setExpirationInDays(days: Int): ComposerRobot =
            expirationDays(days)
                .confirmSetMessageExpiration()

        private fun expirationDays(days: Int): MessageExpirationRobot {
            return this
        }

        private fun confirmSetMessageExpiration(): ComposerRobot {
            return ComposerRobot(composeTestRule)
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class NotSupportedExpirationRobot(
        private val composeTestRule: ComposeContentTestRule
    ) {

        fun sendAnyway(): InboxRobot {
            return InboxRobot(composeTestRule)
        }
    }

    /**
     * Contains all the validations that can be performed by [ComposerRobot].
     */
    class Verify {

        @SuppressWarnings("EmptyFunctionBlock")
        fun messageWithSubjectOpened(subject: String) {}

        @SuppressWarnings("EmptyFunctionBlock")
        fun bodyWithText(text: String) {}

        fun fromEmailIs(email: String): DraftsRobot {
            return DraftsRobot()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}
