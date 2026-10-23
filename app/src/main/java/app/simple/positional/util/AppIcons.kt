package app.simple.positional.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import app.simple.positional.R

object AppIcons {
    private const val ALIAS_PREFIX = "app.simple.positional.activities.alias."

    val icons = listOf(
        "IconOneAlias" to R.mipmap.ic_launcher_default,
        "IconLegacyAlias" to R.mipmap.ic_launcher_legacy,
        "IconTwoAlias" to R.mipmap.ic_launcher_two,
        "IconThreeAlias" to R.mipmap.ic_launcher_three,
        "IconFourAlias" to R.mipmap.ic_launcher_four,
        "IconFiveAlias" to R.mipmap.ic_launcher_five,
        "IconSixAlias" to R.mipmap.ic_launcher_six,
        "IconSevenAlias" to R.mipmap.ic_launcher_seven,
        "IconEightAlias" to R.mipmap.ic_launcher_eight,
        "IconNineAlias" to R.mipmap.ic_launcher_nine,
        "IconTenAlias" to R.mipmap.ic_launcher_ten,
        "IconElevenAlias" to R.mipmap.ic_launcher_eleven,
    )

    fun componentName(alias: String) = ALIAS_PREFIX + alias

    fun current(context: Context): Int = icons.indexOfFirst { (alias, _) ->
        context.packageManager.getComponentEnabledSetting(ComponentName(context, componentName(alias))) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }.coerceAtLeast(0)

    fun select(context: Context, index: Int) {
        icons.forEachIndexed { i, (alias, _) ->
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, componentName(alias)),
                if (i == index) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
