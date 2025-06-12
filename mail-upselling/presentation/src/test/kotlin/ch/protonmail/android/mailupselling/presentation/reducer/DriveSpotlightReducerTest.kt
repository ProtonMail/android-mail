package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightContentEvent
import ch.protonmail.android.mailupselling.presentation.model.DriveSpotlightUIState
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DriveSpotlightReducerTest {

    private val sut = DriveSpotlightContentReducer()

    @Test
    fun `should reduce to error when storage cannot be determined`() {
        // When
        val actual = sut.newStateFrom(DriveSpotlightContentEvent.StorageError)

        // Then
        assertEquals(
            DriveSpotlightUIState.Error(
                Effect.of(
                    TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id)
                )
            ),
            actual
        )
    }

    @Test
    fun `should map to null storage for storage under or at 1500 MB`() {
        // When
        val actual = sut.newStateFrom(DriveSpotlightContentEvent.DataLoaded(1.5f))

        // Then
        assertEquals(
            DriveSpotlightUIState.Data(null),
            actual
        )
    }

    @Test
    fun `should round down to nearest GB integer`() {
        // When
        val actual = sut.newStateFrom(DriveSpotlightContentEvent.DataLoaded(2.9f))

        // Then
        assertEquals(
            DriveSpotlightUIState.Data(2),
            actual
        )
    }
}
