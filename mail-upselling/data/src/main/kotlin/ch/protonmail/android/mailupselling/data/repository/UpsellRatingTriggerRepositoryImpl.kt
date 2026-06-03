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

import ch.protonmail.android.mailupselling.domain.repository.UpsellRatingTriggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpsellRatingTriggerRepositoryImpl @Inject constructor() : UpsellRatingTriggerRepository {

    private val upsellSuccess = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    override fun observeUpsellSuccess(): Flow<Unit> = upsellSuccess.asSharedFlow()

    // Non-suspending tryEmit so the signal does not depend on the (short-lived) UpsellingViewModel
    // scope, which is cancelled when the upsell screen is popped on success.
    override fun emitUpsellSuccess() {
        upsellSuccess.tryEmit(Unit)
    }
}
