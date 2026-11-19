package app.uthja

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

    val next: () -> Unit = {
        if (!finished) {
            if (round + 1 >= plan.size) { finished = true; onDone() } else round++
        }
    }
    val type = plan[round]

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            clock.format(DateTimeFormatter.ofPattern("h:mm a")),
            fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        )
        Text("UTH JA! Solve to stop the alarm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text(
            if (emergency) "🚨 Emergency exit: 20 in a row"
            else "Round ${round + 1} of ${plan.size} · ${type.emoji} ${type.title}",
            fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Box(Modifier.fillMaxWidth()) {
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

        Spacer(Modifier.height(32.dp))
        if (!emergency) {
            TextButton(onClick = { emergency = true }) {
                Text("Stuck? Emergency exit (20 math problems in a row)", textAlign = TextAlign.Center)
            }
        }
    }
}
