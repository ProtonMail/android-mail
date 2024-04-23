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

package ch.protonmail.android.mailmessage.presentation.usecase

import java.util.regex.Pattern
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Document
import javax.inject.Inject

class SanitizeHtmlOfDecryptedMessageBody @Inject constructor() {

    private val blacklistedElements = arrayOf(
        "meta", "audio", "video", "iframe", "object", "picture",
        "form", "map", "area", "input", "embed", "script"
    )

    operator fun invoke(messageBodyWithType: MessageBodyWithType): String {
        if (messageBodyWithType.mimeType == MimeTypeUiModel.PlainText) return messageBodyWithType.messageBody

        return Jsoup.parse(messageBodyWithType.messageBody)
            .removeBlacklistedElements()
            .removePingAttributes()
            .removeLinkElements()
            .removeEventAttributes()
            .removeContentEditableAttributes()
            .toString()
    }

    private fun Document.removeBlacklistedElements(): Document {
        blacklistedElements.forEach { blacklistedElement ->
            val selectedElements = select(blacklistedElement)
            if (selectedElements.`is`("form")) {
                selectedElements.unwrap()
            } else {
                selectedElements.remove()
            }
        }
        return this
    }

    private fun Document.removePingAttributes(): Document {
        select("a[ping]").removeAttr("ping")
        return this
    }

    private fun Document.removeLinkElements(): Document {
        select("link[rel=prefetch]").remove()
        select("link[rel=stylesheet]").remove()
        select("link[rel=preload]").remove()
        select("link[rel=alternate stylesheet]").remove()
        return this
    }

    private fun Document.removeContentEditableAttributes(): Document {
        select("[contenteditable]").forEach { element ->
            element.attributes().remove("contenteditable")
        }
        return this
    }

    private fun Document.removeEventAttributes(): Document {
        select("[^on]").forEach { element ->
            val attributesToRemove = Attributes()
            element.attributes().forEach { attribute ->
                if (Pattern.compile("^on.+").matcher(attribute.key).matches()) {
                    attributesToRemove.add(attribute.key, attribute.value)
                }
            }
            attributesToRemove.forEach { attribute ->
                element.removeAttr(attribute.key)
            }
        }
        return this
    }
}
