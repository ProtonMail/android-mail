package ch.protonmail.android.mailmailbox.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryMailboxRepositoryImplTest {

    private val inMemoryMailboxRepositoryImpl = InMemoryMailboxRepositoryImpl()

    @Test
    fun `should emit correct value when observing mailbox view count`() = runTest {
        inMemoryMailboxRepositoryImpl.observeScreenViewCount().test {
            awaitItem()
            inMemoryMailboxRepositoryImpl.recordScreenViewCount()
            assertEquals(1, awaitItem())
            inMemoryMailboxRepositoryImpl.recordScreenViewCount()
            assertEquals(2, awaitItem())
        }
    }
}
