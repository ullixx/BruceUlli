package bruce.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Temporäres Ereignisprotokoll für autorisierte Portal-Tests; speichert keine Zugangsdaten. */
@Composable
fun LocalPortalTestLogDialog(onDismiss: () -> Unit) {
    val entries = remember { mutableStateListOf<String>() }
    var event by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lokales Testprotokoll") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Testereignisse manuell protokollieren. Keine Passwörter oder Zugangsdaten eingeben.")
                OutlinedTextField(value = event, onValueChange = { event = it },
                    label = { Text("Ereignis (z. B. Portal geöffnet)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("Notiz (ohne Zugangsdaten)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (event.isNotBlank()) {
                        val stamp = formatter.format(Date())
                        entries.add(0, "$stamp | $event" + if (note.isBlank()) "" else " | $note")
                        event = ""
                        note = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Ereignis speichern") }
                Text("Sitzungsprotokoll (" + entries.size + ")")
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                    items(entries) { item -> Text(item, modifier = Modifier.padding(vertical = 3.dp)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { entries.clear() }) { Text("Protokoll löschen") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schließen") } }
    )
}
