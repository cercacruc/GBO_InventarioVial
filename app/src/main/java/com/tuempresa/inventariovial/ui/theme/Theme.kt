package com.tuempresa.inventariovial.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColorScheme=lightColorScheme(
    primary=GboBlue,onPrimary=Color.White,primaryContainer=Color(0xFFE0E9F8),onPrimaryContainer=Color(0xFF123362),
    secondary=GboRed,onSecondary=Color.White,secondaryContainer=Color(0xFFFBE4EA),onSecondaryContainer=Color(0xFF7B102B),
    tertiary=FieldSuccess,background=GboBackground,onBackground=Color(0xFF18293D),
    surface=Color.White,onSurface=Color(0xFF18293D),surfaceVariant=Color(0xFFE8EDF4),onSurfaceVariant=Color(0xFF4E5E71),
    surfaceContainer=Color.White,surfaceContainerLow=Color.White,surfaceContainerHighest=Color(0xFFE8EDF4),
    outline=Color(0xFF738298),outlineVariant=Color(0xFFD5DDE8),error=Color(0xFFAE2334),errorContainer=Color(0xFFFFE8EA))
private val DarkColorScheme=darkColorScheme(primary=Color(0xFFAEC8F4),secondary=Color(0xFFFFB1C0),background=Color(0xFF132030),surface=Color(0xFF1B2A3D))

@Composable
fun InventarioVialTheme(darkTheme:Boolean=false,dynamicColor:Boolean=false,content:@Composable ()->Unit) {
    MaterialTheme(colorScheme=if(darkTheme)DarkColorScheme else LightColorScheme,typography=Typography,
        shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(20.dp)),content=content)
}
