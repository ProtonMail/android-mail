package ch.protonmail.android.mailconversation.data.local

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.data.local.RustConversationsQueryImpl.Companion.NONE_FOLLOWUP_GRACE_MS
import ch.protonmail.android.mailconversation.data.usecase.CreateRustConversationPaginator
import ch.protonmail.android.mailconversation.data.wrapper.ConversationCursorWrapper
import ch.protonmail.android.mailconversation.data.wrapper.ConversationPaginatorWrapper
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.mapper.toLocalConversationId
import ch.protonmail.android.mailpagination.domain.model.PageInvalidationEvent
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PageToLoad
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.mailpagination.domain.repository.PageInvalidationRepository
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.conversation.rust.LocalConversationTestData
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import uniffi.proton_mail_uniffi.ConversationScrollerListUpdate
import uniffi.proton_mail_uniffi.ConversationScrollerLiveQueryCallback
import uniffi.proton_mail_uniffi.ConversationScrollerUpdate
import kotlin.test.assertEquals

class RustConversationsQueryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val createRustConversationPaginator = mockk<CreateRustConversationPaginator>()
    private val rustMailboxFactory = mockk<RustMailboxFactory>()
    private val invalidationRepository = mockk<PageInvalidationRepository>()


    private val rustConversationsQuery = RustConversationsQueryImpl(
        rustMailboxFactory,
        createRustConversationPaginator,
        CoroutineScope(mainDispatcherRule.testDispatcher),
        invalidationRepository
    )

    @Test
    fun `returns IllegalStateError when mailbox is null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val pageKey = PageKey.DefaultPageKey()
        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.Unknown.left()

        // When
        val actual = rustConversationsQuery.getConversations(userId, pageKey)

        // Then
        assertEquals(PaginationError.Other(DataError.Local.IllegalStateError).left(), actual)
        verify { createRustConversationPaginator wasNot Called }
    }

    @Test
    fun `returns first page when called with PageToLoad First and rust emits items in the callback`() = runTest {
        // Given
        val expectedConversations = listOf(LocalConversationTestData.AugConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { this@mockk.nextPage() } answers {
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = expectedConversations,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = rustConversationsQuery.getConversations(userId, pageKey)

        // Then
        assertEquals(expectedConversations.right(), actual)
    }

    @Test
    fun `returns next page when called with PageToLoad Next`() = runTest {
        // Given
        val expectedConversations = listOf(LocalConversationTestData.OctConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId, pageToLoad = PageToLoad.Next)
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { this@mockk.nextPage() } answers {
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = expectedConversations,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                capture(callbackSlot)
            )
        } returns paginator.right()


        // When
        val actual = rustConversationsQuery.getConversations(userId, pageKey)

        // Then
        assertEquals(expectedConversations.right(), actual)
    }

    @Test
    fun `returns first page when called with PageToLoad All and paginator is newly created`() = runTest {
        // Given
        val expectedConversations = listOf(LocalConversationTestData.spamConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(
            labelId = labelId,
            pageToLoad = PageToLoad.All
        )
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } coAnswers {
                launch {
                    delay(100)
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = expectedConversations,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { reload() } returns Unit.right()
            coEvery { filterUnread(false) } just Runs
            coEvery { showSpamAndTrash(false) } just Runs
            every { getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = rustConversationsQuery.getConversations(userId, pageKey)

        // Then
        assertEquals(expectedConversations.right(), actual)
        coVerify(exactly = 1) { paginator.nextPage() }
        coVerify(exactly = 0) { paginator.reload() }
    }

    @Test
    fun `returns all pages when called with PageToLoad All and paginator already exists`() = runTest {
        // Given
        val firstPageConversations = listOf(
            LocalConversationTestData.OctConversation,
            LocalConversationTestData.AugConversation
        )
        val reloadedConversations = listOf(
            LocalConversationTestData.OctConversation,
            LocalConversationTestData.AugConversation
        )

        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val firstPageKey = PageKey.DefaultPageKey(
            labelId = labelId,
            pageToLoad = PageToLoad.First
        )
        val allPageKey = PageKey.DefaultPageKey(
            labelId = labelId,
            pageToLoad = PageToLoad.All
        )

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } coAnswers {
                launch {
                    delay(100)
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = firstPageConversations,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }

            coEvery { reload() } coAnswers {
                launch {
                    delay(100)
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.ReplaceFrom(
                                idx = 0uL,
                                items = reloadedConversations,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }

            coEvery { filterUnread(false) } just Runs
            coEvery { showSpamAndTrash(false) } just Runs
            every { getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val firstResult = rustConversationsQuery.getConversations(userId, firstPageKey)
        val allResult = rustConversationsQuery.getConversations(userId, allPageKey)

        // Then
        assertEquals(firstPageConversations.right(), firstResult)
        assertEquals(reloadedConversations.right(), allResult)

        coVerify(exactly = 1) { paginator.nextPage() }
        coVerify(exactly = 1) { paginator.reload() }
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
    }

    @Test
    fun `initialises paginator only once for any given label`() = runTest {
        // Given
        val firstPage = listOf(LocalConversationTestData.AugConversation)
        val nextPage = listOf(LocalConversationTestData.OctConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)
        val nextPageKey = pageKey.copy(pageToLoad = PageToLoad.Next)
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        var methodCallCounter = 0
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { this@mockk.nextPage() } answers {
                methodCallCounter++
                val expectedPage = when {
                    methodCallCounter == 1 -> firstPage
                    else -> nextPage
                }
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = expectedPage,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        rustConversationsQuery.getConversations(userId, pageKey)
        rustConversationsQuery.getConversations(userId, nextPageKey)

        // Then
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
    }

    @Test
    fun `re initialises paginator when labelId changes`() = runTest {
        // Given
        val firstPage = listOf(LocalConversationTestData.AugConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val newLabelId = SystemLabelId.Archive.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)
        val newPageKey = pageKey.copy(newLabelId)
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { this@mockk.nextPage() } answers {
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = firstPage,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.disconnect() } just Runs
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        rustConversationsQuery.getConversations(userId, pageKey)
        rustConversationsQuery.getConversations(userId, newPageKey)

        // Then
        coVerify(exactly = 2) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }

        coVerify { paginator.disconnect() }
    }

    @Test
    fun `updates paginator without re-initialising it when unread filter is updated`() = runTest {
        // Given
        val firstPage = listOf(LocalConversationTestData.AugConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { this@mockk.nextPage() } answers {
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = firstPage,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.disconnect() } just Runs
            coEvery { this@mockk.filterUnread(any()) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        rustConversationsQuery.getConversations(userId, pageKey)
        rustConversationsQuery.updateUnreadFilter(true)

        // Then
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
    }

    @Test
    fun `updates paginator without re-initialising when show trash filter is called`() = runTest {
        // Given
        val firstPage = listOf(LocalConversationTestData.AugConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { this@mockk.nextPage() } answers {
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = firstPage,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.disconnect() } just Runs
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(any()) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        rustConversationsQuery.getConversations(userId, pageKey)
        rustConversationsQuery.updateShowSpamTrashFilter(true)

        // Then
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
    }

    @Test
    fun `submits invalidation when onUpdate callback is fired with ReplaceBefore event`() = runTest {
        // Given
        val firstPage = listOf(LocalConversationTestData.AugConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } answers {
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = firstPage,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        expectConversationsInvalidatedSubmit()

        // When
        rustConversationsQuery.getConversations(userId, pageKey)
        callbackSlot.captured.onUpdate(
            ConversationScrollerUpdate.List(
                ConversationScrollerListUpdate.ReplaceBefore(
                    idx = 2uL,
                    items = firstPage,
                    scrollerId = DefaultScrollerId
                )
            )
        )

        // Then
        verifyConversationsInvalidatedSubmitted()
    }

    @Test
    fun `submits invalidation when onUpdate is fired with ReplaceFrom event with index greater than 0`() = runTest {
        // Given
        val firstPage = listOf(LocalConversationTestData.AugConversation)
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } answers {
                launch {
                    delay(100) // Simulate callback delay compared to nextPage invocation
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = firstPage,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        expectConversationsInvalidatedSubmit()

        // When
        rustConversationsQuery.getConversations(userId, pageKey)
        callbackSlot.captured.onUpdate(
            ConversationScrollerUpdate.List(
                ConversationScrollerListUpdate.ReplaceFrom(
                    idx = 2uL,
                    items = firstPage,
                    scrollerId = DefaultScrollerId
                )
            )
        )

        // Then
        verifyConversationsInvalidatedSubmitted()
    }

    @Test
    fun `Append None followed by ReplaceBefore(0) within grace returns follow-up items`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId, pageToLoad = PageToLoad.First)

        val expectedFollowUp = listOf(LocalConversationTestData.OctConversation)

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        // Emit immediate None, then within the grace window emit ReplaceBefore(0, items)
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } answers {
                CoroutineScope(mainDispatcherRule.testDispatcher).launch {
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.None(DefaultScrollerId)
                        )
                    )
                    delay(NONE_FOLLOWUP_GRACE_MS - 100)
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.ReplaceBefore(
                                idx = 0uL,
                                items = expectedFollowUp,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = rustConversationsQuery.getConversations(userId, pageKey)

        // Then
        assertEquals(expectedFollowUp.right(), actual)
    }

    @Test
    fun `Append None then follow-up arrives after grace returns empty`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId, pageToLoad = PageToLoad.First)

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        expectConversationsInvalidatedSubmit()

        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } answers {
                CoroutineScope(mainDispatcherRule.testDispatcher).launch {
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.None(DefaultScrollerId)
                        )
                    )
                    delay(NONE_FOLLOWUP_GRACE_MS + 100)
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.ReplaceBefore(
                                idx = 0uL,
                                items = listOf(LocalConversationTestData.OctConversation),
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = rustConversationsQuery.getConversations(userId, pageKey)

        // Then
        assertEquals(emptyList<LocalConversation>().right(), actual)
    }

    @Test
    fun `Append None then late Append after grace period triggers invalidation`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId, pageToLoad = PageToLoad.First)

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        coEvery { invalidationRepository.submit(PageInvalidationEvent.ConversationsInvalidated(id = 1)) } just Runs

        val lateItems = listOf(LocalConversationTestData.OctConversation)

        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } answers {
                CoroutineScope(mainDispatcherRule.testDispatcher).launch {
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.None(DefaultScrollerId)
                        )
                    )
                    delay(NONE_FOLLOWUP_GRACE_MS + 100)
                    callbackSlot.captured.onUpdate(
                        ConversationScrollerUpdate.List(
                            ConversationScrollerListUpdate.Append(
                                items = lateItems,
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { this@mockk.filterUnread(false) } just Runs
            coEvery { this@mockk.showSpamAndTrash(false) } just Runs
            every { this@mockk.getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = rustConversationsQuery.getConversations(userId, pageKey)

        // Then
        assertEquals(emptyList<LocalConversation>().right(), actual)

        // Late Append causes invalidation
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) {
            invalidationRepository.submit(
                PageInvalidationEvent.ConversationsInvalidated(id = 1)
            )
        }
    }

    @Test
    fun `getCursor returns cursor from existing paginator without reinitializing`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val conversationId = ConversationId("111").toLocalConversationId()
        val expectedCursor = mockk<ConversationCursorWrapper>()

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { nextPage() } coAnswers {
                callbackSlot.captured.onUpdate(
                    ConversationScrollerUpdate.List(
                        ConversationScrollerListUpdate.Append(
                            items = listOf(LocalConversationTestData.spamConversation),
                            scrollerId = DefaultScrollerId
                        )
                    )
                )
                Unit.right()
            }
            coEvery { filterUnread(false) } just Runs
            coEvery { showSpamAndTrash(false) } just Runs
            every { getScrollerId() } returns DefaultScrollerId
            coEvery { getCursor(conversationId) } returns expectedCursor.right()
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // Initialize paginator first so getCursor can reuse it
        rustConversationsQuery.getConversations(
            userId = userId,
            pageKey = PageKey.DefaultPageKey(
                labelId = labelId,
                pageToLoad = PageToLoad.First
            )
        )

        // When
        val actual = rustConversationsQuery.getCursor(
            userId = userId,
            labelId = labelId,
            conversationId = conversationId
        )

        // Then
        assertEquals(expectedCursor.right(), actual)
        coVerify(exactly = 1) { paginator.getCursor(conversationId) }
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
        coVerify(exactly = 0) { rustMailboxFactory.create(userId, labelId.toLocalLabelId()) }
    }

    @Test
    fun `getCursor initializes paginator when needed and returns cursor`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val conversationId = ConversationId("111").toLocalConversationId()
        val expectedCursor = mockk<ConversationCursorWrapper>()

        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val paginator = mockk<ConversationPaginatorWrapper> {
            coEvery { filterUnread(false) } just Runs
            coEvery { showSpamAndTrash(false) } just Runs
            every { getScrollerId() } returns DefaultScrollerId
            coEvery { getCursor(conversationId) } returns expectedCursor.right()
        }

        coEvery {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        } returns mailbox.right()

        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = rustConversationsQuery.getCursor(
            userId = userId,
            labelId = labelId,
            conversationId = conversationId
        )

        // Then
        assertEquals(expectedCursor.right(), actual)
        coVerify(exactly = 1) {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        }
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
        coVerify(exactly = 1) { paginator.getCursor(conversationId) }
    }

    private fun expectConversationsInvalidatedSubmit() {
        coEvery {
            invalidationRepository.submit(match { it is PageInvalidationEvent.ConversationsInvalidated })
        } just Runs
    }

    private fun verifyConversationsInvalidatedSubmitted(exactly: Int = 1) {
        coVerify(exactly = exactly) {
            invalidationRepository.submit(match { it is PageInvalidationEvent.ConversationsInvalidated })
        }
    }

    companion object {

        private const val DefaultScrollerId = "scroller-id"
    }
}
