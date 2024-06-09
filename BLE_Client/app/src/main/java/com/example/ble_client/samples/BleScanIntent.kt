package com.example.ble_client.samples

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.ble_client.base.PermissionBox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/*
    The scan will continue even when the screen is no longer visible, or the app is killed by OS.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
fun BleScanIntent() {
    // Request required permissions from user
    val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
    }
    PermissionBox(permissions = permissions) {
        // All necessary permissions granted
        BleScanIntentScreen()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
internal fun BleScanIntentScreen() {
    val context = LocalContext.current
    val scanner = context.getSystemService<BluetoothManager>()?.adapter?.bluetoothLeScanner
    if (scanner != null) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            ScanPendingItem(scanner)
        }
    } else {
        Text(text = "Bluetooth Scanner not found")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
internal fun ScanPendingItem(scanner: BluetoothLeScanner) {
    val context = LocalContext.current
    var pendingIntent by remember {
        mutableStateOf(
            PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, BleScanReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_MUTABLE
            )
        )
    }
    // If the PendingIntent is null, it means it's not registered
    val isScheduled = pendingIntent != null

    val devices by BleScanReceiver.devices.collectAsState()
    LazyColumn {
        stickyHeader {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Scan even if app is not alive", modifier = Modifier.weight(0.5f))
                Button(
                    onClick = {
                        pendingIntent = if (isScheduled) {
                            pendingIntent.cancel()
                            null
                        } else {
                            startScan(context, scanner)
                        }
                    },
                    modifier = Modifier.weight(0.5f)
                ) {
                    if (isScheduled) {
                        Text(text = "Stop scanning")
                    } else {
                        Text(text = "Schedule Scan")
                    }
                }
            }
        }
        items(devices) {
//            BluetoothDeviceItem(bluetoothDevice = it.device, isSampleServer = true, onConnect = {})
            Text(it.device.toString()) // TODO: replace this with a defined composable
            Divider(Modifier.padding(vertical = 8.dp))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
internal fun startScan(context: Context, scanner: BluetoothLeScanner): PendingIntent? {
    val scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setReportDelay(3000)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    val resultIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, BleScanReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    // We only want the our ESP32 server
    val scanFilter = emptyList<ScanFilter>()
    scanner.startScan(scanFilter, scanSettings, resultIntent)
    return resultIntent
}

class BleScanReceiver : BroadcastReceiver() {
    companion object {
        // Static StateFlow that caches the list of scanned devices used by our sample
        // This is an **anti-pattern** used for demo purpose and simplicity
        val devices = MutableStateFlow(emptyList<ScanResult>())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val results = intent.getScanResults()
        Log.d("MPB", "Devices found: ${results.size}")

        // Update our results cached list
        if (results.isNotEmpty()) {
            devices.update { scanResults ->
                (scanResults + results).distinctBy { it.device.address }
            }
        }
    }

    /**
     * Extract the list of scan result from the intent if available
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun Intent.getScanResults(): List<ScanResult> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                ScanResult::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
        } ?: emptyList()
}