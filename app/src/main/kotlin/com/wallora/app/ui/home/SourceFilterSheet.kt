package com.wallora.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallora.app.domain.model.SourceId

@Composable
fun SourceFilterSheet(
    enabledSources: Set<SourceId>,
    onToggleSource: (SourceId, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text("Sources", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        SourceId.entries.forEach { source ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Text(
                    text = source.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = source in enabledSources,
                    onCheckedChange = { enabled -> onToggleSource(source, enabled) },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
