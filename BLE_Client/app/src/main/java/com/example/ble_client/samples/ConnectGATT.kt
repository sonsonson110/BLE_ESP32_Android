@file:Suppress("DEPRECATION")

package com.example.ble_client.samples

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
val CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
val CLIENT_CONFIGURATION_CHARACTERISTIC_UUID: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

@Composable
@SuppressLint("MissingPermission")
fun ConnectGATT() {
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    BluetoothBox {
        AnimatedContent(targetState = selectedDevice, label = "Selected device") { device ->
            if (device == null) {
                BleScanScreen {
                    selectedDevice = it
                }
            } else {
                // Once the device is connected, show the UI and try to connect to the device
                ConnectDeviceScreen(device = device) {
                    selectedDevice = null
                }
            }
        }
    }
}

@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun ConnectDeviceScreen(device: BluetoothDevice, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Keeps track of the last connection state with the device
    var state by remember(device) {
        mutableStateOf<DeviceConnectionState?>(null)
    }

    // This effect will handle the connection and notify when the state changes
    BLEConnectEffect(device = device) {
        // update our state to recompose the UI
        state = it
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Devices details", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Name: ${device.name} (${device.address})",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Status: ${state?.connectionState?.toConnectionStateString()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Services: ${state?.services?.joinToString(separator = "\n") { it.uuid.toString() }}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Message received:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Hex: ${state?.messageReceived?.toHex()}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Float: ${state?.messageReceived?.toFloat()}",
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            enabled = state?.gatt != null,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    // Once we have the connection discover the peripheral services
                    state?.gatt?.discoverServices()
                }
            },
        ) {
            Text(text = "Discover")
        }
        Button(onClick = onClose) {
            Text(text = "Close")
        }
    }
}

@Composable
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun BLEConnectEffect(
    device: BluetoothDevice,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStateChange: (DeviceConnectionState) -> Unit,
) {
    val context = LocalContext.current
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    // Keep the current connection state
    var state by remember {
        mutableStateOf(DeviceConnectionState.None)
    }

    DisposableEffect(lifecycleOwner, device) {
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                state = state.copy(gatt = gatt, connectionState = newState)
                currentOnStateChange(state)

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    // Here you should handle the error returned in status based on the constants
                    // https://developer.android.com/reference/android/bluetooth/BluetoothGatt#summary
                    Log.e("BLEConnectEffect", "An error happened: $status")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                // enable receive notify from specify characteristic
                val tempCharacteristic =
                    gatt.services
                        .find { it.uuid == SERVICE_UUID }
                        ?.getCharacteristic(CHARACTERISTIC_UUID)
                val result = enableNotification(gatt, characteristic = tempCharacteristic)
                Log.d("onServicesDiscovered", "Notification enabled?: $result")
                state = state.copy(services = gatt.services)
                currentOnStateChange(state)
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
            ) {
                super.onCharacteristicChanged(gatt, characteristic)
                state = state.copy(messageReceived = characteristic.value)
                currentOnStateChange(state)
            }

            // https://developer.android.com/develop/connectivity/bluetooth/ble/transfer-ble-data#notification
            @SuppressLint("MissingPermission")
            private fun enableNotification(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
            ): Boolean {
                if (gatt == null || characteristic == null) {
                    Log.e("enableNotification", "gatt: $gatt - characteristic: $characteristic")
                    return false
                }

                // This is specific to our own UUID
                // Get client characteristic configuration from server - 0x2902
                characteristic.getDescriptor(CLIENT_CONFIGURATION_CHARACTERISTIC_UUID)
                    ?.let { descriptor ->
                        if (!gatt.setCharacteristicNotification(characteristic, true)) {
                            Log.e(
                                "enableNotification",
                                "setCharacteristicNotification failed for ${characteristic.uuid}"
                            )
                            return false
                        }
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)  // like what we set in 0x2902 descriptor in server
                        return gatt.writeDescriptor(descriptor)    // write in legacy mode, not support android 13+
                        // From now onCharacteristicChanged is triggered from callback
                    } ?: Log.e(
                    "enableNotification",
                    "${characteristic.uuid} doesn't contain the CCC descriptor!"
                )
                return false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (state.gatt != null) {
                    // If we previously had a GATT connection let's reestablish it
                    state.gatt?.connect()
                } else {
                    // Otherwise create a new GATT connection
                    state = state.copy(gatt = device.connectGatt(context, false, callback))
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Unless you have a reason to keep connected while in the bg you should disconnect
                state.gatt?.disconnect()
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer and close the connection
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            state.gatt?.close()
            state = DeviceConnectionState.None
        }
    }
}

internal fun Int.toConnectionStateString() = when (this) {
    BluetoothProfile.STATE_CONNECTED -> "Connected"
    BluetoothProfile.STATE_CONNECTING -> "Connecting"
    BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
    BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
    else -> "N/A"
}

private data class DeviceConnectionState(
    val gatt: BluetoothGatt?,
    val connectionState: Int,
    val mtu: Int,
    val services: List<BluetoothGattService> = emptyList(),
    val messageSent: Boolean = false,
    val messageReceived: ByteArray? = null,
) {
    companion object {
        val None = DeviceConnectionState(null, -1, -1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeviceConnectionState

        if (gatt != other.gatt) return false
        if (connectionState != other.connectionState) return false
        if (mtu != other.mtu) return false
        if (services != other.services) return false
        if (messageSent != other.messageSent) return false
        if (messageReceived != null) {
            if (other.messageReceived == null) return false
            if (!messageReceived.contentEquals(other.messageReceived)) return false
        } else if (other.messageReceived != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = gatt?.hashCode() ?: 0
        result = 31 * result + connectionState
        result = 31 * result + mtu
        result = 31 * result + services.hashCode()
        result = 31 * result + messageSent.hashCode()
        result = 31 * result + (messageReceived?.contentHashCode() ?: 0)
        return result
    }
}

fun ByteArray.toFloat(): Float {
    require(this.size == 4) { "Byte array must be exactly 4 bytes long" }
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    return buffer.getFloat()
}

fun ByteArray.toHex(): String =
    joinToString(separator = "-") { eachByte -> "%02x".format(eachByte) }