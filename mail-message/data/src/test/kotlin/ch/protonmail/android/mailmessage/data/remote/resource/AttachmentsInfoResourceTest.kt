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

package ch.protonmail.android.mailmessage.data.remote.resource

import org.junit.Test
import kotlin.test.assertEquals

class AttachmentsInfoResourceTest {

    @Test
    fun `when atts info is null then atts count is zero`() {
        val resource = null

        val actual = resource.toAttachmentsCount()

        assertEquals(0, actual.calendar)
    }

    @Test
    fun `when atts info contains no atts of ics type calendar then atts count is zero`() {
        val resource = AttachmentsInfoResource(
            ics = AttachmentCountsResource(attachedCount = 0, inlineCount = 0),
            applicationIcs = AttachmentCountsResource(attachedCount = 0, inlineCount = 0)
        )

        val actual = resource.toAttachmentsCount()

        assertEquals(0, actual.calendar)
    }

    @Test
    fun `when atts info contains 1 or more atts of ics type then calendar att count is equal to that number`() {
        val resource = AttachmentsInfoResource(
            ics = AttachmentCountsResource(attachedCount = 2, inlineCount = 0),
            applicationIcs = AttachmentCountsResource(attachedCount = 0, inlineCount = 0)
        )

        val actual = resource.toAttachmentsCount()

        assertEquals(2, actual.calendar)
    }

    @Test
    fun `when atts info contains 1 or more atts of app-ics type then calendar att count is equal to that number`() {
        val resource = AttachmentsInfoResource(
            ics = AttachmentCountsResource(attachedCount = 0, inlineCount = 0),
            applicationIcs = AttachmentCountsResource(attachedCount = 1, inlineCount = 0)
        )

        val actual = resource.toAttachmentsCount()

        assertEquals(1, actual.calendar)
    }

    @Test
    fun `when atts info contains both atts of ics and app-ics types then calendar att count is equal to their sum`() {
        val resource = AttachmentsInfoResource(
            ics = AttachmentCountsResource(attachedCount = 3, inlineCount = 0),
            applicationIcs = AttachmentCountsResource(attachedCount = 1, inlineCount = 0)
        )

        val actual = resource.toAttachmentsCount()

        assertEquals(4, actual.calendar)
    }

    @Test
    fun `count of atts with inline disposition is ignored for calendar attachments as not legal`() {
        val resource = AttachmentsInfoResource(
            ics = AttachmentCountsResource(attachedCount = 1, inlineCount = 2),
            applicationIcs = AttachmentCountsResource(attachedCount = 0, inlineCount = 5)
        )

        val actual = resource.toAttachmentsCount()

        assertEquals(1, actual.calendar)
    }
}
