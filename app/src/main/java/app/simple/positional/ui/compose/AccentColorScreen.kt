package app.simple.positional.ui.compose

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.util.ColorUtils.toHex

@Immutable
data class AccentOption(@ColorInt val color: Int, val name: String, val dynamic: Boolean = false)

object AccentPalette {
    const val MATERIAL_YOU = "Material You (Dynamic)"

    val colors = listOf(
        R.color.positional to "Positional",
        R.color.blue to "Blue",
        R.color.blueGrey to "Blue Grey",
        R.color.darkBlue to "Dark Blue",
        R.color.red to "Red",
        R.color.green to "Green",
        R.color.orange to "Orange",
        R.color.purple to "Purple",
        R.color.yellow to "Yellow",
        R.color.caribbeanGreen to "Caribbean Green",
        R.color.persianGreen to "Persian Green",
        R.color.amaranth to "Amaranth",
        R.color.indian_red to "Indian Red",
        R.color.light_coral to "Light Coral",
        R.color.pink_flare to "Pink Flare",
        R.color.makeup_tan to "Makeup Tan",
        R.color.egg_yellow to "Egg Yellow",
        R.color.medium_green to "Medium Green",
        R.color.olive to "Olive",
        R.color.copperfield to "Copperfield",
        R.color.mineral_green to "Mineral Green",
        R.color.lochinvar to "Lochinvar",
        R.color.beach_grey to "Beach Grey",
    )

    fun options(context: Context): List<AccentOption> = buildList {
        colors.forEach { (res, name) -> add(AccentOption(context.getColor(res), name)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(1, AccentOption(context.getColor(android.R.color.system_accent1_500), MATERIAL_YOU, dynamic = true))
        }
    }

    fun isSelected(option: AccentOption, @ColorInt selected: Int, materialYou: Boolean): Boolean =
        if (materialYou) option.dynamic else !option.dynamic && option.color == selected
}

@Composable
fun AccentColorScreen(
    options: List<AccentOption>,
    @ColorInt selected: Int,
    materialYou: Boolean,
    topInset: Dp,
    onSelect: (AccentOption) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset, bottom = 16.dp)) {
            item {
                Text(
                    stringResource(R.string.accent_colors),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 16.dp),
                )
            }
            item {
                Text(
                    "${stringResource(R.string.total)} ${options.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            items(options, key = { it.name }) { option ->
                AccentRow(option, AccentPalette.isSelected(option, selected, materialYou)) { onSelect(option) }
            }
        }
    }
}

@Composable
private fun AccentRow(option: AccentOption, selected: Boolean, onClick: () -> Unit) {
    val color = Color(option.color)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 100.dp, height = 70.dp)
                .shadow(5.dp, shape, ambientColor = color, spotColor = color)
                .background(color, shape),
        ) {
            if (selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(option.name, style = MaterialTheme.typography.titleMedium)
            Text(
                option.color.toHex(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
