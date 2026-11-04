/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailmessage.domain.model

import ch.protonmail.android.mailattachments.domain.model.AttachmentId

/**
 * Where a search matched inside one of a message's metadata fields, fuzzy matches included.
 *
 * [start] and [end] are UTF-8 byte offsets into the field's value, as the SDK indexes it. For
 * [SearchMatchField.Attachment] they are offsets into the name of the attachment [attachmentId].
 */
data class SearchMatchPosition(
    val field: SearchMatchField,
    val valueIndex: Int,
    val start: Int,
    val end: Int,
    val attachmentId: AttachmentId? = null
)

enum class SearchMatchField { Subject, From, To, Cc, Bcc, Attachment }

/**
 * The text that search matched at each of [positions], such as "government" for the query "governmnt".
 *
 * The SDK indexes each field as one value, with recipients joined by ", ". Positions in any other
 * value, or outside the field, are skipped. Attachment positions point into one attachment's name,
 * so they are read from [Message.attachmentPreviews] and skipped when that attachment is not among
 * them. Each value is encoded once, however many positions it has.
 */
fun Message.searchMatchedTerms(positions: List<SearchMatchPosition>): List<String> {
    val encodedValues = mutableMapOf<Pair<SearchMatchField, AttachmentId?>, ByteArray?>()
    return positions.mapNotNull { position ->
        val value = encodedValues.getOrPut(position.field to position.attachmentId) {
            searchIndexedValue(position)?.encodeToByteArray()
        } ?: return@mapNotNull null
        position
            .takeIf { it.isInIndexedValue() && it.start in 0 until it.end && it.end <= value.size }
            ?.let { value.decodeToString(it.start, it.end) }
    }
}

private fun Message.searchIndexedValue(position: SearchMatchPosition): String? = when (position.field) {
    SearchMatchField.Subject -> subject
    SearchMatchField.From -> sender.address
    SearchMatchField.To -> toList.joinToString(", ") { it.address }
    SearchMatchField.Cc -> ccList.joinToString(", ") { it.address }
    SearchMatchField.Bcc -> bccList.joinToString(", ") { it.address }
    SearchMatchField.Attachment -> attachmentPreviews.firstOrNull { it.attachmentId == position.attachmentId }?.name
}

/** Fields other than attachment names are indexed as one value. */
private fun SearchMatchPosition.isInIndexedValue() = field == SearchMatchField.Attachment || valueIndex == 0
