package ch.protonmail.android.uicomponents.chips

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.Random
import java.util.UUID
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.chips.item.ChipItemsList
import kotlin.test.assertNotNull

class ChipsListStateTest {

    @Test
    fun `Should not add an item when just typing a space`() {
        // Given
        val state = buildState()

        // When
        state.type(" ")

        // Then
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
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
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
    }

    @Test
    fun `Should not add an item when adding a space word`() {
        // Given
        val state = buildState()

        // When
        state.typeWord(" ")

        // Then
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
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
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
    }

    @Test
    fun `Should add a valid chip item when typing a valid word`() {
        // Given
        val theWord = UUID.randomUUID().toString()
        val state = buildState(isValid = { true })

        // When
        state.typeWord(theWord)

        // Then
        val itemsList = state.getItems() as ChipItemsList.Unfocused.Single
        assertThat(itemsList.item, equalTo(ChipItem.Valid(theWord)))
    }

    @Test
    fun `Should add an invalid chip item when typing an  invalid word`() {
        // Given
        val theWord = UUID.randomUUID().toString()
        val state = buildState(isValid = { false })

        // When
        state.typeWord(theWord)

        // Then
        val itemsList = state.getItems() as ChipItemsList.Unfocused.Single
        assertThat(itemsList.item, equalTo(ChipItem.Invalid(theWord)))
    }

    @Test
    fun `Should not add an item until a new line is typed`() {
        // Given
        val state = buildState(isValid = { true })

        // When
        state.type("w")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type("wo")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type("wor")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type("word")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type("\n")

        // Then
        val itemsList = state.getItems() as ChipItemsList.Unfocused.Single
        assertThat(itemsList.item, equalTo(ChipItem.Valid("word")))
    }

    @Test
    fun `Should not add an item when a space is typed`() {
        // Given
        val state = buildState(isValid = { true })

        // When
        state.type("w")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type("wo")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type("wor")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type("word")
        assertThat(state.getItems() is ChipItemsList.Empty, equalTo(true))
        state.type(" ")

        // Then
        assertNotNull(state.getItems() as? ChipItemsList.Empty)
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

    @Test
    fun `Should set the correct text in the items when changing it and the element is focused`() {
        // Given
        val state = buildState()
        state.setFocusState(true)

        // When
        state.typeWord("hello")
        state.typeWord("world")
        state.type("!")
        state.type("\n")

        // Then
        val itemsList = state.getItems() as ChipItemsList.Focused
        assertThat(itemsList.items[0], equalTo(ChipItem.Valid("hello")))
        assertThat(itemsList.items[1], equalTo(ChipItem.Valid("world")))
        assertThat(itemsList.items[2], equalTo(ChipItem.Valid("!")))
    }

    @Test
    fun `Should set the correct text in the items when changing it and the element is not focused`() {
        // Given
        val state = buildState()
        state.setFocusState(false)

        // When
        state.typeWord("hello")

        // Then
        val itemsListUnfocusedSingle = state.getItems() as ChipItemsList.Unfocused.Single
        assertThat(itemsListUnfocusedSingle.item, equalTo(ChipItem.Valid("hello")))

        // When
        state.typeWord("world")

        // Then
        var itemsListUnfocusedMultiple = state.getItems() as ChipItemsList.Unfocused.Multiple
        assertThat(itemsListUnfocusedMultiple.item, equalTo(ChipItem.Valid("hello")))
        assertThat(itemsListUnfocusedMultiple.counter, equalTo(ChipItem.Counter("+1")))

        // When
        state.type("!")
        state.type("\n")

        // Then
        itemsListUnfocusedMultiple = state.getItems() as ChipItemsList.Unfocused.Multiple
        assertThat(itemsListUnfocusedMultiple.item, equalTo(ChipItem.Valid("hello")))
        assertThat(itemsListUnfocusedMultiple.counter, equalTo(ChipItem.Counter("+2")))
    }

    private fun buildState(isValid: (String) -> Boolean = { true }) = ChipsListState(
        isValid = isValid,
        onListChanged = {}
    )
}
