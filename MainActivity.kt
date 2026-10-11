package com.ecetprep.app

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val Context.ds by preferencesDataStore("settings")
val EXAM = longPreferencesKey("exam_epoch_day")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDb.get(this).dao()
        setContent {
            LaunchedEffect(Unit) {
                if (dao.subjectCount() == 0)
                    listOf("Mathematics", "Physics", "Chemistry", "Electronics and Communication Engineering (ECE)")
                        .forEach { dao.insertSubject(Subject(name = it)) }
            }
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Home(dao) }
            }
        }
    }
}

@Composable
fun Home(dao: PrepDao) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val subjects by dao.subjects().collectAsState(emptyList())
    val topics by dao.topics().collectAsState(emptyList())
    val examDay by ctx.ds.data.map { it[EXAM] }.collectAsState(null)
    var addFor by remember { mutableStateOf<Subject?>(null) }
    var text by remember { mutableStateOf("") }

    fun pickDate() {
        val d = examDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
        DatePickerDialog(ctx, { _, y, m, day ->
            scope.launch { ctx.ds.edit { it[EXAM] = LocalDate.of(y, m + 1, day).toEpochDay() } }
        }, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }

    LazyColumn(Modifier.statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("ECET PrepMate", style = MaterialTheme.typography.headlineMedium)
            Text("Learn. Revise. Practice. Succeed.", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val days = examDay?.let { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(it)) }
                    Text(if (days == null) "Exam date not set" else if (days >= 0) "$days days to ECET" else "Exam date has passed",
                        style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pickDate() }) { Text(if (days == null) "Set exam date" else "Change exam date") }
                }
            }
        }
        item {
            val done = topics.count { it.completed }
            Text(if (topics.isEmpty()) "Overall progress: no topics added yet"
                 else "Overall progress: $done of ${topics.size} topics (${done * 100 / topics.size}%)",
                 style = MaterialTheme.typography.titleMedium)
        }
        items(subjects, key = { it.id }) { s ->
            val mine = topics.filter { it.subjectId == s.id }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium)
                    val frac = if (mine.isEmpty()) 0f else mine.count { it.completed }.toFloat() / mine.size
                    LinearProgressIndicator(progress = { frac }, Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    mine.forEach { t ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(t.completed, { c -> scope.launch { dao.update(t.copy(completed = c)) } })
                            Text(t.name, Modifier.weight(1f))
                            TextButton({ scope.launch { dao.delete(t) } }) { Text("Delete") }
                        }
                    }
                    TextButton({ text = ""; addFor = s }) { Text("+ Add topic") }
                }
            }
        }
    }

    addFor?.let { s ->
        AlertDialog(onDismissRequest = { addFor = null },
            title = { Text("New topic in ${s.name}") },
            text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Topic name") }) },
            confirmButton = { TextButton({
                if (text.isNotBlank()) scope.launch { dao.insertTopic(Topic(subjectId = s.id, name = text.trim())) }
                addFor = null }) { Text("Add") } },
            dismissButton = { TextButton({ addFor = null }) { Text("Cancel") } })
    }
}
