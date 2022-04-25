/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.domain

import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.repository.AlternativeRoutingRepository
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.domain.repository.AutoLockRepository
import ch.protonmail.android.mailsettings.domain.repository.CombinedContactsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveAppSettings @Inject constructor(
    private val autoLockRepository: AutoLockRepository,
    private val alternativeRoutingRepository: AlternativeRoutingRepository,
    private val appLanguageRepository: AppLanguageRepository,
    private val combinedContactsRepository: CombinedContactsRepository
) {

    operator fun invoke(): Flow<AppSettings> = combine(
        autoLockRepository.observe(),
        alternativeRoutingRepository.observe(),
        appLanguageRepository.observe(),
        combinedContactsRepository.observe()
    ) { autoLockPref, alternativeRouting, customLanguage, combinedContacts ->
        AppSettings(
            hasAutoLock = autoLockPref.isEnabled,
            hasAlternativeRouting = alternativeRouting.isEnabled,
            customAppLanguage = customLanguage?.langName,
            hasCombinedContacts = combinedContacts.isEnabled
        )
    }

}
