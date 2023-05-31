package ch.protonmail.android.uicomponents.chips

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.Random
import java.util.UUID

class ChipsListStateTest {

    @Test
    fun `Should not add an item when just typing a space`() {
        // Given
        val state = buildState()

        // When
        state.type(" ")

        // Then
        assertThat(state.getItems().isEmpty(), equalTo(true))
    }

    @Test
    fun `Should not add an item chip when just typing a random blank string`() {
        // Given
        val state = buildState()

        // When
        state.type(
            listOf(Random().nextInt(100)).joinToString {
                when (it % 2 == 0) {
                    true -> " "
                    false -> "\t"
                }
            }
        )

        // Then
        assertThat(state.getItems().isEmpty(), equalTo(true))
    }

    @Test
    fun `Should not add an item when adding a space word`() {
        // Given
        val state = buildState()

        // When
        state.typeWord(" ")

        // Then
        assertThat(state.getItems().isEmpty(), equalTo(true))
    }

    @Test
    fun `Should not add an item when adding a random blank word`() {
        // Given
        val state = buildState()

        // When
        state.typeWord(
            listOf(Random().nextInt(100)).joinToString {
                when (it % 2 == 0) {
                    true -> " "
                    false -> "\t"
                }
            }
        )

        // Then
        assertThat(state.getItems().isEmpty(), equalTo(true))
    }

    @Test
    fun `Should add a valid chip item when typing a valid word`() {
        // Given
        val theWord = UUID.randomUUID().toString()
        val state = buildState(isValid = { true })

        // When
        state.typeWord(theWord)

        // Then
        assertThat(state.getItems().size, equalTo(1))
        assertThat(state.getItems().first(), equalTo(ChipItem.Valid(theWord)))
    }

    @Test
    fun `Should add an invalid chip item when typing an  invalid word`() {
        // Given
        val theWord = UUID.randomUUID().toString()
        val state = buildState(isValid = { false })

        // When
        state.typeWord(theWord)

        // Then
        assertThat(state.getItems().size, equalTo(1))
        assertThat(state.getItems().first(), equalTo(ChipItem.Invalid(theWord)))
    }

    @Test
    fun `Should not add an item until a space is typed`() {
        // Given
        val state = buildState(isValid = { true })

        // When
        state.type("w")
        assertThat(state.getItems().isEmpty(), equalTo(true))
        state.type("wo")
        assertThat(state.getItems().isEmpty(), equalTo(true))
        state.type("wor")
        assertThat(state.getItems().isEmpty(), equalTo(true))
        state.type("word")
        assertThat(state.getItems().isEmpty(), equalTo(true))
        state.type(" ")

        // Then
        assertThat(state.getItems().isEmpty(), equalTo(false))
        assertThat(state.getItems().first(), equalTo(ChipItem.Valid("word")))
    }

    @Test
    fun `Should set the correct focused state when changing it`() {
        // Given
        val state = buildState()

        // When
        state.setFocusState(true)

        // Then
        assertThat(state.isFocused(), equalTo(true))

        // When
        state.setFocusState(false)

        // Then
        assertThat(state.isFocused(), equalTo(false))
    }

    private fun buildState(isValid: (String) -> Boolean = { true }) = ChipsListState(
        isValid = isValid,
        onListChanged = {},
        initialValue = emptyList()
    )
}
