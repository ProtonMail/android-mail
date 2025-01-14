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

import android.content.Context
import androidx.work.WorkManager
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinkHelperImpl
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.compose.theme.AppTheme
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.configuration.EnvironmentConfiguration
import me.proton.core.domain.entity.AppStore
import me.proton.core.domain.entity.Product
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class LocalDiskOpCoroutineScope

    @Provides
    @Singleton
    fun provideProduct(): Product = Product.Mail

    @Provides
    @Singleton
    fun provideAppStore() = AppStore.GooglePlay

    @Provides
    fun provideAppTheme() = AppTheme { content ->
        ProtonTheme { content() }
    }

    @Provides
    @Singleton
    fun provideAppInfo(envConfig: EnvironmentConfiguration): AppInformation = AppInformation(
        appName = "Proton Mail",
        appVersionName = BuildConfig.VERSION_NAME,
        appVersionCode = BuildConfig.VERSION_CODE,
        appBuildType = BuildConfig.BUILD_TYPE,
        appBuildFlavor = BuildConfig.FLAVOR,
        appHost = envConfig.host
    )

    @Provides
    @Singleton
    fun provideRequiredAccountType(): AccountType = AccountType.Internal

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideReviewManager(@ApplicationContext context: Context): ReviewManager = ReviewManagerFactory.create(context)

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {

        @Binds
        fun bindNotificationsDeepLinkHelper(impl: NotificationsDeepLinkHelperImpl): NotificationsDeepLinkHelper
    }
}
