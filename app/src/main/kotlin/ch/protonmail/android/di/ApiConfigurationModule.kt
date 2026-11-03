package ch.protonmail.android.di

import ch.protonmail.android.BuildConfig
import ch.protonmail.android.api.ApiHostOverride
import ch.protonmail.android.api.MailRustApiConfig
import ch.protonmail.android.mailcommon.domain.model.ApiEnvironment
import ch.protonmail.android.mailsession.domain.model.RustApiConfig
import ch.protonmail.android.useragent.BuildUserAgent
import ch.protonmail.android.useragent.GetAppVersion
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.Optional
import javax.inject.Singleton
import kotlin.jvm.optionals.getOrNull

@Module
@InstallIn(SingletonComponent::class)
object ApiConfigurationModule {

    @Provides
    @Singleton
    fun provideRustApiConfig(
        buildUserAgent: BuildUserAgent,
        getAppVersion: GetAppVersion,
        httpUrl: HttpUrl
    ): RustApiConfig = MailRustApiConfig(buildUserAgent, getAppVersion, httpUrl)

    @Provides
    @Singleton
    fun provideApiEnvironment(apiHostOverride: Optional<ApiHostOverride>): ApiEnvironment = ApiEnvironment(
        host = apiHostOverride.getOrNull()?.host() ?: BuildConfig.API_HOST,
        apiPrefix = BuildConfig.API_PREFIX
    )
}

@Module
@InstallIn(SingletonComponent::class)
interface ApiHostOverrideModule {

    /** Only the debug menu binds an override. */
    @BindsOptionalOf
    fun optionalApiHostOverride(): ApiHostOverride
}

@Module
@InstallIn(SingletonComponent::class)
object BaseUrlModule {
    @Provides
    @Singleton
    fun provideProtonApiUrl(apiEnvironment: ApiEnvironment): HttpUrl = apiEnvironment.baseUrl.toHttpUrl()
}
