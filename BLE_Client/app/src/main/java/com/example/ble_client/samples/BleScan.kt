package com.example.ble_client.samples

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

/*
    This scan is kill when screen is no longer visible
 */
private const val TAG = "BleScan.kt"

@SuppressLint("MissingPermission")
@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
fun BleScan() {
    BluetoothBox {
        BleScanScreen {
            Log.d("FindDeviceSample", "Name: ${it.name} Address: ${it.address} Type: ${it.type}")
        }
    }
}

@Composable
@SuppressLint("InlinedApi", "MissingPermission")
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
fun BleScanScreen(onConnect: (BluetoothDevice) -> Unit) {
    val context = LocalContext.current
    val adapter = checkNotNull(context.getSystemService<BluetoothManager>()?.adapter)
    var scanning by remember { mutableStateOf(true) }
    val devices = remember { mutableStateListOf<BluetoothDevice>() }
    val pairedDevices =
        remember { mutableStateListOf<BluetoothDevice>(*adapter.bondedDevices.toTypedArray()) }
    val scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    if (scanning) {
        BluetoothScanEffect(
            scanSettings = scanSettings,
            onScanFailed = {
                scanning = false
                Log.w(TAG, "Scan failed with error: $it")
            },
            onDeviceFound = { scanResult ->
                if (!devices.contains(scanResult.device)) {
                    devices.add(scanResult.device)
                }
            }
        )

        // Stop scanning after a while
        LaunchedEffect(true) {
            delay(15000)
            scanning = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Available devices", style = MaterialTheme.typography.titleSmall)
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = {
                        devices.clear()
                        scanning = true
                    }
                ) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (devices.isEmpty()) {
                item {
                    Text("No devices found", style = MaterialTheme.typography.bodyLarge)
                }
            }
            items(devices) { item ->
                BluetoothDeviceItem(item, onConnect)
            }
            item {
                Divider(Modifier.padding(vertical = 8.dp))
            }
            if (pairedDevices.isNotEmpty()) {
                item {
                    Text(text = "Saved devices", style = MaterialTheme.typography.titleSmall)
                }
                items(pairedDevices) {
                    BluetoothDeviceItem(
                        bluetoothDevice = it,
                        onConnect = onConnect,
                    )
                }
            }
        }
    }
}

@Composable
@SuppressLint("MissingPermission")
fun BluetoothDeviceItem(bluetoothDevice: BluetoothDevice, onConnect: (BluetoothDevice) -> Unit) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
            .clickable { onConnect(bluetoothDevice) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            bluetoothDevice.name ?: "N/A",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            bluetoothDevice.address,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.5f)
        )
        val state = when (bluetoothDevice.bondState) {
            BluetoothDevice.BOND_BONDED -> "Paired"
            BluetoothDevice.BOND_BONDING -> "Pairing"
            else -> "None"
        }
        Text(
            text = state,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.2f)
        )
    }
}

@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
internal fun BluetoothScanEffect(
    scanSettings: ScanSettings,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onScanFailed: (Int) -> Unit,
    onDeviceFound: (devices: ScanResult) -> Unit,
) {
    val context = LocalContext.current
    val adapter = context.getSystemService<BluetoothManager>()?.adapter

    if (adapter == null) {
        onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        return
    }

    val currentOnDeviceFound by rememberUpdatedState(onDeviceFound)

    DisposableEffect(lifecycleOwner, scanSettings) {
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                currentOnDeviceFound(result)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                onScanFailed(errorCode)
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            // Start scanning once the app is in foreground and stop when in background
            if (event == Lifecycle.Event.ON_START) {
                adapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            } else if (event == Lifecycle.Event.ON_STOP) {
                adapter.bluetoothLeScanner.stopScan(leScanCallback)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and stop scanning
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adapter.bluetoothLeScanner.stopScan(leScanCallback)
        }
    }
}