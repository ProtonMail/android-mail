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

package ch.protonmail.android.uicomponents.chips

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import ch.protonmail.android.uicomponents.chips.item.ChipItem
import ch.protonmail.android.uicomponents.chips.item.ChipItemsList
import me.proton.core.util.kotlin.takeIfNotBlank

@Stable
class ChipsListState(
    private val isValid: (String) -> Boolean,
    private val onListChanged: (List<ChipItem>) -> Unit,
    private val onInvalidItem: () -> Unit = {}
) {

    private val items: SnapshotStateList<ChipItem> = mutableStateListOf()
    private val typedText: MutableState<String> = mutableStateOf("")
    private val focusedState: MutableState<Boolean> = mutableStateOf(false)

    fun updateItems(newItems: List<ChipItem>) {
        if (newItems != items.toList()) {
            items.clear()
            items.addAll(newItems)
        }
    }

    fun getItems(): ChipItemsList = when {
        items.isEmpty() -> ChipItemsList.Empty
        items.size == 1 && !focusedState.value -> ChipItemsList.Unfocused.Single(items.first())
        items.size > 1 && !focusedState.value -> {
            ChipItemsList.Unfocused.Multiple(items.first(), ChipItem.Counter("+${items.size - 1}"))
        }

        else -> ChipItemsList.Focused(items)
    }

    fun type(newValue: String) {
        when {
            typedText.value + newValue.trim() == EmptyString -> clearTypedText()
            ChipsCreationRegex.containsMatchIn(newValue) -> {
                val words = typedText.value.split(
                    EmptySpace,
                    NewLineDelimiter,
                    CarriageReturnNewLineDelimiter,
                    CommaDelimiter,
                    SemiColonDelimiter,
                    TabDelimiter
                ).mapNotNull { it.takeIfNotBlank()?.trim() }

                if (words.isNotEmpty()) {
                    words.forEach { add(it) }
                    // added here so we only check for duplicates after pasting all the words
                    onListChanged(items)
                    clearTypedText()
                } else typedText.value = newValue
            }

            else -> typedText.value = newValue
        }
    }

    fun typeWord(word: String) {
        type(word)
        createChip()
    }

    fun createChip() {
        type(NewLineDelimiter)
    }

    fun isFocused(): Boolean = focusedState.value

    private fun add(item: String) {
        val chipContent = if (isValid(item)) {
            ChipItem.Valid(item)
        } else {
            onInvalidItem()
            ChipItem.Invalid(item)
        }

        items.add(chipContent)
    }

    fun onDelete() {
        if (typedText.value.isEmpty()) {
            items.removeLastOrNull()
        }
        onListChanged(items)
    }

    fun onDelete(index: Int) {
        items.removeAt(index)
        onListChanged(items)
    }

    fun setFocusState(focused: Boolean) {
        focusedState.value = focused
    }

    private fun clearTypedText() {
        typedText.value = EmptyString
    }

    companion object {
        val ChipsCreationRegex = Regex("[\n\r;,\t]+$")

        private const val EmptySpace = " "
        private const val EmptyString = ""
        private const val NewLineDelimiter = "\n"
        private const val CarriageReturnNewLineDelimiter = "\r\n"
        private const val SemiColonDelimiter = ";"
        private const val CommaDelimiter = ","
        private const val TabDelimiter = "\t"
    }
}
