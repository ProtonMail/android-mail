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

package ch.protonmail.android.mailsettings.data.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailpinlock.model.AutoLockInterval
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsettings.data.local.RustAppSettingsDataSource
import ch.protonmail.android.mailsettings.data.mapper.toAppDiff
import ch.protonmail.android.mailsettings.data.mapper.toAppSettings
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.model.AppSettingsDiff
import ch.protonmail.android.mailsettings.domain.model.SwipeNextPreference
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.domain.repository.AppLanguageRepository
import ch.protonmail.android.mailsettings.domain.repository.AppSettingsRepository
import ch.protonmail.android.mailsettings.domain.repository.MobileSignatureRepository
import ch.protonmail.android.mailsettings.domain.repository.SwipeNextRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class AppSettingsRepository @Inject constructor(
    private val mailSessionRepository: MailSessionRepository,
    private val userSessionRepository: UserSessionRepository,
    private val rustAppSettingsDataSource: RustAppSettingsDataSource,
    private val appLanguageRepository: AppLanguageRepository,
    private val swipeNextRepository: SwipeNextRepository,
    private val mobileSignatureRepository: MobileSignatureRepository
) : AppSettingsRepository {

    private val restartTrigger = MutableSharedFlow<Unit>(replay = 1)

    override fun observeAppSettings(): Flow<AppSettings> = restartTrigger.flatMapLatest {
        userSessionRepository.observePrimaryUserId()
            .filterNotNull()
            .flatMapLatest { userId ->
                combine(
                    getLatestRustAppSettings(),
                    appLanguageRepository.observe(),
                    mobileSignatureRepository.observeMobileSignature(userId),
                    swipeNextRepository.observeSwipeNext(userId)
                ) { appSettingsEither, customLanguage, mobileSignature, swipeNext ->

                    appSettingsEither.fold(
                        {
                            AppSettings.default().apply {
                                Timber.e("Unable to get app settings $it, returning default settings: $this")
                            }
                        },
                        {
                            val swipePreference = swipeNext.getOrNull() ?: SwipeNextPreference.NotEnabled
                            it.toAppSettings(customLanguage, mobileSignature, swipePreference)
                        }
                    )
                }
            }
    }.apply { restartTrigger.tryEmit(Unit) }

    override fun observeTheme(): Flow<Theme> = observeAppSettings().map { it.theme }

    private fun getLatestRustAppSettings() = flow {
        emit(rustAppSettingsDataSource.getAppSettings(mailSessionRepository.getMailSession()))
    }

    private suspend fun updateAppSettings(diff: AppSettingsDiff): Either<DataError, Unit> =
        rustAppSettingsDataSource.updateAppSettings(
            mailSessionRepository.getMailSession(),
            diff.toAppDiff()
        ).onLeft { error ->
            Timber.e("Was not able to update app setting using the diff $error")
        }.onRight {
            restartTrigger.emit(Unit)
        }

    override suspend fun updateInterval(interval: AutoLockInterval): Either<DataError, Unit> =
        updateAppSettings(AppSettingsDiff(interval = interval))

    override suspend fun updateTheme(theme: Theme): Either<DataError, Unit> =
        updateAppSettings(AppSettingsDiff(theme = theme))

    override suspend fun updateAlternativeRouting(value: Boolean): Either<DataError, Unit> =
        updateAppSettings(AppSettingsDiff(alternativeRouting = value))

    override suspend fun updateUseCombineContacts(value: Boolean): Either<DataError, Unit> =
        updateAppSettings(AppSettingsDiff(combineContacts = value))

    override suspend fun updateUseMobileDataForContentSearch(value: Boolean): Either<DataError, Unit> =
        updateAppSettings(AppSettingsDiff(useMobileDataForContentSearchIndexing = value))

    override suspend fun updateSwipeToNextEmail(userId: UserId, value: Boolean): Either<DataError, Unit> =
        swipeNextRepository.setSwipeNextEnabled(userId, value)

    override suspend fun refreshSettings() {
        restartTrigger.emit(Unit)
    }
}
