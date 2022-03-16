package ch.protonmail.android.uitest.test.suite

import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.test.login.LoginFlowTests
import ch.protonmail.android.uitest.test.settings.AccountSettingsScreenTest
import ch.protonmail.android.uitest.test.settings.SettingsFlowTest
import ch.protonmail.android.uitest.test.settings.SettingsScreenTest
import org.junit.experimental.categories.Categories
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Categories::class)
@Categories.IncludeCategory(SmokeTest::class)
@Suite.SuiteClasses(
    LoginFlowTests::class,
    SettingsFlowTest::class,
    SettingsScreenTest::class,
    AccountSettingsScreenTest::class
)
class SmokeSuite
