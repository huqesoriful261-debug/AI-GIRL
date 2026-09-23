package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZoyaCyan
import com.example.ui.theme.ZoyaMagenta
import com.example.ui.theme.ZoyaSurfaceVariant
import com.example.ui.theme.ZoyaTextWhite

@Composable
fun QuickPromptsBar(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        "Are you flirting with me? 😏",
        "Turn on the flashlight ⚡",
        "Roast my current mood 💅",
        "Open YouTube 📺",
        "Give me a sassy pep talk ✨",
        "Why are boys so weird? 🙄",
        "Search what's trending 🔍"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ZoyaMagenta.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = ZoyaCyan,
                modifier = Modifier.size(16.dp)
            )
        }

        prompts.forEachIndexed { index, prompt ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(ZoyaSurfaceVariant.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = if (index % 2 == 0) ZoyaMagenta.copy(alpha = 0.35f) else ZoyaCyan.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onPromptSelected(prompt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("quick_prompt_chip_$index")
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ZoyaTextWhite,
                        fontSize = 13.sp
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
    }
}
