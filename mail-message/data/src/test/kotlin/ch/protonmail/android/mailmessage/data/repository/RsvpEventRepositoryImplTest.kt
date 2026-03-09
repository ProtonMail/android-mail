package ch.protonmail.android.mailmessage.data.repository

import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalRsvpAnswer
import ch.protonmail.android.mailcommon.data.mapper.LocalRsvpAttendee
import ch.protonmail.android.mailcommon.data.mapper.LocalRsvpCalendar
import ch.protonmail.android.mailcommon.data.mapper.LocalRsvpEvent
import ch.protonmail.android.mailcommon.data.mapper.LocalRsvpOccurrence
import ch.protonmail.android.mailcommon.data.mapper.LocalRsvpOrganizer
import ch.protonmail.android.mailcommon.data.mapper.LocalRsvpStateCancelledReminder
import ch.protonmail.android.mailmessage.data.local.RustRsvpEventDataSource
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toMessageId
import ch.protonmail.android.mailmessage.domain.model.CalendarId
import ch.protonmail.android.mailmessage.domain.model.EventId
import ch.protonmail.android.mailmessage.domain.model.RsvpAnswer
import ch.protonmail.android.mailmessage.domain.model.RsvpAttendee
import ch.protonmail.android.mailmessage.domain.model.RsvpAttendeeStatus
import ch.protonmail.android.mailmessage.domain.model.RsvpCalendar
import ch.protonmail.android.mailmessage.domain.model.RsvpEvent
import ch.protonmail.android.mailmessage.domain.model.RsvpOccurrence
import ch.protonmail.android.mailmessage.domain.model.RsvpOrganizer
import ch.protonmail.android.mailmessage.domain.model.RsvpState
import ch.protonmail.android.testdata.message.rust.LocalMessageIdSample
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import uniffi.mail_uniffi.RsvpAttendeeStatus as LocalRsvpAttendeeStatus

class RsvpEventRepositoryImplTest {

    private val dataSource = mockk<RustRsvpEventDataSource>()

    private val repository = RsvpEventRepositoryImpl(dataSource)

    @Test
    fun `identify rsvp should call the correct data source method`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast.toMessageId()
        coEvery { dataSource.identifyRsvp(userId, messageId.toLocalMessageId()) } returns true.right()

        // When
        val actual = repository.identifyRsvp(userId, messageId)

        // Then
        coVerify { dataSource.identifyRsvp(userId, messageId.toLocalMessageId()) }
        assertEquals(true.right(), actual)
    }

    @Test
    fun `get rsvp event should call the correct data source method`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast.toMessageId()
        coEvery { dataSource.getRsvpEvent(userId, messageId.toLocalMessageId()) } returns LocalRsvpEvent(
            id = "id",
            summary = "summary",
            location = "location",
            description = "description",
            recurrence = "recurrence",
            startsAt = 123.toULong(),
            endsAt = 124.toULong(),
            occurrence = LocalRsvpOccurrence.DATE,
            organizer = LocalRsvpOrganizer("organizerName", "organizerEmail"),
            attendees = listOf(LocalRsvpAttendee("attendeeName", "attendeeEmail", LocalRsvpAttendeeStatus.YES)),
            userAttendeeIdx = 0.toUInt(),
            calendar = LocalRsvpCalendar("id", "calendarName", "color"),
            state = LocalRsvpStateCancelledReminder
        ).right()

        // When
        val actual = repository.getRsvpEvent(userId, messageId)

        // Then
        val expected = RsvpEvent(
            eventId = EventId("id"),
            summary = "summary",
            location = "location",
            description = "description",
            recurrence = "recurrence",
            startsAt = 123L,
            endsAt = 124L,
            occurrence = RsvpOccurrence.Date,
            organizer = RsvpOrganizer("organizerName", "organizerEmail"),
            attendees = listOf(RsvpAttendee("attendeeName", "attendeeEmail", RsvpAttendeeStatus.Yes)),
            userAttendeeIdx = 0,
            calendar = RsvpCalendar(CalendarId("id"), "calendarName", "color"),
            state = RsvpState.CancelledReminder
        )
        coVerify { dataSource.getRsvpEvent(userId, messageId.toLocalMessageId()) }
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `answer rsvp event should call the correct data source method`() = runTest {
        // Given
        val userId = UserIdTestData.userId
        val messageId = LocalMessageIdSample.AugWeatherForecast.toMessageId()
        coEvery {
            dataSource.answerRsvpEvent(userId, messageId.toLocalMessageId(), LocalRsvpAnswer.YES)
        } returns Unit.right()

        // When
        val actual = repository.answerRsvpEvent(userId, messageId, RsvpAnswer.Yes)

        // Then
        coVerify { dataSource.answerRsvpEvent(userId, messageId.toLocalMessageId(), LocalRsvpAnswer.YES) }
        assertEquals(Unit.right(), actual)
    }
}
