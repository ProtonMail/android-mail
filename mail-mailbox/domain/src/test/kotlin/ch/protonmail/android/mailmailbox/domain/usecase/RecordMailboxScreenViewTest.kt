package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailmailbox.domain.repository.InMemoryMailboxRepository
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test

class RecordMailboxScreenViewTest {

    private val inMemoryMailboxRepository = mockk<InMemoryMailboxRepository>(relaxUnitFun = true)

    private val recordMailboxScreenView = RecordMailboxScreenView(inMemoryMailboxRepository)

    @Test
    fun `should call repository method when the use case is called`() {
        // When
        recordMailboxScreenView()

        // Then
        verify { inMemoryMailboxRepository.recordScreenViewCount() }
    }
}
