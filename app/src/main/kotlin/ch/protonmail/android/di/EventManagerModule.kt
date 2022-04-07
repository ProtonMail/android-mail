/*
 * Copyright (c) 2020 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.di

import androidx.work.WorkManager
import ch.protonmail.android.mailconversation.data.ConversationEventListener
import ch.protonmail.android.mailmessage.data.MessageEventListener
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.proton.core.contact.data.ContactEmailEventListener
import me.proton.core.contact.data.ContactEventListener
import me.proton.core.eventmanager.data.EventManagerConfigProviderImpl
import me.proton.core.eventmanager.data.EventManagerCoroutineScope
import me.proton.core.eventmanager.data.EventManagerFactory
import me.proton.core.eventmanager.data.EventManagerProviderImpl
import me.proton.core.eventmanager.data.db.EventMetadataDatabase
import me.proton.core.eventmanager.data.repository.EventMetadataRepositoryImpl
import me.proton.core.eventmanager.data.work.EventWorkerManagerImpl
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.eventmanager.domain.EventManagerConfigProvider
import me.proton.core.eventmanager.domain.EventManagerProvider
import me.proton.core.eventmanager.domain.repository.EventMetadataRepository
import me.proton.core.eventmanager.domain.work.EventWorkerManager
import me.proton.core.label.data.LabelEventListener
import me.proton.core.mailsettings.data.MailSettingsEventListener
import me.proton.core.network.data.ApiProvider
import me.proton.core.presentation.app.AppLifecycleProvider
import me.proton.core.user.data.UserAddressEventListener
import me.proton.core.user.data.UserEventListener
import me.proton.core.usersettings.data.UserSettingsEventListener
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("LongParameterList")
object EventManagerModule {

    @Provides
    @Singleton
    @EventManagerCoroutineScope
    fun provideEventManagerCoroutineScope(): CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Provides
    @Singleton
    fun provideEventManagerConfigProvider(
        eventMetadataRepository: EventMetadataRepository
    ): EventManagerConfigProvider = EventManagerConfigProviderImpl(eventMetadataRepository)

    @Provides
    @Singleton
    @JvmSuppressWildcards
    fun provideEventManagerProvider(
        eventManagerFactory: EventManagerFactory,
        eventManagerConfigProvider: EventManagerConfigProvider,
        eventListeners: Set<EventListener<*, *>>
    ): EventManagerProvider = EventManagerProviderImpl(
        eventManagerFactory,
        eventManagerConfigProvider,
        eventListeners
    )

    @Provides
    @Singleton
    fun provideEventMetadataRepository(
        db: EventMetadataDatabase,
        provider: ApiProvider
    ): EventMetadataRepository = EventMetadataRepositoryImpl(db, provider)

    @Provides
    @Singleton
    fun provideEventWorkManager(
        workManager: WorkManager,
        appLifecycleProvider: AppLifecycleProvider
    ): EventWorkerManager = EventWorkerManagerImpl(workManager, appLifecycleProvider)

    @Provides
    @Singleton
    @ElementsIntoSet
    @JvmSuppressWildcards
    fun provideEventListenerSet(
        userEventListener: UserEventListener,
        userAddressEventListener: UserAddressEventListener,
        userSettingsEventListener: UserSettingsEventListener,
        mailSettingsEventListener: MailSettingsEventListener,
        contactEventListener: ContactEventListener,
        contactEmailEventListener: ContactEmailEventListener,
        labelEventListener: LabelEventListener,
        messageEventListener: MessageEventListener,
        conversationEventListener: ConversationEventListener
    ): Set<EventListener<*, *>> = setOf(
        userEventListener,
        userAddressEventListener,
        userSettingsEventListener,
        mailSettingsEventListener,
        contactEventListener,
        contactEmailEventListener,
        labelEventListener,
        messageEventListener,
        conversationEventListener,
    )
}
