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

package ch.protonmail.android.mailmailbox.presentation.paging

import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class PagingLoadingStateMapperKtTest {

    @Test
    fun `when source is loading and itemCount is 0 then Loading is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.source.refresh } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.Loading, items.mapToUiStates())
    }

    @Test
    fun `when mediator is loading and itemCount is 0 then Loading is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { loadState.mediator } returns mockk(relaxed = true)
            every { itemCount } returns 0
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator!!.refresh } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.Loading, items.mapToUiStates())
    }

    @Test
    fun `when append is loading and itemCount is 0 then Loading is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { loadState.mediator } returns mockk(relaxed = true)
            every { itemCount } returns 0
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator!!.refresh } returns LoadState.NotLoading(false)
            every { loadState.append } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.Loading, items.mapToUiStates())
    }

    @Test
    fun `when source is loading and itemCount is larger than 0 then LoadingWithData is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.source.refresh } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.LoadingWithData(items), items.mapToUiStates())
    }

    @Test
    fun `when mediator is loading and item count is larger than 0 then LoadingWithData is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.mediator!!.refresh } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.LoadingWithData(items), items.mapToUiStates())
    }

    @Test
    fun `when loadState is error and item count is 0 then Error is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.refresh } returns LoadState.Error(Exception())
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.Error, items.mapToUiStates())
    }

    @Test
    fun `when loadState is error and item count larger than 0 then ErrorWithData is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.refresh } returns LoadState.Error(Exception())
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.ErrorWithData(items), items.mapToUiStates())
    }

    @Test
    fun `when itemCount is 0 then Empty is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.source } returns mockk(relaxed = true)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.append } returns LoadState.NotLoading(false)
            every { loadState.prepend } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.Empty, items.mapToUiStates())
    }

    @Test
    fun `when itemCount is larger than 0 then Data is returned`() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.source } returns mockk(relaxed = true)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.append } returns LoadState.NotLoading(false)
            every { loadState.prepend } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.Data(items), items.mapToUiStates())
    }
}
