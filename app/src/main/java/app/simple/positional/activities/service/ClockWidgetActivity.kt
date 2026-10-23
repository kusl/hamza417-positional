package app.simple.positional.activities.service

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.simple.positional.R
import app.simple.positional.extensions.activity.BaseActivity
import app.simple.positional.ui.compose.setPositionalContent
import app.simple.positional.util.PermissionUtils.hasNotificationPermission
import app.simple.positional.util.PermissionUtils.isIgnoringBatteryOptimizations

class ClockWidgetActivity : BaseActivity() {
    private var appWidgetCode = AppWidgetManager.INVALID_APPWIDGET_ID
    private var batteryGranted by mutableStateOf(false)
    private var notificationGranted by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refresh() else Toast.makeText(this, R.string.notification_permission_desc, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetCode = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        refresh()
        if (batteryGranted && notificationGranted) close()
        setPositionalContent {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    Modifier.systemBarsPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Image(painterResource(R.drawable.clock_face), contentDescription = null, modifier = Modifier.size(160.dp))
                    Text(stringResource(R.string.battery_optimization_desc), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = ::onBattery) { Text(stringResource(if (batteryGranted) R.string.button_close else R.string.button_grant)) }
                    Text(stringResource(R.string.notification_permission_desc), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = ::onNotification) { Text(stringResource(if (notificationGranted) R.string.button_close else R.string.button_grant)) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        batteryGranted = isIgnoringBatteryOptimizations()
        notificationGranted = hasNotificationPermission()
    }

    private fun onBattery() {
        when {
            !batteryGranted -> startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            notificationGranted -> close()
            else -> Toast.makeText(this, R.string.notification_permission_desc, Toast.LENGTH_LONG).show()
        }
    }

    private fun onNotification() {
        when {
            !notificationGranted -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            batteryGranted -> close()
            else -> Toast.makeText(this, R.string.battery_optimization_desc, Toast.LENGTH_LONG).show()
        }
    }

    private fun close() {
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetCode))
        finish()
    }
}
