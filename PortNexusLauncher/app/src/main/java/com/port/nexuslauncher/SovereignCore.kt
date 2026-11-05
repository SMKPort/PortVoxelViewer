package com.port.nexuslauncher

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun SovereignCore(mode: NexusMode, animated: Boolean, visual: SovereignVisual, onClick: () -> Unit) {
    val phase = if (animated) {
        val t = rememberInfiniteTransition(label = "sovereign")
        val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(36000, easing = LinearEasing)), label = "orbit")
        angle
    } else 0f
    Box(Modifier.fillMaxWidth().height(if (visual.compact) 192.dp else 284.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = min(size.width * .36f, size.height * .44f)
            drawCircle(Brush.radialGradient(listOf(mode.primary.copy(alpha = .20f), Color.Transparent), c, r * 1.5f), r * 1.5f, c)
            if (visual.core == "SERAPH") {
                for (side in listOf(-1f, 1f)) repeat(3) { wing ->
                    repeat(9) { feather ->
                        val start = Offset(c.x + side * r * .36f, c.y + (wing - 1) * r * .24f)
                        val tip = Offset(c.x + side * r * (1.12f + feather * .028f), c.y + (wing - 1) * r * .70f - r * .48f + feather * r * .10f)
                        drawLine(if (wing == 1) mode.primary.copy(alpha = .40f) else mode.gold.copy(alpha = .35f), start, tip, (9 - feather) * .65.dp.toPx(), StrokeCap.Round)
                    }
                }
            }
            rotate(phase, c) {
                drawCircle(mode.gold.copy(alpha = .5f), r, c, style = Stroke(1.dp.toPx()))
                repeat(72) { i ->
                    val a = i * PI / 36
                    val inner = if (i % 6 == 0) .90f else .96f
                    drawLine(mode.gold.copy(alpha = if (i % 6 == 0) .85f else .28f),
                        c + Offset(cos(a).toFloat(), sin(a).toFloat()) * (r * inner),
                        c + Offset(cos(a).toFloat(), sin(a).toFloat()) * r, 1.dp.toPx())
                }
                repeat(6) { i -> drawArc(mode.primary.copy(alpha = .7f), i * 60f, 24f, false,
                    c - Offset(r * .80f, r * .80f), Size(r * 1.6f, r * 1.6f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)) }
            }
            rotate(-phase * 1.4f, c) {
                if (visual.core == "ORBIT") repeat(3) { i -> rotate(i * 60f, c) {
                    drawOval(mode.gold.copy(alpha = .65f), c - Offset(r * .9f, r * .39f), Size(r * 1.8f, r * .78f), style = Stroke(1.dp.toPx()))
                } } else {
                    val path = Path()
                    repeat(7) { i -> val a = i * PI / 3 - PI / 2
                        val x = c.x + cos(a).toFloat() * r * .68f
                        val y = c.y + sin(a).toFloat() * r * .68f
                        if (i == 0) path.moveTo(x,y) else path.lineTo(x,y)
                    }
                    drawPath(path, mode.gold.copy(alpha = .6f), style = Stroke(1.dp.toPx()))
                }
                repeat(18) { i -> val a = i * 2 * PI / 18
                    val radius = r * (1.08f + (i % 3) * .07f)
                    drawCircle(mode.gold.copy(alpha = .5f), (if (i % 3 == 0) 2f else 1f).dp.toPx(), c + Offset(cos(a).toFloat(), sin(a).toFloat()) * radius)
                }
            }
            if (visual.core == "ECLIPSE") drawArc(mode.gold, 210f, 120f, false, c - Offset(r*.61f,r*.61f), Size(r*1.22f,r*1.22f), style = Stroke(7.dp.toPx()))
        }
        Column(Modifier.size(if (visual.compact) 88.dp else 112.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(mode.primary.copy(alpha = .60f), Color(0xFF100A15))))
            .border(1.dp, mode.gold.copy(alpha = .8f), CircleShape), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (visual.core == "SERAPH") "♛" else mode.glyph, fontSize = 32.sp, color = mode.gold)
            Text("NEXUS", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 2.sp, color = Color.White)
            Text("SOVEREIGN", fontSize = 7.sp, letterSpacing = 1.sp, color = mode.gold)
        }
    }
}
