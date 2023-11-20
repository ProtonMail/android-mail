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

package ch.protonmail.android.mailcomposer.domain.model

data class DraftFields(
    val sender: SenderEmail,
    val subject: Subject,
    val body: DraftBody,
    val recipientsTo: RecipientsTo,
    val recipientsCc: RecipientsCc,
    val recipientsBcc: RecipientsBcc,
    val originalHtmlQuote: OriginalHtmlQuote?
) {
    /**
     * Returns true if all of the fields (except sender and quoted html body) are blank.
     * Can be used to infer whether these fields should be used to store or discard a draft.
     */
    fun areBlank() = haveBlankSubject() &&
        haveBlankRecipients() &&
        body.value.isBlank()

    fun haveBlankSubject() = subject.value.isBlank()

    fun haveBlankRecipients() = recipientsTo.value.isEmpty() &&
        recipientsCc.value.isEmpty() &&
        recipientsBcc.value.isEmpty()
}
