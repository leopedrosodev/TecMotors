package br.com.tec.tecmotors

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
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
    private val splashStartedAt = SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Precisa vir antes do super.onCreate para o sistema desenhar a splash
        // desde o primeiro frame, sem flash de janela vazia.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // A splash sai quando os dados estao prontos. Onde a marca anima
        // (API 31+), tambem esperamos a animacao terminar - senao ela seria
        // cortada no meio, que fica pior do que nao ter animacao nenhuma.
        splashScreen.setKeepOnScreenCondition {
            val elapsed = SystemClock.uptimeMillis() - splashStartedAt
            !appContainer.appStartup.ready.value || elapsed < splashMinMillis
        }
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

    /**
     * Piso de tempo da splash.
     *
     * Zero fora do Android 12+: la a splash de compatibilidade nao roda
     * AnimatedVectorDrawable, entao segurar a tela seria atraso puro, sem nada
     * para mostrar em troca.
     */
    private val splashMinMillis: Long
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SPLASH_ANIMATION_MILLIS
        } else {
            0L
        }

    private companion object {
        const val SPLASH_FADE_MILLIS = 220L

        /** Casa com android:windowSplashScreenAnimationDuration em values-v31. */
        const val SPLASH_ANIMATION_MILLIS = 760L
    }
}
