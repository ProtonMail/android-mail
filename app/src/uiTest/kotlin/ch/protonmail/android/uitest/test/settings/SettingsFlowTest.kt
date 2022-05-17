package ch.protonmail.android.uitest.test.settings

import ch.protonmail.android.uitest.BaseTest
import ch.protonmail.android.uitest.annotation.SmokeTest
import ch.protonmail.android.uitest.robot.menu.MenuRobot
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class SettingsFlowTest : BaseTest() {

    private val user = quark.userCreate()
    private val menuRobot = MenuRobot(composeTestRule)

    @Before
    fun setUp() {
        login(user)
    }

    @Test
    @Category(SmokeTest::class)
    fun openAccountSettings() {

        menuRobot
            .settings()
            .openUserAccountSettings()
            .verify { accountSettingsOpened(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    fun openConversationModeSetting() {
        menuRobot
            .settings()
            .openUserAccountSettings()
            .also { it.verify { accountSettingsOpened(composeTestRule) } }
            .conversationMode()
            .also { it.verify { conversationModeToggleShown(composeTestRule) } }
    }

    @Test
    @Category(SmokeTest::class)
    fun openSettingAndChangePreferredTheme() {
        val themeSettingsRobot = menuRobot
            .settings()
            .selectThemeSettings()

        themeSettingsRobot
            .selectSystemDefault()
            .verify { defaultThemeSettingShown(composeTestRule) }
        themeSettingsRobot
            .selectDarkTheme()
            .verify { darkThemeSelected(composeTestRule) }
    }

    @Test
    @Category(SmokeTest::class)
    fun openSettingAndChangePreferredLanguage() {
        val languageSettingsRobot = menuRobot
            .settings()
            .selectLanguageSettings()

        languageSettingsRobot
            .selectSystemDefault()
            .verify { defaultLanguagesScreenIsShown(composeTestRule) }

        languageSettingsRobot
            .selectSpanish()
            .verify {
                spanishLanguageSelected(composeTestRule)
                appLanguageChangedToSpanish(composeTestRule)
            }

        languageSettingsRobot
            .selectPortuguese()
            .verify {
                portugueseLanguageSelected(composeTestRule)
                appLanguageChangedToPortuguese(composeTestRule)
            }

        languageSettingsRobot
            .selectSystemDefault()
            .verify { defaultLanguagesScreenIsShown(composeTestRule) }
    }
}
