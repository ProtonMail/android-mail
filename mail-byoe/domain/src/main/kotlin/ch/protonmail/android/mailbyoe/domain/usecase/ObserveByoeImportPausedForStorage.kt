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

package ch.protonmail.android.mailbyoe.domain.usecase

import ch.protonmail.android.mailbyoe.domain.model.ByoeImportError
import ch.protonmail.android.mailbyoe.domain.model.ByoeImportState
import ch.protonmail.android.mailbyoe.domain.repository.ByoeImportRepository
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsByoeMobileKillswitchOn
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Whether a Gmail import is paused because the user ran out of storage. It stops being true once the import
 * resumes after an upgrade, or is cancelled or undone.
 *
 * The SDK does not gate import statuses on any BYOE flag, so the mobile killswitch is checked here. The experiment
 * flag is not: an import that exists needs its banner whatever the user's treatment.
 */
class ObserveByoeImportPausedForStorage @Inject constructor(
    private val byoeImportRepository: ByoeImportRepository,
    @IsByoeMobileKillswitchOn private val isByoeMobileKillswitchOn: FeatureFlag<Boolean>
) {

    operator fun invoke(userId: UserId): Flow<Boolean> = flow {
        if (isByoeMobileKillswitchOn.get()) {
            emit(false)
            return@flow
        }

        emitAll(
            byoeImportRepository.observeImportStatuses(userId).map { statuses ->
                statuses.any { it.state == ByoeImportState.Paused && it.error == ByoeImportError.StorageLimit }
            }
        )
    }.distinctUntilChanged()
}
