package ch.protonmail.android.mailmessage.domain.usecase

import java.io.IOException
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.MessageWithLabels
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetMessagesWithLabelsTest {

    private val userId = UserIdSample.Primary
    private val messageIds = listOf(MessageIdSample.AugWeatherForecast, MessageIdSample.HtmlInvoice)
    private val messages = listOf(
        MessageSample.AugWeatherForecast.copy(labelIds = listOf(LabelIdSample.Folder2022, LabelIdSample.News)),
        MessageSample.HtmlInvoice.copy(labelIds = listOf(LabelIdSample.Folder2022))
    )
    private val labels = listOf(LabelSample.Document, LabelSample.News)
    private val folders = listOf(LabelSample.Folder2021, LabelSample.Folder2022)
    private val localErrorFlow = flowOf(DataResult.Error.Local("error message", IOException()))

    private val observeMessages: ObserveMessages = mockk {
        every { this@mockk(userId, messageIds) } returns flowOf(messages.right())
    }
    private val labelRepository: LabelRepository = mockk {
        every { observeLabels(userId, LabelType.MessageLabel) } returns flowOf(
            DataResult.Success(ResponseSource.Remote, labels)
        )
        every { observeLabels(userId, LabelType.MessageFolder) } returns flowOf(
            DataResult.Success(ResponseSource.Remote, folders)
        )
    }

    private val getMessagesWithLabels = GetMessagesWithLabels(observeMessages, labelRepository)

    @BeforeTest
    fun setUp() {
        mockkStatic("ch.protonmail.android.mailcommon.domain.mapper.DataResultEitherMappingsKt")
        every { localErrorFlow.mapToEither() } returns flowOf(DataError.Local.NoDataCached.left())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `when all calls are successful, return messages with labels`() = runTest {
        // Given
        val expected = listOf(
            MessageWithLabels(message = messages.first(), labels = listOf(LabelSample.Folder2022, LabelSample.News)),
            MessageWithLabels(message = messages.last(), labels = listOf(LabelSample.Folder2022))
        ).right()

        // When
        val actual = getMessagesWithLabels.invoke(userId, messageIds)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when observing the message fails, return an error`() = runTest {
        // Given
        every { observeMessages(userId, messageIds) } returns flowOf(DataError.Local.NoDataCached.left())
        val expectedResult = DataError.Local.NoDataCached.left()

        // When
        val actual = getMessagesWithLabels.invoke(userId, messageIds)

        // Then
        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when observing the labels fails, return an error`() = runTest {
        // Given
        every { labelRepository.observeLabels(userId, LabelType.MessageLabel) } returns localErrorFlow
        val expectedResult = DataError.Local.NoDataCached.left()

        // When
        val actual = getMessagesWithLabels.invoke(userId, messageIds)

        // Then
        assertEquals(expectedResult, actual)
    }

    @Test
    fun `when observing the folders fails, return an error`() = runTest {
        // Given
        every { labelRepository.observeLabels(userId, LabelType.MessageFolder) } returns localErrorFlow
        val expectedResult = DataError.Local.NoDataCached.left()

        // When
        val actual = getMessagesWithLabels.invoke(userId, messageIds)

        // Then
        assertEquals(expectedResult, actual)
    }
}
