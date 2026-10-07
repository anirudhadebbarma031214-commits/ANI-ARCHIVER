package com.anistudio.archiver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AniArchiverApp() }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color(0xFF6E8CFF).copy(alpha = 0.07f),
                        Color.White.copy(alpha = 0.035f)
                    )
                ),
                RoundedCornerShape(26.dp)
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.18f),
                RoundedCornerShape(26.dp)
            )
            .padding(20.dp),
        content = content
    )
}

@Composable
fun AniArchiverApp() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7C8CFF),
            secondary = Color(0xFFB06CFF),
            background = Color(0xFF05060A),
            surface = Color(0xFF10131C)
        )
    ) {
        Column(
            Modifier.fillMaxSize()
                .background(Color(0xFF05060A))
                .padding(18.dp)
        ) {
            Text("ANI ARCHIVER", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("YOUR FILES. YOUR ARCHIVES.", fontSize = 11.sp, color = Color(0xFFAAB4D4))
            Spacer(Modifier.height(22.dp))

            GlassCard {
                Text("FILE MANAGER", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Text("Browse, copy, move, rename and manage your files.", color = Color(0xFFC2C8DC))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("OPEN FILES") }
            }

            Spacer(Modifier.height(16.dp))
            GlassCard {
                Text("ARCHIVE CENTER", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Text("Create and extract archives. ZIP support is planned for v1.", color = Color(0xFFC2C8DC))
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("ARCHIVE TOOLS") }
            }

            Spacer(Modifier.height(16.dp))
            GlassCard {
                Text("ABOUT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Created by ANIRUDDHA DEBBARMA", color = Color.White, fontWeight = FontWeight.Bold)
                Text("ANI STUDIO", color = Color(0xFF8FA8FF), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.weight(1f))
            Text("ANI ARCHIVER v1.0", color = Color(0xFF697188), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
