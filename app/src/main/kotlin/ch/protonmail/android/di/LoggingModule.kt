package ch.protonmail.android.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.proton.android.account.api.ProtonAccountApi
import me.proton.android.core.client.info.di.PlatformName
import me.proton.android.core.client.info.di.ProductName
import me.proton.android.core.logging.Logger
import me.proton.android.core.logging.TimberLogger
import me.proton.android.core.logging.sentry.NoOpSentryAppLogger
import me.proton.android.core.logging.sentry.SentryAppLogger
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LoggingModule {
    @Provides
    @IntoSet
    fun provideTimberLogger(): Logger {
        // Inject it into the Set<Logger> for DefaultLogger.addLoggers().
        return TimberLogger()
    }

    @Provides
    @IntoSet
    fun provideAppLogger(sentryAppLogger: SentryAppLogger): Logger {
        // Inject it into the Set<Logger> for DefaultLogger.addLoggers().
        return sentryAppLogger
    }

    @Provides
    @Singleton
    fun provideSentryAppLogger(
        @ApplicationContext context: Context,
        @PlatformName platformName: String,
        @ProductName productName: String,
        accountApi: ProtonAccountApi
    ): SentryAppLogger {
        return NoOpSentryAppLogger
        /* TODO: Migrate Mail to use initSentry.
        return initSentry(
            context = context,
            name = "mail-app-logger"
        ) { options ->
            options.dsn = BuildConfig.MAIL_APP_SENTRY_DSN
            options.environment = "$platformName-$productName@prod"
            options.release = "android-mail@${BuildConfig.VERSION_NAME}"
            options.installDefaultInterceptors(
                context = context,
                isCrashReportsDisabled = { accountApi.isCrashReportsDisabled() },
                minEventLevel = SentryLevel.ERROR,
                minBreadcrumbLevel = SentryLevel.INFO,
                tags = listOf("ch.protonmail.android")
            )
        }*/
    }
}
