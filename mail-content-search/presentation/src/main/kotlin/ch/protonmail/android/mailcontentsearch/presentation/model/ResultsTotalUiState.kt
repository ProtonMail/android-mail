/*
 * Copyright (c) 2026 Proton Technologies AG
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
package ch.protonmail.android.mailcontentsearch.presentation.model

import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState

/**
 * What the results header says about how many results the search has.
 *
 * [count] is the latest total Rust sent. It is null until the search has one.
 *
 * The header shows [shownCount], which stays null while the loading bar runs. The header says the
 * search is still running instead, so it never shows a number that is about to change.
 */
data class ResultsTotalUiState(
    val loadingBar: LoadingBarUiState,
    val count: Int?
) {

    val shownCount: Int? get() = count.takeIf { loadingBar is LoadingBarUiState.Hide }

    companion object {

        val Initial = ResultsTotalUiState(loadingBar = LoadingBarUiState.Hide, count = null)
    }
}
