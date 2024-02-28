package ch.protonmail.android.mailcomposer.domain.usecase

import ch.protonmail.android.mailcomposer.domain.model.RecipientsBcc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsCc
import ch.protonmail.android.mailcomposer.domain.model.RecipientsTo
import ch.protonmail.android.mailmessage.domain.model.Participant
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.key.domain.entity.key.PublicAddress
import me.proton.core.key.domain.repository.PublicAddressRepository
import me.proton.core.key.domain.repository.getPublicAddressOrNull
import kotlin.test.Test
import kotlin.test.assertEquals

class GetExternalRecipientsTest {

    private val userId = UserIdTestData.userId
    private val internalEmailAddress = "internal@pm.me"
    private val externalEmailAddress = "external@external.com"
    private val internalParticipant = Participant(internalEmailAddress, "Internal")
    private val externalParticipant = Participant(externalEmailAddress, "External")
    private val internalPublicAddress = PublicAddress(
        email = internalEmailAddress,
        recipientType = 1,
        mimeType = null,
        keys = emptyList(),
        signedKeyList = null,
        ignoreKT = null
    )
    private val externalPublicAddress = PublicAddress(
        email = externalEmailAddress,
        recipientType = 2,
        mimeType = null,
        keys = emptyList(),
        signedKeyList = null,
        ignoreKT = null
    )

    private val publicAddressRepository = mockk<PublicAddressRepository> {
        coEvery { getPublicAddressOrNull(userId, internalEmailAddress) } returns internalPublicAddress
        coEvery { getPublicAddressOrNull(userId, externalEmailAddress) } returns externalPublicAddress
    }

    private val getExternalRecipients = GetExternalRecipients(publicAddressRepository)

    @Test
    fun `should return a list of external participants`() = runTest {
        // When
        val actual = getExternalRecipients(
            userId,
            RecipientsTo(listOf(internalParticipant)),
            RecipientsCc(listOf(externalParticipant)),
            RecipientsBcc(emptyList())
        )

        // Then
        val expected = listOf(externalParticipant)
        assertEquals(expected, actual)
    }

    @Test
    fun `should return an empty list when getting the public address fails`() = runTest {
        // Given
        coEvery { publicAddressRepository.getPublicAddressOrNull(userId, externalEmailAddress) } returns null

        // When
        val actual = getExternalRecipients(
            userId,
            RecipientsTo(listOf(internalParticipant)),
            RecipientsCc(listOf(externalParticipant)),
            RecipientsBcc(emptyList())
        )

        // Then
        val expected = emptyList<Participant>()
        assertEquals(expected, actual)
    }
}
