package app.simple.positional.util

import android.content.Context
import android.os.Build
import androidx.annotation.StyleRes
import app.simple.positional.R
import app.simple.positional.preferences.MainPreferences

object AccentThemes {
    private val themes = listOf(
        R.color.positional to R.style.Positional,
        R.color.blue to R.style.Blue,
        R.color.blueGrey to R.style.BlueGrey,
        R.color.darkBlue to R.style.DarkBlue,
        R.color.red to R.style.Red,
        R.color.green to R.style.Green,
        R.color.orange to R.style.Orange,
        R.color.purple to R.style.Purple,
        R.color.yellow to R.style.Yellow,
        R.color.caribbeanGreen to R.style.CaribbeanGreen,
        R.color.persianGreen to R.style.PersianGreen,
        R.color.amaranth to R.style.Amaranth,
        R.color.indian_red to R.style.IndianRed,
        R.color.light_coral to R.style.LightCoral,
        R.color.pink_flare to R.style.PinkFlare,
        R.color.makeup_tan to R.style.MakeupTan,
        R.color.egg_yellow to R.style.EggYellow,
        R.color.medium_green to R.style.MediumGreen,
        R.color.olive to R.style.Olive,
        R.color.copperfield to R.style.Copperfield,
        R.color.mineral_green to R.style.MineralGreen,
        R.color.lochinvar to R.style.Lochinvar,
        R.color.beach_grey to R.style.BeachGrey,
    )

    @StyleRes
    fun of(context: Context): Int? {
        if (MainPreferences.isMaterialYouAccentColor() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return R.style.MaterialYou
        val accent = MainPreferences.getAccentColor()
        return themes.firstOrNull { context.getColor(it.first) == accent }?.second
    }
}
