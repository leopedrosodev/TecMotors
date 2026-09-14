package br.com.tec.tecmotors

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class AppSmokeInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun canNavigateSaveData_andKeepAfterRecreate() {
        // O app abre no Resumo, nao mais num formulario.
        composeRule.waitUntil(timeoutMillis = 8000) {
            composeRule.onAllNodesWithText("Resumo").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onAllNodesWithText("Veículos").onFirst().performClick()

        composeRule.onAllNodesWithText("Data (DD/MM/AAAA)").onFirst().performTextInput("25/02/2026")
        composeRule.onAllNodesWithText("Odometro (km)").onFirst().performTextInput("22222")
        composeRule.onNodeWithText("Salvar odometro").performClick()

        composeRule.onNodeWithText("Odometro registrado").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 8000) {
            composeRule.onAllNodesWithText("Resumo").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Veículos").onFirst().performClick()
        composeRule.onNodeWithText("Ultimo odometro:", substring = true).assertIsDisplayed()
    }

    @Test
    fun quickRefuelSheet_opensFromFab() {
        composeRule.waitUntil(timeoutMillis = 8000) {
            composeRule.onAllNodesWithText("Resumo").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Abastecer").performClick()

        composeRule.onNodeWithText("Novo abastecimento").assertIsDisplayed()
        composeRule.onAllNodesWithText("Valor pago").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Litros").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("Odômetro").onFirst().assertIsDisplayed()
    }
}
