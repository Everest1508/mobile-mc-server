package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerStatus
import com.example.ui.theme.*

@Composable
fun StatusBadge(status: ServerStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, dotColor) = when (status) {
        ServerStatus.RUNNING -> Triple(SleekMintContainer, SleekMint, SleekMint)
        ServerStatus.STARTING, ServerStatus.INSTALLING -> Triple(SleekAmberContainer, SleekAmber, SleekAmber)
        ServerStatus.STOPPING -> Triple(Color(0x2EF97316), Color(0xFFF97316), Color(0xFFF97316))
        ServerStatus.CRASHED -> Triple(SleekCoralContainer, SleekCoral, SleekCoral)
        ServerStatus.STOPPED -> Triple(Color(0x20CAC4D0), TextSecondary, TextMuted)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(50)),
        color = bgColor,
        contentColor = textColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = status.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color = SleekPrimary,
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, SleekBorder, RoundedCornerShape(24.dp)),
        color = SleekCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = TextSecondary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp
                    ),
                    color = TextWhite
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }

            if (progress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = SleekOutline
                )
            }
        }
    }
}

@Composable
fun SleekNavTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                if (isSelected) SleekPrimary else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(24.dp)
            ),
        color = if (isSelected) SleekPrimaryContainer else SleekCard
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) SleekOnPrimaryContainer else SleekPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                ),
                color = if (isSelected) SleekOnPrimaryContainer else TextPrimary
            )
        }
    }
}

@Composable
fun MonospacePill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        color = SleekSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall,
            color = SleekPrimary
        )
    }
}
