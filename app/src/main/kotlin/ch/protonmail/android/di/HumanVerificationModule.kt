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

package ch.protonmail.android.di

import ch.protonmail.android.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.humanverification.data.HumanVerificationListenerImpl
import me.proton.core.humanverification.data.HumanVerificationManagerImpl
import me.proton.core.humanverification.data.HumanVerificationProviderImpl
import me.proton.core.humanverification.data.db.HumanVerificationDatabase
import me.proton.core.humanverification.data.repository.HumanVerificationRepositoryImpl
import me.proton.core.humanverification.data.repository.UserVerificationRepositoryImpl
import me.proton.core.humanverification.domain.HumanVerificationManager
import me.proton.core.humanverification.domain.HumanVerificationWorkflowHandler
import me.proton.core.humanverification.domain.repository.HumanVerificationRepository
import me.proton.core.humanverification.domain.repository.UserVerificationRepository
import me.proton.core.humanverification.presentation.CaptchaApiHost
import me.proton.core.humanverification.presentation.HumanVerificationApiHost
import me.proton.core.humanverification.presentation.HumanVerificationOrchestrator
import me.proton.core.humanverification.presentation.utils.HumanVerificationVersion
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.humanverification.HumanVerificationListener
import me.proton.core.network.domain.humanverification.HumanVerificationProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HumanVerificationModule {

    @Provides
    @HumanVerificationApiHost
    fun provideHumanVerificationApiHost(): String = "https://${BuildConfig.HUMAN_VERIFICATION_HOST}/"

    @Provides
    fun provideHumanVerificationVersion() = HumanVerificationVersion.HV2

    @Provides
    @CaptchaApiHost
    fun provideCaptchaApiHost(): String = "api.${BuildConfig.HOST}"

    @Provides
    fun provideHumanVerificationOrchestrator(
        humanVerificationVersion: HumanVerificationVersion
    ): HumanVerificationOrchestrator = HumanVerificationOrchestrator(humanVerificationVersion)

    @Provides
    @Singleton
    fun provideHumanVerificationListener(
        humanVerificationRepository: HumanVerificationRepository
    ): HumanVerificationListener = HumanVerificationListenerImpl(humanVerificationRepository)

    @Provides
    @Singleton
    fun provideHumanVerificationProvider(
        humanVerificationRepository: HumanVerificationRepository
    ): HumanVerificationProvider = HumanVerificationProviderImpl(humanVerificationRepository)

    @Provides
    @Singleton
    fun provideHumanVerificationRepository(
        db: HumanVerificationDatabase,
        keyStoreCrypto: KeyStoreCrypto
    ): HumanVerificationRepository = HumanVerificationRepositoryImpl(db, keyStoreCrypto)

    @Provides
    @Singleton
    fun provideUserVerificationRepository(
        apiProvider: ApiProvider
    ): UserVerificationRepository = UserVerificationRepositoryImpl(apiProvider)

    @Provides
    @Singleton
    fun provideHumanVerificationManagerImpl(
        humanVerificationProvider: HumanVerificationProvider,
        humanVerificationListener: HumanVerificationListener,
        humanVerificationRepository: HumanVerificationRepository
    ): HumanVerificationManagerImpl = HumanVerificationManagerImpl(
        humanVerificationProvider,
        humanVerificationListener,
        humanVerificationRepository
    )
}

@Module
@InstallIn(SingletonComponent::class)
interface HumanVerificationBindModule {

    @Binds
    fun bindHumanVerificationManager(
        humanVerificationManagerImpl: HumanVerificationManagerImpl
    ): HumanVerificationManager

    @Binds
    fun bindHumanVerificationWorkflowHandler(
        humanVerificationManagerImpl: HumanVerificationManagerImpl
    ): HumanVerificationWorkflowHandler
}
