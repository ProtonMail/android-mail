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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.paging.exception.DataErrorException
import ch.protonmail.android.test.annotations.suite.SmokeTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@SmokeTest
class PagingLoadingStateMapperKtTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun returnLoadingWhenItemCountIsZero() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.source.refresh } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.Loading, items.mapToUiStates(false))
    }

    @Test
    fun returnLoadingWhenItemCountIsZeroAndMediatorIsLoading() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { loadState.mediator } returns mockk(relaxed = true)
            every { itemCount } returns 0
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator!!.refresh } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.Loading, items.mapToUiStates(false))
    }

    @Test
    fun returnLoadingWithDataWhenItemCountIsLargerThanZeroAndSourceIsLoading() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.source.refresh } returns LoadState.Loading
            every { loadState.append } returns LoadState.NotLoading(false)

        }
        assertEquals(MailboxScreenState.LoadingWithData, items.mapToUiStates(false))
    }

    @Test
    fun returnLoadingWithDataWhenItemCountIsLargerThanZeroAndMediatorIsLoading() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.append } returns LoadState.NotLoading(false)
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.mediator!!.refresh } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.LoadingWithData, items.mapToUiStates(false))
    }

    @Test
    fun returnUnexpectedErrorWhenLoadStateIsAnUnknownError() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.refresh } returns LoadState.Error(Exception("Not a DataErrorException"))
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.UnexpectedError, items.mapToUiStates(false))
    }

    @Test
    fun returnErrorWhenLoadStateIsAKnownErrorAndItemCountIsZero() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.refresh } returns LoadState.Error(
                DataErrorException(DataError.Remote.Http(NetworkError.ServerError))
            )
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.Error, items.mapToUiStates(false))
    }

    @Test
    fun returnOfflineWhenRefreshLoadStateIsOfflineAndItemCountIsZero() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.refresh } returns LoadState.Error(
                DataErrorException(DataError.Remote.Http(NetworkError.NoNetwork))
            )
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.Offline, items.mapToUiStates(false))
    }

    @Test
    fun returnErrorWithDataWhenLoadStateIsAKnownErrorAndItemCountIsLargerThanZero() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.refresh } returns LoadState.Error(
                DataErrorException(DataError.Remote.Http(NetworkError.Unreachable))
            )
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.ErrorWithData, items.mapToUiStates(false))
    }

    @Test
    fun returnOfflineWithDataWhenLoadStateIsOfflineAndItemCountIsLargerThanZero() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.refresh } returns LoadState.Error(
                DataErrorException(DataError.Remote.Http(NetworkError.NoNetwork))
            )
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.OfflineWithData, items.mapToUiStates(false))
    }

    @Test
    fun returnEmptyWhenItemCountIsZero() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 0
            every { loadState.source } returns mockk(relaxed = true)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.append } returns LoadState.NotLoading(false)
            every { loadState.prepend } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.Empty, items.mapToUiStates(false))
    }

    @Test
    fun returnDataWhenItemCountIsLargerThanZero() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.source } returns mockk(relaxed = true)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.append } returns LoadState.NotLoading(false)
            every { loadState.prepend } returns LoadState.NotLoading(false)
        }
        assertEquals(MailboxScreenState.Data(items), items.mapToUiStates(false))
    }

    @Test
    fun returnAppendLoadingWhenAppendLoadStateIsLoading() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.Loading
        }
        assertEquals(MailboxScreenState.AppendLoading, items.mapToUiStates(false))
    }

    @Test
    fun returnUnexpectedErrorWhenAppendLoadStateIsAnUnknownError() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.Error(Exception("Not a known DataErrorException"))
        }
        assertEquals(MailboxScreenState.UnexpectedError, items.mapToUiStates(false))
    }

    @Test
    fun returnExpectedErrorWhenAppendLoadStateIsAnKnownError() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.Error(
                DataErrorException(DataError.Remote.Http(NetworkError.ServerError))
            )
        }
        assertEquals(MailboxScreenState.AppendError, items.mapToUiStates(false))
    }

    @Test
    fun returnAppendOfflineErrorWhenAppendLoadStateIsOfflineError() {
        val items = mockk<LazyPagingItems<MailboxItemUiModel>> {
            every { itemCount } returns 10
            every { loadState.refresh } returns LoadState.NotLoading(false)
            every { loadState.source.refresh } returns LoadState.NotLoading(false)
            every { loadState.mediator } returns mockk(relaxed = true)
            every { loadState.append } returns LoadState.Error(
                DataErrorException(DataError.Remote.Http(NetworkError.NoNetwork))
            )
        }
        assertEquals(MailboxScreenState.AppendOfflineError, items.mapToUiStates(false))
    }
}
