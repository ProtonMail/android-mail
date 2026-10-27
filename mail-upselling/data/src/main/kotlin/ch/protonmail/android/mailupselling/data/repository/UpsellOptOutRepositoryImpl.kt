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

package ch.protonmail.android.mailupselling.data.repository

import ch.protonmail.android.mailupselling.domain.repository.UpsellOptOutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpsellOptOutRepositoryImpl @Inject constructor() : UpsellOptOutRepository {

    private val optOutChanges = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    override fun observeOptOutChanges(): Flow<Unit> = optOutChanges.asSharedFlow().onStart { emit(Unit) }

    override fun emitOptOutChanged() {
        optOutChanges.tryEmit(Unit)
    }
}
