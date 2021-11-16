package ch.protonmail.android.initializer

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory

class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        Sentry.init(
            BuildConfig.SENTRY_DSN,
            AndroidSentryClientFactory(context)
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
