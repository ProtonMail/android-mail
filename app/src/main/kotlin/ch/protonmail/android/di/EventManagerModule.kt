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

import ch.protonmail.android.mailconversation.data.ConversationEventListener
import ch.protonmail.android.mailconversation.data.UnreadConversationsCountEventListener
import ch.protonmail.android.mailmessage.data.UnreadMessagesCountEventListener
import ch.protonmail.android.mailmessage.data.MessageEventListener
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import me.proton.core.contact.data.ContactEmailEventListener
import me.proton.core.contact.data.ContactEventListener
import me.proton.core.eventmanager.data.EventManagerQueryMapProvider
import me.proton.core.eventmanager.domain.EventListener
import me.proton.core.label.data.LabelEventListener
import me.proton.core.mailsettings.data.MailSettingsEventListener
import me.proton.core.notification.data.NotificationEventListener
import me.proton.core.push.data.PushEventListener
import me.proton.core.user.data.UserAddressEventListener
import me.proton.core.user.data.UserEventListener
import me.proton.core.user.data.UserSpaceEventListener
import me.proton.core.usersettings.data.UserSettingsEventListener
import javax.inject.Singleton

@Module(includes = [EventManagerModule.BindersModule::class])
@InstallIn(SingletonComponent::class)
@Suppress("LongParameterList")
object EventManagerModule {

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
        conversationEventListener: ConversationEventListener,
        notificationEventListener: NotificationEventListener,
        pushEventListener: PushEventListener,
        unreadMessagesCountEventListener: UnreadMessagesCountEventListener,
        unreadConversationsCountEventListener: UnreadConversationsCountEventListener,
        userSpaceEventListener: UserSpaceEventListener
    ): Set<EventListener<*, *>> = setOf(
        userEventListener,
        userAddressEventListener,
        userSettingsEventListener,
        userSpaceEventListener,
        mailSettingsEventListener,
        contactEventListener,
        contactEmailEventListener,
        labelEventListener,
        messageEventListener,
        conversationEventListener,
        notificationEventListener,
        pushEventListener,
        unreadMessagesCountEventListener,
        unreadConversationsCountEventListener
    )

    @Module
    @InstallIn(SingletonComponent::class)
    internal interface BindersModule {

        @Binds
        @Singleton
        fun bindsEventManagerQueryMapProvider(impl: MailEventManagerQueryMapProvider): EventManagerQueryMapProvider
    }
}
