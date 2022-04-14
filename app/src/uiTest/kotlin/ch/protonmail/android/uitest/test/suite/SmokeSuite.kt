package ch.protonmail.android.uitest.test.suite

import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.test.login.LoginFlowTests
import ch.protonmail.android.uitest.test.settings.SettingsFlowTest
import ch.protonmail.android.uitest.test.settings.account.AccountSettingsScreenTest
import ch.protonmail.android.uitest.test.settings.account.conversationmode.ConversationModeSettingScreenTest
import ch.protonmail.android.uitest.test.settings.appsettings.SettingsScreenTest
import ch.protonmail.android.uitest.test.settings.appsettings.theme.ThemeSettingScreenTest
import org.junit.experimental.categories.Categories
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Categories::class)
@Categories.IncludeCategory(SmokeTest::class)
@Suite.SuiteClasses(
    LoginFlowTests::class,
    SettingsFlowTest::class,
    SettingsScreenTest::class,
    AccountSettingsScreenTest::class,
    ConversationModeSettingScreenTest::class,
    ThemeSettingScreenTest::class
)
class SmokeSuite
