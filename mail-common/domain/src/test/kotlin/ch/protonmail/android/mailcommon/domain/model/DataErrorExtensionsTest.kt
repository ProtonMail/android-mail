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

package ch.protonmail.android.mailcommon.domain.model

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DataErrorExtensionsTest {

    @Test
    fun `data error is an offline error when type is Remote http and contains No Network Error`() {
        // given
        val dataError = DataError.Remote.Http(NetworkError.NoNetwork)
        // then
        assertTrue(dataError.isOfflineError())
    }

    @Test
    fun `data error is not an offline error when type is Remote http and does not contain No Network Error`() {
        // given
        val dataError = DataError.Remote.Http(NetworkError.ServerError)
        // then
        assertFalse(dataError.isOfflineError())
    }

    @Test
    fun `data error is not an offline error when type is Local`() {
        // given
        val dataError = DataError.Local.NoDataCached
        // then
        assertFalse(dataError.isOfflineError())
    }

    @Test
    fun `data error is a message already sent error when type is Remote Proton and contains DraftNotDraftError`() {
        // given
        val dataError = DataError.Remote.Proton(ProtonError.MessageUpdateDraftNotDraft, null)
        // then
        assertTrue(dataError.isMessageAlreadySentDraftError())
    }

    @Test
    fun `is not a message already sent error when type is Remote Proton and does not contain DraftNotDraftError`() {
        // given
        val dataError = DataError.Remote.Proton(ProtonError.Banned, null)
        // then
        assertFalse(dataError.isMessageAlreadySentDraftError())
    }

    @Test
    fun `is a message already sent error when type is Remote Proton and contains AttachmentMessageAlreadySentError`() {
        // given
        val dataError = DataError.Remote.Proton(ProtonError.AttachmentUploadMessageAlreadySent, null)
        // then
        assertTrue(dataError.isMessageAlreadySentAttachmentError())
    }

    @Test
    fun `is a message already sent error when type is Remote Proton and contains MessageAlreadySentError`() {
        // given
        val dataError = DataError.Remote.Proton(ProtonError.MessageAlreadySent, null)
        // then
        assertTrue(dataError.isMessageAlreadySentSendingError())
    }

    @Test
    fun `is not a message already sent error when type is Local`() {
        // given
        val dataError = DataError.Local.NoDataCached
        // then
        assertFalse(dataError.isMessageAlreadySentDraftError())
    }

    @Test
    fun `is search input invalid error when type is Remote proton of type SearchInputInvalid`() {
        // given
        val dataError = DataError.Remote.Proton(ProtonError.InputInvalid, null)
        // then
        assertTrue(dataError.isSearchInputInvalidError())
    }
}
