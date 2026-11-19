package app.uthja

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.sqrt
import kotlin.random.Random

enum class ChallengeType(val title: String, val emoji: String) {
    MATH("Math", "🧮"),
    QUIZ("GK Quiz", "🧠"),
    SHAKE("Dance / Shake", "💃"),
    MEMORY("Memory", "🔢"),
}

object Challenges {
    const val ROUNDS = 3

    /** Today's mix: same all day, different every day. */
    fun planFor(date: LocalDate): List<ChallengeType> =
        ChallengeType.entries.shuffled(Random(date.toEpochDay() * 7919L)).take(ROUNDS)
}

// ---------------------------------------------------------------- Math

data class MathProblem(val text: String, val answer: Int) {
    companion object {
        fun random(): MathProblem {
            val r = Random.Default
            return when (r.nextInt(3)) {
                0 -> {
                    val a = r.nextInt(12, 50); val b = r.nextInt(3, 10); val c = r.nextInt(10, 100)
                    MathProblem("$a × $b + $c", a * b + c)
                }
                1 -> {
                    val a = r.nextInt(100, 1000); val b = r.nextInt(100, 1000)
                    MathProblem("$a + $b", a + b)
                }
                else -> {
                    val a = r.nextInt(300, 1000); val b = r.nextInt(100, a)
                    MathProblem("$a − $b", a - b)
                }
            }
        }
    }
}

@Composable
fun MathChallenge(target: Int, onSolved: () -> Unit) {
    var problem by remember { mutableStateOf(MathProblem.random()) }
    var answer by remember { mutableStateOf("") }
    var streak by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Get $target right in a row · $streak/$target", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(problem.text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it.filter(Char::isDigit).take(6) },
            label = { Text("Answer") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Button(
            onClick = {
                if (done) return@Button
                if (answer.toIntOrNull() == problem.answer) {
                    streak++
                    msg = "✅ Correct"
                    if (streak >= target) { done = true; onSolved() }
                } else {
                    msg = "❌ It was ${problem.answer}. Streak reset."
                    streak = 0
                }
                problem = MathProblem.random()
                answer = ""
            },
            enabled = answer.isNotEmpty(),
        ) { Text("Submit") }
        Text(msg)
    }
}

// ---------------------------------------------------------------- GK quiz

data class Question(val q: String, val options: List<String>, val answer: String)

object GkBank {
    // First option is always the correct one; options are shuffled on screen.
    private fun q(text: String, vararg opts: String) = Question(text, opts.toList(), opts[0])

    val questions = listOf(
        q("Capital of Australia?", "Canberra", "Sydney", "Melbourne", "Perth"),
        q("Largest planet in our solar system?", "Jupiter", "Saturn", "Neptune", "Earth"),
        q("Who wrote Gitanjali?", "Rabindranath Tagore", "Bankim Chandra Chattopadhyay", "Sarat Chandra Chattopadhyay", "Kazi Nazrul Islam"),
        q("Chemical symbol for gold?", "Au", "Ag", "Gd", "Go"),
        q("India's national aquatic animal?", "Ganges river dolphin", "Gharial", "Olive ridley turtle", "Mahseer"),
        q("How many bones in an adult human body?", "206", "201", "212", "196"),
        q("Which planet is called the Red Planet?", "Mars", "Venus", "Mercury", "Jupiter"),
        q("First Indian to win a Nobel Prize?", "Rabindranath Tagore", "C. V. Raman", "Amartya Sen", "Mother Teresa"),
        q("Who is called the 'Missile Man of India'?", "A. P. J. Abdul Kalam", "Vikram Sarabhai", "Homi J. Bhabha", "Satish Dhawan"),
        q("Smallest prime number?", "2", "1", "3", "0"),
        q("Gas that plants absorb for photosynthesis?", "Carbon dioxide", "Oxygen", "Nitrogen", "Hydrogen"),
        q("Hardest natural substance?", "Diamond", "Quartz", "Iron", "Graphite"),
        q("Which country gifted the Statue of Liberty to the USA?", "France", "England", "Spain", "Italy"),
        q("Year India became independent?", "1947", "1950", "1942", "1948"),
        q("Largest ocean on Earth?", "Pacific", "Atlantic", "Indian", "Arctic"),
        q("Who painted the Mona Lisa?", "Leonardo da Vinci", "Michelangelo", "Raphael", "Vincent van Gogh"),
        q("Boiling point of water at sea level in °F?", "212", "100", "180", "250"),
        q("Indian state with the longest coastline?", "Gujarat", "Tamil Nadu", "Andhra Pradesh", "Kerala"),
        q("Currency of Japan?", "Yen", "Won", "Yuan", "Ringgit"),
        q("Who wrote 'Vande Mataram'?", "Bankim Chandra Chattopadhyay", "Rabindranath Tagore", "Sarojini Naidu", "Subramania Bharati"),
        q("Square root of 144?", "12", "14", "11", "16"),
        q("Largest organ of the human body?", "Skin", "Liver", "Brain", "Lungs"),
        q("Largest hot desert in the world?", "Sahara", "Thar", "Gobi", "Kalahari"),
        q("Which city is called the 'City of Joy'?", "Kolkata", "Mumbai", "Jaipur", "Lucknow"),
        q("What does CPU stand for?", "Central Processing Unit", "Computer Personal Unit", "Central Program Utility", "Core Processing Unit"),
        q("Who discovered penicillin?", "Alexander Fleming", "Louis Pasteur", "Marie Curie", "Edward Jenner"),
        q("Fastest land animal?", "Cheetah", "Lion", "Horse", "Pronghorn"),
        q("Which Mughal emperor built the Taj Mahal?", "Shah Jahan", "Akbar", "Aurangzeb", "Humayun"),
        q("How many continents are there?", "7", "6", "5", "8"),
        q("Which vitamin does your skin make from sunlight?", "Vitamin D", "Vitamin C", "Vitamin A", "Vitamin B12"),
        q("Pune sits at the meeting of which two rivers?", "Mula and Mutha", "Krishna and Koyna", "Godavari and Pravara", "Bhima and Indrayani"),
        q("How many players does a cricket team have on the field?", "11", "9", "10", "12"),
    )
}

