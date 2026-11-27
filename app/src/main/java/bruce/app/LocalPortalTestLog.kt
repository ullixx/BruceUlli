package bruce.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PortalLogEntry(val timestamp: String, val event: String, val note: String)

private fun encodePortalEntries(entries: List<PortalLogEntry>): String {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(JSONObject().put("timestamp", entry.timestamp).put("event", entry.event).put("note", entry.note))
    }
    return array.toString()
}

private fun decodePortalEntries(json: String): List<PortalLogEntry> {
    val array = JSONArray(json)
    return (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        PortalLogEntry(item.optString("timestamp"), item.optString("event"), item.optString("note"))
    }
}

/** Lokales Ereignisprotokoll. Speichert nur manuell eingegebene Ereignisse/Notizen, keine Zugangsdaten. */
@Composable
fun LocalPortalTestLogDialog(context: Context, isGerman: Boolean, onDismiss: () -> Unit) {
    val prefs = remember { context.getSharedPreferences("bruce_portal_test_log", Context.MODE_PRIVATE) }
    val entries = remember {
        mutableStateListOf<PortalLogEntry>().apply {
            val saved = prefs.getString("entries", "[]") ?: "[]"
            try { addAll(decodePortalEntries(saved)) } catch (_: Exception) { /* ignore invalid local log */ }
        }
    }
    var event by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf(-1) }
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY) }

    fun persist() {
        prefs.edit().putString("entries", encodePortalEntries(entries)).apply()
    }
    fun exportTo(uri: Uri?) {
        if (uri == null) return
        try { context.contentResolver.openOutputStream(uri)?.use { it.write(encodePortalEntries(entries).toByteArray(Charsets.UTF_8)) } } catch (_: Exception) { }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: "[]"
                entries.clear()
                entries.addAll(decodePortalEntries(json))
                persist()
            } catch (_: Exception) { /* keep current log if import is invalid */ }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isGerman) "Lokales Testprotokoll" else "Local test log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isGerman) "Ereignisse und Notizen werden lokal auf diesem Gerät gespeichert. Keine Passwörter oder Zugangsdaten eingeben." else "Events and notes are stored locally on this device. Do not enter passwords or credentials.")
                OutlinedTextField(value = event, onValueChange = { event = it },
                    label = { Text(if (isGerman) "Ereignis (z. B. Portal geöffnet)" else "Event (e.g. portal opened)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text(if (isGerman) "Notiz (ohne Zugangsdaten)" else "Note (no credentials)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    if (event.isNotBlank()) {
                        val stamp = formatter.format(Date())
                        val entry = PortalLogEntry(if (editingIndex >= 0) entries[editingIndex].timestamp else stamp, event.trim(), note.trim())
                        if (editingIndex >= 0 && editingIndex < entries.size) entries[editingIndex] = entry else entries.add(0, entry)
                        editingIndex = -1
                        event = ""
                        note = ""
                        persist()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isGerman) if (editingIndex >= 0) "Änderung speichern" else "Ereignis speichern" else if (editingIndex >= 0) "Save changes" else "Save event")
                }
                Text((if (isGerman) "Gespeicherte Einträge (" else "Saved entries (") + entries.size + ")")
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)) {
                    items(entries) { item ->
                        Column(modifier = Modifier.fillMaxWidth().clickable {
                            editingIndex = entries.indexOf(item)
                            event = item.event
                            note = item.note
                        }.padding(vertical = 5.dp)) {
                            Text(item.timestamp)
                            Text(item.event)
                            if (item.note.isNotBlank()) Text(item.note)
                            TextButton(onClick = {
                                entries.remove(item)
                                if (editingIndex >= entries.size) editingIndex = -1
                                persist()
                            }) { Text(if (isGerman) "Eintrag löschen" else "Delete entry") }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { exportLauncher.launch("bruce-testprotokoll.json") }) { Text(if (isGerman) "Export" else "Export") }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }) { Text(if (isGerman) "Import" else "Import") }
                    TextButton(onClick = { entries.clear(); editingIndex = -1; persist() }) { Text(if (isGerman) "Alle löschen" else "Clear all") }
                }
                Text(if (isGerman) "Tippe einen Eintrag an, um ihn zu bearbeiten. Exportiere regelmäßig eine Sicherung." else "Tap an entry to edit it. Export backups regularly.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (isGerman) "Schließen" else "Close") } }
    )
}
