package ch.protonmail.android.di

import java.io.File
import android.content.Context
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.R
import ch.protonmail.android.api.AccountApiProvider
import ch.protonmail.android.api.LegacyAccountApiProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.android.account.api.startup.AccountSdkEnabled
import me.proton.android.account.welcome.ui.model.WelcomeScreenConfig
import me.proton.android.core.client.info.capability.ClientInfoResponse
import me.proton.android.core.client.info.di.ClientVersionName
import me.proton.android.core.client.info.di.CustomScheme
import me.proton.android.core.client.info.di.ProductName
import me.proton.android.core.env.CoreEnvResolver
import me.proton.android.core.shell.CoreEnv
import me.proton.android.core.shell.CoreParams
import javax.inject.Singleton

private const val DATABASE_FILE_PATH = "account-db.sqlite"
private const val LOG_FILE_PATH = "account-log.txt"

@Module
@InstallIn(SingletonComponent::class)
interface AccountBindings {

    @Binds
    @Singleton
    abstract fun provideAccountApiProvider(impl: LegacyAccountApiProvider): AccountApiProvider
}

@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    @Provides
    @AccountSdkEnabled
    fun provideAccountSdkEnabled(): Boolean = false

    @Provides
    @Singleton
    @ClientVersionName
    @Suppress("FunctionOnlyReturningConstant")
    fun provideVersionName(): String = BuildConfig.VERSION_NAME

    @Provides
    @Singleton
    @ProductName
    @Suppress("FunctionOnlyReturningConstant")
    fun provideProductName(): String = "mail"

    @Provides
    @Singleton
    @CustomScheme
    fun provideCustomScheme(
        @ApplicationContext context: Context,
    ): String = context.getString(R.string.core_web_auth_redirect_scheme)

    @Provides
    @Singleton
    fun provideCoreEnv(@ApplicationContext context: Context): CoreEnv =
        CoreEnvResolver.resolve(context, BuildConfig.BE_API_ENV)

    @Provides
    @Singleton
    fun provideShellParams(
        @ApplicationContext context: Context,
        clientInfo: ClientInfoResponse,
    ): CoreParams = CoreParams(
        env = clientInfo.environment,
        dbPath = File(context.filesDir, DATABASE_FILE_PATH).absolutePath.toString(),
        logPath = File(context.filesDir, LOG_FILE_PATH).absolutePath.toString(),
        logLevel = if (BuildConfig.DEBUG) "debug,h2=off,pgp=off" else "info,h2=off,pgp=off",
        platform = clientInfo.platform,
        product = clientInfo.product,
        version = clientInfo.version,
        userAgent = clientInfo.userAgent,
    )

    @Provides
    @Singleton
    fun provideWelcomeScreenConfig(@ApplicationContext context: Context): WelcomeScreenConfig = WelcomeScreenConfig(
        headerImageRes = R.drawable.header_mail,
        logoRes = R.drawable.logo_mail,
        subtitle = context.getString(R.string.app_welcome_subtitle),
    )
}
