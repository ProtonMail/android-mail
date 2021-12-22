package ch.protonmail.android.initializer

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.logging.SentryUserObserver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.sentry.Sentry
import io.sentry.SentryOptions

class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Sentry.init { options: SentryOptions ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
            options.environment = BuildConfig.HOST
        }

        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SentryInitializerEntryPoint::class.java
        ).observer().start()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SentryInitializerEntryPoint {
        fun observer(): SentryUserObserver
    }
}
