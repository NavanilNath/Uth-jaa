package app.uthja

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UthJaTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HomeScreen(refresh) {
                        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh++
        // If an alarm is still ringing, go straight back to the challenges.
        if (AlarmService.isRinging) startActivity(AlarmService.ringIntent(this, AlarmService.currentId))
    }
}

data class SetupIssue(val title: String, val detail: String, val fix: () -> Unit)

@SuppressLint("BatteryLife")
fun setupIssues(ctx: Context, requestNotif: () -> Unit): List<SetupIssue> {
    val pkg = Uri.parse("package:${ctx.packageName}")
    val list = mutableListOf<SetupIssue>()
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        list += SetupIssue("Allow notifications", "Needed to show the alarm.", requestNotif)
    }
    if (Build.VERSION.SDK_INT >= 34 && !ctx.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()) {
        list += SetupIssue("Allow full-screen alarms", "Lets the alarm take over your lock screen.") {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg))
        }
    }
    if (Build.VERSION.SDK_INT >= 31 && !ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
        list += SetupIssue("Allow exact alarms", "So it rings on the exact minute.") {
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg))
        }
    }
    if (!ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)) {
        list += SetupIssue("Turn off battery saving for Uth Ja!", "Stops your phone from killing the alarm overnight.") {
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg))
        }
    }
    return list
}

fun ringsInText(millis: Long): String {
    val mins = ((millis - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
    val h = mins / 60; val m = mins % 60
    return if (h > 0) "Rings in $h h $m min" else "Rings in $m min"
}

@Composable
fun HomeScreen(refreshKey: Int, requestNotif: () -> Unit) {
    val ctx = LocalContext.current
    var alarms by remember { mutableStateOf(AlarmStore.all(ctx)) }
    var issues by remember { mutableStateOf(listOf<SetupIssue>()) }
    var showAdd by remember { mutableStateOf(false) }
    LaunchedEffect(refreshKey) {
        alarms = AlarmStore.all(ctx)
        issues = setupIssues(ctx, requestNotif)
    }
    val today = remember { Challenges.planFor(LocalDate.now()) }

    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("Uth Ja!", fontSize = 44.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Text("The alarm that won't let you sleep in. No snooze.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Today's challenges", fontWeight = FontWeight.SemiBold)
                    Text(today.joinToString("  ·  ") { "${it.emoji} ${it.title}" }, fontSize = 17.sp)
                    Text("Changes every day.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
        items(issues, key = { it.title }) { issue ->
            Card(
                Modifier.fillMaxWidth().clickable { issue.fix() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ ${issue.title}", fontWeight = FontWeight.SemiBold)
                    Text(issue.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tap to fix", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) { Text("+ Add alarm") }
                OutlinedButton(
                    onClick = {
                        Scheduler.scheduleAt(ctx, Scheduler.TEST_ID, System.currentTimeMillis() + 10_000)
                        Toast.makeText(ctx, "Test alarm in 10 seconds. Lock your phone!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Test in 10 sec") }
            }
        }
        if (alarms.isEmpty()) {
            item { Text("No alarms yet. Add one above.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(alarms, key = { it.id }) { a ->
            AlarmRow(
                alarm = a,
                onToggle = { on ->
                    val updated = a.copy(enabled = on)
                    AlarmStore.upsert(ctx, updated)
                    Scheduler.schedule(ctx, updated)
                    if (on) Toast.makeText(ctx, ringsInText(Scheduler.nextTrigger(updated)), Toast.LENGTH_SHORT).show()
                    alarms = AlarmStore.all(ctx)
                },
                onDelete = {
                    Scheduler.cancel(ctx, a.id)
                    AlarmStore.delete(ctx, a.id)
                    alarms = AlarmStore.all(ctx)
                },
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showAdd) {
        AddAlarmDialog(
            onDismiss = { showAdd = false },
            onSave = { h, m, days ->
                val alarm = Alarm(AlarmStore.newId(ctx), h, m, days, true)
                AlarmStore.upsert(ctx, alarm)
                Scheduler.schedule(ctx, alarm)
                Toast.makeText(ctx, ringsInText(Scheduler.nextTrigger(alarm)), Toast.LENGTH_SHORT).show()
                alarms = AlarmStore.all(ctx)
                showAdd = false
            },
        )
    }
}

@Composable
fun AlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    alarm.timeText(), fontSize = 34.sp, fontWeight = FontWeight.Bold,
                    color = if (alarm.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(alarm.daysText(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onDelete) { Text("Delete") }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(onDismiss: () -> Unit, onSave: (Int, Int, Set<Int>) -> Unit) {
    val now = remember { Calendar.getInstance() }
    val state = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = false,
    )
    var days by remember { mutableStateOf(setOf<Int>()) }
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(state.hour, state.minute, days) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New alarm") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Text("Repeat (leave empty for once)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    labels.forEachIndexed { i, label ->
                        val day = i + 1
                        val selected = day in days
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { days = if (selected) days - day else days + day },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
    )
}
