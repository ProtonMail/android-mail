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
package ch.protonmail.android.uitests.robots.mailbox.composer

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessageExpirationRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot.MessagePasswordRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.messagedetail.MessageRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot

/**
 * [ComposerRobot] class contains actions and verifications for email composer functionality.
 * Inner classes: [MessagePasswordRobot], [MessageExpirationRobot].
 */
@Suppress("unused", "TooManyFunctions", "ExpressionBodySyntax")
class ComposerRobot {

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
        return ContactsRobot.ContactsGroupView()
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
            .addImageCaptureAttachment(R.drawable.logo_mail)
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
            .addImageCaptureAttachment(R.drawable.logo_mail)
            .sendWithNotSupportedExpiryConfirmation()
            .sendAnyway()

    fun sendMessageCameraCaptureAttachment(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addImageCaptureAttachment(R.drawable.logo_mail)
            .send()

    fun sendMessageWithFileAttachment(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addFileAttachment(R.drawable.logo_mail)
            .send()

    fun sendMessageTwoImageCaptureAttachments(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addTwoImageCaptureAttachments(R.drawable.logo_mail, R.drawable.logo_proton)
            .send()

    fun addAndRemoveAttachmentAndSend(to: String, subject: String, body: String): InboxRobot =
        composeMessage(to, subject, body)
            .attachments()
            .addFileAttachment(R.drawable.logo_mail)
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
            .addImageCaptureAttachment(R.drawable.logo_mail)
    }

    fun editBodyAndReply(newBody: String): MessageRobot =
        body(newBody).reply()

    fun clickUpButton(): ComposerRobot {

        return this
    }

    fun confirmDraftSaving(): InboxRobot {
        clickPositiveDialogButton()
        return InboxRobot()
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
        return MessagePasswordRobot()
    }

    private fun messageExpiration(): MessageExpirationRobot {
        return MessageExpirationRobot()
    }

    private fun hideExpirationView(): ComposerRobot {
        return this
    }

    fun attachments(): MessageAttachmentsRobot {
        return MessageAttachmentsRobot()
    }

    fun removeLastAttachment(): ComposerRobot {
        return ComposerRobot()
    }

    fun removeOneOfTwoAttachments(): ComposerRobot {
        return ComposerRobot()
    }

    fun send(): InboxRobot {
        waitForConditionAndSend()
        return InboxRobot()
    }

    private fun sendAndLaunchApp(): InboxRobot {
        return InboxRobot()
    }

    private fun sendWithNotSupportedExpiryConfirmation(): NotSupportedExpirationRobot {
        return NotSupportedExpirationRobot()
    }

    private fun sendToContact(): ContactsRobot {
        waitForConditionAndSend()
        return ContactsRobot()
    }

    private fun reply(): MessageRobot {
        waitForConditionAndSend()
        return MessageRobot()
    }

    private fun forward(): MessageRobot {
        waitForConditionAndSend()
        return MessageRobot()
    }

    @SuppressWarnings("EmptyFunctionBlock")
    private fun waitForConditionAndSend() {}

    /**
     * Class represents Message Password dialog.
     */
    class MessagePasswordRobot {

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
            return ComposerRobot()
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class MessageExpirationRobot {

        fun setExpirationInDays(days: Int): ComposerRobot =
            expirationDays(days)
                .confirmSetMessageExpiration()

        private fun expirationDays(days: Int): MessageExpirationRobot {
            return this
        }

        private fun confirmSetMessageExpiration(): ComposerRobot {
            return ComposerRobot()
        }
    }

    /**
     * Class represents Message Expiration dialog.
     */
    class NotSupportedExpirationRobot {

        fun sendAnyway(): InboxRobot {
            return InboxRobot()
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
