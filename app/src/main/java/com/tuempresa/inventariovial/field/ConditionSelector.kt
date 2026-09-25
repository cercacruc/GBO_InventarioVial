package com.tuempresa.inventariovial.field

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.inventariovial.ui.theme.*

@Composable
fun ConditionSelector(title:String,selected:String,descriptions:Map<String,String>,onSelect:(String)->Unit) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(title,style=MaterialTheme.typography.titleLarge)
        val functional=title.contains("funcional",ignoreCase=true)
        listOf("1" to if(functional) "BUENA" else "BUENO","2" to "REGULAR","3" to if(functional) "MALA" else "MALO").forEach {(code,name)->
            val active=selected==code
            val color=when(code){"1"->FieldSuccess;"2"->FieldPending;else->MaterialTheme.colorScheme.error}
            OutlinedCard(onClick={onSelect(code)},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp),
                border=BorderStroke(if(active)2.dp else 1.dp,if(active)color else MaterialTheme.colorScheme.outlineVariant),
                colors=CardDefaults.outlinedCardColors(containerColor=if(active)color.copy(alpha=0.08f) else MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("${if(active) "✓" else "○"} $code · $name${if(active) " · Seleccionado" else ""}",style=MaterialTheme.typography.titleMedium,color=if(active)color else MaterialTheme.colorScheme.onSurface)
                    descriptions[code]?.let {Text(it,style=MaterialTheme.typography.bodyMedium)}
                }
            }
        }
    }
}
