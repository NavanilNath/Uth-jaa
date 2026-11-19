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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private var refresh by mutableIntStateOf(0)
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            UthJaTheme {
                Box(Modifier.fillMaxSize().background(UJ.Night)) {
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
        list += SetupIssue("Turn off battery saving", "Stops your phone from killing the alarm overnight.") {
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

fun greeting(): String = when (LocalTime.now().hour) {
    in 4..11 -> "Good morning ☀️"
    in 12..16 -> "Good afternoon 🌤️"
    in 17..20 -> "Good evening 🌆"
    else -> "Good night 🌙"
}

@OptIn(ExperimentalLayoutApi::class)
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
    val next = alarms.filter { it.enabled }.minOfOrNull { Scheduler.nextTrigger(it) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SunriseHeader(next) }

        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionTitle("Today's challenges")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    today.forEach { Pill("${it.emoji} ${it.title}") }
                }
                Text("A new mix every day.", color = UJ.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        items(issues, key = { it.title }) { issue ->
            GlassCard(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                color = Color(0xFF2E1A2E),
                onClick = issue.fix,
            ) {
                Text("⚠️  ${issue.title}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(issue.detail, color = UJ.Muted)
                Text("Tap to fix →", color = UJ.Orange, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GradientButton("+  New alarm", onClick = { showAdd = true }, modifier = Modifier.weight(1f))
                GhostButton(
                    "Test 10s",
                    onClick = {
                        Scheduler.scheduleAt(ctx, Scheduler.TEST_ID, System.currentTimeMillis() + 10_000)
                        Toast.makeText(ctx, "Test alarm in 10 seconds. Lock your phone!", Toast.LENGTH_LONG).show()
                    },
                )
            }
        }

        item { Box(Modifier.padding(horizontal = 20.dp)) { SectionTitle("Your alarms") } }

        if (alarms.isEmpty()) {
            item {
                GlassCard(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
                    Text("😴  No alarms yet", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("Add one, if you dare.", color = UJ.Muted)
                }
            }
        }
        items(alarms, key = { it.id }) { a ->
            AlarmRow(
                alarm = a,
                modifier = Modifier.padding(horizontal = 20.dp),
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
fun SectionTitle(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

/** Header: a little dawn scene (sky, glow, rising sun, horizon) behind the title. */
@Composable
fun SunriseHeader(nextAlarm: Long?) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(UJ.sky)
            val sun = Offset(size.width * 0.80f, size.height * 0.84f)
            val r = size.width * 0.17f
            drawCircle(
                Brush.radialGradient(listOf(UJ.Gold.copy(alpha = 0.55f), Color.Transparent), center = sun, radius = r * 2.4f),
                radius = r * 2.4f, center = sun,
            )
            drawCircle(Brush.verticalGradient(listOf(UJ.SunLight, Color(0xFFFFB020)), startY = sun.y - r, endY = sun.y + r), radius = r, center = sun)
            listOf(0.15f to 0.22f, 0.45f to 0.12f, 0.62f to 0.30f, 0.30f to 0.40f).forEach { (x, y) ->
                drawCircle(Color.White.copy(alpha = 0.7f), radius = 2.5f, center = Offset(size.width * x, size.height * y))
            }
            val horizon = Path().apply {
                moveTo(0f, size.height * 0.88f)
                quadraticBezierTo(size.width / 2, size.height * 0.78f, size.width, size.height * 0.88f)
                lineTo(size.width, size.height); lineTo(0f, size.height); close()
            }
            drawPath(horizon, UJ.Night)
        }
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 56.dp)
        ) {
            Text(greeting(), color = UJ.Text.copy(alpha = 0.85f), fontSize = 16.sp)
            Text("Uth Ja!", fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 58.sp)
            Text("The alarm that won't let you sleep in.", color = UJ.Text.copy(alpha = 0.85f))
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.28f))
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    if (nextAlarm != null) "⏰  ${ringsInText(nextAlarm)}" else "⏰  No alarm set",
                    color = Color.White, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun AlarmRow(alarm: Alarm, modifier: Modifier = Modifier, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val on = alarm.enabled
    GlassCard(modifier.fillMaxWidth(), color = if (on) UJ.Card else UJ.Night) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val (time, ampm) = alarm.timeText().split(" ").let { it[0] to it.getOrElse(1) { "" } }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(time, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = if (on) UJ.Text else UJ.Muted, lineHeight = 46.sp)
                    Text(" $ampm", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = if (on) UJ.Orange else UJ.Muted, modifier = Modifier.padding(bottom = 7.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (alarm.days.isEmpty()) {
                        Text("Once", color = UJ.Muted, fontWeight = FontWeight.SemiBold)
                    } else {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEachIndexed { i, l ->
                            Text(
                                l,
                                color = if (i + 1 in alarm.days) (if (on) UJ.Orange else UJ.Muted) else UJ.Border,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = on,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = UJ.Orange,
                        checkedBorderColor = UJ.Orange,
                        uncheckedThumbColor = UJ.Muted,
                        uncheckedTrackColor = UJ.CardHi,
                        uncheckedBorderColor = UJ.Border,
                    ),
                )
                Text(
                    "Remove",
                    color = UJ.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete).padding(6.dp),
                )
            }
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
        containerColor = UJ.Card,
        shape = RoundedCornerShape(32.dp),
        confirmButton = { GradientButton("Save", onClick = { onSave(state.hour, state.minute, days) }) },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = UJ.Muted) } },
        title = { Text("New alarm", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = UJ.CardHi,
                        selectorColor = UJ.Orange,
                        clockDialSelectedContentColor = Color(0xFF1A0E08),
                        clockDialUnselectedContentColor = UJ.Text,
                        periodSelectorSelectedContainerColor = UJ.Orange,
                        periodSelectorSelectedContentColor = Color(0xFF1A0E08),
                        periodSelectorUnselectedContentColor = UJ.Muted,
                        periodSelectorBorderColor = UJ.Border,
                        timeSelectorSelectedContainerColor = Color(0xFF5A2E1C),
                        timeSelectorSelectedContentColor = UJ.Gold,
                        timeSelectorUnselectedContainerColor = UJ.CardHi,
                        timeSelectorUnselectedContentColor = UJ.Text,
                    ),
                )
                Text("Repeat (leave empty for once)", color = UJ.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    labels.forEachIndexed { i, label ->
                        val day = i + 1
                        val selected = day in days
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .then(if (selected) Modifier.background(UJ.sunrise) else Modifier.background(UJ.CardHi))
                                .border(1.dp, if (selected) Color.Transparent else UJ.Border, CircleShape)
                                .clickable { days = if (selected) days - day else days + day },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = if (selected) Color(0xFF1A0E08) else UJ.Text, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.width(1.dp))
            }
        },
    )
}
