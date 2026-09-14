package br.com.tec.tecmotors.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import br.com.tec.tecmotors.MainActivity
import br.com.tec.tecmotors.R
import br.com.tec.tecmotors.data.local.TecMotorsDatabase
import br.com.tec.tecmotors.data.local.mapper.toDomain
import br.com.tec.tecmotors.data.local.migration.RoomMigrations
import br.com.tec.tecmotors.domain.usecase.DecideRemindersUseCase
import br.com.tec.tecmotors.domain.usecase.ReminderAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Lembretes do TecMotors.
 *
 * Um unico alarme diario acorda o app; ele so publica notificacao quando
 * [DecideRemindersUseCase] encontra um motivo concreto. Cada notificacao vem
 * com acao direta - o usuario resolve dali, sem navegar pelo app.
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "tec_motors_reminders"
    private const val DAILY_ALARM_REQUEST_CODE = 42010

    private const val NOTIFICATION_ID_MAINTENANCE_BASE = 71_000
    private const val NOTIFICATION_ID_MONTH_END_BASE = 72_000
    private const val NOTIFICATION_ID_REFUEL_GAP_BASE = 73_000

    private const val MAX_NOTIFICATIONS_PER_RUN = 3

    /** Alarmes antigos (2x ao dia) que precisam ser cancelados no update. */
    private const val LEGACY_ODOMETER_ALARM_REQUEST_CODE = 42011

    const val EXTRA_OPEN_QUICK_REFUEL = "br.com.tec.tecmotors.OPEN_QUICK_REFUEL"
    const val EXTRA_OPEN_ODOMETER = "br.com.tec.tecmotors.OPEN_ODOMETER"

    fun initialize(context: Context) {
        createChannel(context)
        cancelLegacyAlarms(context)
        scheduleDailyCheck(context)
    }

    fun scheduleDailyCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }.timeInMillis

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger,
            AlarmManager.INTERVAL_DAY,
            dailyPendingIntent(context)
        )
    }

    /**
     * Avalia o estado local e publica no maximo [MAX_NOTIFICATIONS_PER_RUN]
     * lembretes. Sem motivo, nao publica nada.
     */
    suspend fun publishDueRemindersIfNeeded(context: Context, now: LocalDate = LocalDate.now()) {
        if (!hasNotificationPermission(context)) return

        val alerts = loadAlerts(context, now)
        if (alerts.isEmpty()) return

        createChannel(context)
        val manager = NotificationManagerCompat.from(context)

        alerts.take(MAX_NOTIFICATIONS_PER_RUN).forEach { alert ->
            val (id, notification) = buildNotification(context, alert, now)
            manager.notify(id, notification)
        }
    }

    private suspend fun loadAlerts(context: Context, now: LocalDate): List<ReminderAlert> {
        val database = buildReminderDb(context)
        return try {
            DecideRemindersUseCase()(
                today = now,
                vehicles = database.vehicleDao().getAll().map { it.toDomain() },
                fuelRecords = database.fuelDao().getAll().map { it.toDomain() },
                odometerRecords = database.odometerDao().getAll().map { it.toDomain() },
                maintenanceRecords = database.maintenanceDao().getAll().map { it.toDomain() }
            )
        } finally {
            database.close()
        }
    }

    private fun buildNotification(
        context: Context,
        alert: ReminderAlert,
        now: LocalDate
    ): Pair<Int, android.app.Notification> = when (alert) {
        is ReminderAlert.MaintenanceDueSoon -> {
            val id = NOTIFICATION_ID_MAINTENANCE_BASE + alert.recordId.toInt()
            val title = if (alert.kmRemaining < 0) {
                context.getString(
                    R.string.notif_title_maintenance_overdue,
                    alert.vehicleName,
                    alert.title,
                    formatKm(abs(alert.kmRemaining))
                )
            } else {
                context.getString(
                    R.string.notif_title_maintenance_due,
                    alert.vehicleName,
                    alert.title,
                    formatKm(alert.kmRemaining)
                )
            }
            val text = context.getString(
                R.string.notif_text_maintenance_due,
                formatKm(alert.dueOdometerKm),
                formatKm(alert.currentOdometerKm)
            )

            id to baseNotification(context, title, text)
                .addAction(
                    0,
                    context.getString(R.string.notif_action_already_done),
                    ReminderActionReceiver.markMaintenanceDoneIntent(context, alert.recordId, id)
                )
                .addAction(
                    0,
                    context.getString(R.string.notif_action_details),
                    openAppPendingIntent(context, requestCode = id)
                )
                .build()
        }

        is ReminderAlert.MonthEndWithoutOdometer -> {
            val id = NOTIFICATION_ID_MONTH_END_BASE + alert.vehicleId.toInt()
            val title = context.getString(R.string.notif_title_month_end_odometer)
            val text = context.getString(
                R.string.notif_text_month_end_odometer,
                now.format(monthFormatter)
            )

            id to baseNotification(context, title, text)
                .addAction(
                    0,
                    context.getString(R.string.notif_action_register_now),
                    openAppPendingIntent(context, requestCode = id, openOdometer = true)
                )
                .addAction(
                    0,
                    context.getString(R.string.notif_action_snooze),
                    ReminderActionReceiver.dismissIntent(context, id)
                )
                .build()
        }

        is ReminderAlert.NoRecentRefuel -> {
            val id = NOTIFICATION_ID_REFUEL_GAP_BASE + alert.vehicleId.toInt()
            val title = context.getString(
                R.string.notif_title_no_recent_refuel,
                alert.daysSinceLastRefuel
            )
            val text = if (alert.lastStationName.isBlank()) {
                context.getString(R.string.notif_text_no_recent_refuel_no_station)
            } else {
                context.getString(R.string.notif_text_no_recent_refuel, alert.lastStationName)
            }

            id to baseNotification(context, title, text)
                .addAction(
                    0,
                    context.getString(R.string.notif_action_register_now),
                    openAppPendingIntent(context, requestCode = id, openQuickRefuel = true)
                )
                .addAction(
                    0,
                    context.getString(R.string.notif_action_not_refueled),
                    ReminderActionReceiver.dismissIntent(context, id)
                )
                .build()
        }
    }

    private fun baseNotification(
        context: Context,
        title: String,
        text: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppPendingIntent(context, requestCode = title.hashCode()))
    }

    private fun openAppPendingIntent(
        context: Context,
        requestCode: Int,
        openQuickRefuel: Boolean = false,
        openOdometer: Boolean = false
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openQuickRefuel) putExtra(EXTRA_OPEN_QUICK_REFUEL, true)
            if (openOdometer) putExtra(EXTRA_OPEN_ODOMETER, true)
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    internal fun buildReminderDb(context: Context): TecMotorsDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TecMotorsDatabase::class.java,
            "tec_motors.db"
        ).addMigrations(*RoomMigrations.ALL).build()
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun dailyPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            DAILY_ALARM_REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Remove o alarme das 19h30 de versoes anteriores. */
    private fun cancelLegacyAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val legacy = PendingIntent.getBroadcast(
            context,
            LEGACY_ODOMETER_ALARM_REQUEST_CODE,
            Intent(context, OdometerReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(legacy)
        legacy.cancel()
    }

    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", ptBr)

    private fun formatKm(value: Double): String {
        val formatter = NumberFormat.getNumberInstance(ptBr)
        formatter.maximumFractionDigits = 0
        return formatter.format(value)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.publishDueRemindersIfNeeded(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Recebe os botoes das notificacoes. */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, -1) ?: -1
        if (notificationId > 0) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }

        when (intent?.action) {
            ACTION_MARK_MAINTENANCE_DONE -> {
                val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
                if (recordId <= 0L) return

                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    val database = ReminderScheduler.buildReminderDb(appContext)
                    try {
                        database.maintenanceDao().updateDone(recordId, true)
                    } finally {
                        database.close()
                        pendingResult.finish()
                    }
                }
            }

            else -> Unit // ACTION_DISMISS: cancelar a notificacao ja basta
        }
    }

    companion object {
        private const val ACTION_MARK_MAINTENANCE_DONE =
            "br.com.tec.tecmotors.action.MARK_MAINTENANCE_DONE"
        private const val ACTION_DISMISS = "br.com.tec.tecmotors.action.DISMISS"
        private const val EXTRA_RECORD_ID = "record_id"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun markMaintenanceDoneIntent(
            context: Context,
            recordId: Long,
            notificationId: Int
        ): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ACTION_MARK_MAINTENANCE_DONE
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun dismissIntent(context: Context, notificationId: Int): PendingIntent {
            val intent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            }
            return PendingIntent.getBroadcast(
                context,
                notificationId + 500_000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

/** Mantido apenas para cancelar o alarme agendado por versoes anteriores. */
class OdometerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) = Unit
}

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.initialize(context)
        }
    }
}