@Composable
fun QuizChallenge(target: Int, onSolved: () -> Unit) {
    val order = remember { GkBank.questions.shuffled() }
    var index by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    val question = order[index % order.size]
    val options = remember(index) { question.options.shuffled() }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Get $target right in a row · $streak/$target", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(question.q, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        options.forEach { opt ->
            OutlinedButton(
                onClick = {
                    if (done) return@OutlinedButton
                    if (opt == question.answer) {
                        streak++
                        msg = "✅ Correct"
                        if (streak >= target) { done = true; onSolved() }
                    } else {
                        msg = "❌ It was ${question.answer}. Streak reset."
                        streak = 0
                    }
                    index++
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(opt, fontSize = 17.sp) }
        }
        Text(msg)
    }
}

// ---------------------------------------------------------------- Dance / shake

@Composable
fun ShakeChallenge(target: Int, onSolved: () -> Unit) {
    val ctx = LocalContext.current
    val sensorManager = remember { ctx.getSystemService(SensorManager::class.java) }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (sensor == null) {
        // No motion sensor on this phone: fall back to math.
        MathChallenge(target = 3, onSolved = onSolved)
        return
    }
    var count by remember { mutableIntStateOf(0) }
    val solved by rememberUpdatedState(onSolved)

    DisposableEffect(Unit) {
        var last = 0L
        var done = false
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = e.values[0]; val y = e.values[1]; val z = e.values[2]
                val g = sqrt(x * x + y * y + z * z)  // ~9.8 when still
                val now = System.currentTimeMillis()
                if (!done && g > 17f && now - last > 250) {
                    last = now
                    count++
                    if (count >= target) { done = true; solved() }
                }
            }
            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Get out of bed and dance!", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Text("Hold the phone and shake it hard with every move.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text("$count / $target", fontSize = 60.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = { (count.toFloat() / target).coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp),
        )
    }
}

// ---------------------------------------------------------------- Memory

@Composable
fun MemoryChallenge(target: Int, digits: Int = 6, onSolved: () -> Unit) {
    fun newNumber() = (1..digits).map { Random.nextInt(10) }.joinToString("")
    var attempt by remember { mutableIntStateOf(0) }
    var number by remember { mutableStateOf(newNumber()) }
    var showing by remember { mutableStateOf(true) }
    var answer by remember { mutableStateOf("") }
    var streak by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(attempt) {
        showing = true
        delay(3000)
        showing = false
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Memorise the number · $streak/$target", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            if (showing) number else "• ".repeat(digits).trim(),
            fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
        )
        if (!showing) {
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it.filter(Char::isDigit).take(digits) },
                label = { Text("Type it from memory") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = {
                    if (done) return@Button
                    if (answer == number) {
                        streak++
                        msg = "✅ Correct"
                        if (streak >= target) { done = true; onSolved(); return@Button }
                    } else {
                        msg = "❌ It was $number. Streak reset."
                        streak = 0
                    }
                    number = newNumber()
                    answer = ""
                    attempt++
                },
                enabled = answer.length == digits,
            ) { Text("Submit") }
        } else {
            Text("Look carefully… 3 seconds", modifier = Modifier.padding(top = 8.dp))
        }
        Text(msg)
    }
}
