package br.com.tec.tecmotors

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.lifecycleScope
import br.com.tec.tecmotors.core.di.AppContainer
import br.com.tec.tecmotors.presentation.app.TecMotorsRoot
import br.com.tec.tecmotors.reminder.ReminderScheduler
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Precisa vir antes do super.onCreate para o sistema desenhar a splash
        // desde o primeiro frame, sem flash de janela vazia.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // A splash sai quando os dados estao prontos - nao depois de um tempo fixo.
        splashScreen.setKeepOnScreenCondition { !appContainer.appStartup.ready.value }
        splashScreen.setOnExitAnimationListener(::fadeOutSplash)

        lifecycleScope.launch { appContainer.appStartup.awaitReady() }

        enableEdgeToEdge()
        ReminderScheduler.initialize(this)

        val openQuickRefuel = intent?.getBooleanExtra(
            ReminderScheduler.EXTRA_OPEN_QUICK_REFUEL,
            false
        ) == true
        val openOdometer = intent?.getBooleanExtra(
            ReminderScheduler.EXTRA_OPEN_ODOMETER,
            false
        ) == true

        setContent {
            TecMotorsRoot(
                appContainer = appContainer,
                openQuickRefuelOnStart = openQuickRefuel,
                openOdometerOnStart = openOdometer
            )
        }
    }

    private fun fadeOutSplash(provider: SplashScreenViewProvider) {
        ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f).apply {
            interpolator = AccelerateInterpolator()
            duration = SPLASH_FADE_MILLIS
            doOnEnd { provider.remove() }
            start()
        }
    }

    private companion object {
        const val SPLASH_FADE_MILLIS = 220L
    }
}
