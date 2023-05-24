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

package ch.protonmail.android.mailcommon.dagger

import android.app.NotificationManager
import android.content.Context
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailcommon.presentation.system.NotificationProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NotificationCommonModule {

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    fun provideNotificationProvider(): NotificationProvider = NotificationProviderImpl()


}
