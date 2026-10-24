package app.simple.positional.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    fun checkPermission(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_COARSE_LOCATION) || hasFineLocation(context)

    fun hasFineLocation(context: Context): Boolean = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
