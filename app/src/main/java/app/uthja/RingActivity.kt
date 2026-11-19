package app.uthja

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class RingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Back button does nothing. No escape.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        })

        val plan = Challenges.planFor(LocalDate.now())
        setContent {
            UthJaTheme {
                RingScreen(plan) {
                    AlarmService.stop(this)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AlarmService.isRinging) finish()
    }
}

@Composable
fun RingScreen(plan: List<ChallengeType>, onDone: () -> Unit) {
    var round by remember { mutableIntStateOf(0) }
    var emergency by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var clock by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) { clock = LocalTime.now(); delay(1000) }
    }

    // Background pulses between night and an angry sunrise.
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse",
    )
    val bg = Brush.verticalGradient(
        listOf(
            lerp(UJ.Indigo, Color(0xFF6A1236), pulse),
            lerp(UJ.Plum, UJ.Rose, pulse),
            lerp(UJ.Rose, UJ.Orange, pulse),
        )
    )

    val next: () -> Unit = {
        if (!finished) {
            if (round + 1 >= plan.size) { finished = true; onDone() } else round++
        }
    }
    val type = plan[round]

    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("UTH JA!", color = UJ.Gold, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = 6.sp)
        Text(
            clock.format(DateTimeFormatter.ofPattern("h:mm")),
            fontSize = 88.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, lineHeight = 92.sp,
        )
        Text("Solve to stop the alarm", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
        Spacer(Modifier.height(18.dp))

        if (!emergency) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                plan.forEachIndexed { i, t ->
                    val bgc = when {
                        i < round -> UJ.Good
                        i == round -> Color.White
                        else -> Color.Black.copy(alpha = 0.25f)
                    }
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(bgc),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (i < round) "✓" else t.emoji, fontSize = 20.sp, color = UJ.Night, fontWeight = FontWeight.ExtraBold) }
                }
            }
            Spacer(Modifier.height(18.dp))
        }

        GlassCard(Modifier.fillMaxWidth(), color = UJ.Night.copy(alpha = 0.88f)) {
            Text(
                if (emergency) "🚨 Emergency exit" else "Round ${round + 1} of ${plan.size} · ${type.title}",
                color = UJ.Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(14.dp))
            if (emergency) {
                MathChallenge(target = 20) { if (!finished) { finished = true; onDone() } }
            } else {
                key(round) {
                    when (type) {
                        ChallengeType.MATH -> MathChallenge(target = 3, onSolved = next)
                        ChallengeType.QUIZ -> QuizChallenge(target = 3, onSolved = next)
                        ChallengeType.SHAKE -> ShakeChallenge(target = 40, onSolved = next)
                        ChallengeType.MEMORY -> MemoryChallenge(target = 2, onSolved = next)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        if (!emergency) {
            TextButton(onClick = { emergency = true }) {
                Text("Stuck? Emergency exit · 20 math problems in a row", color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
            }
        }
    }
}
