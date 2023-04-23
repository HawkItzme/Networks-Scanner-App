package com.example.networks_scanner_app

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.networks_scanner_app.databinding.ActivityMainBinding
import com.google.android.gms.common.util.DataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    //Bluetooth
    private lateinit var bluetoothAdapter: BluetoothAdapter
    var bluetoothGatt: BluetoothGatt? = null
    companion object {
        const val REQUEST_ENABLE_BT = 1
    }
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                binding.button.setOnClickListener {
                    GlobalScope.launch {
                        updateNetworkInfo()
                    }
                }
                super.onAvailable(network)
            }

            override fun onLost(network: Network) {
                    GlobalScope.launch {
                        clearNetworkInfo()
                }
                super.onLost(network)
            }
        }

        //Bluetooth
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            // Ask the user to turn on Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            // Bluetooth is already enabled, do whatever you need to do with it
            Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show()
        }
        binding.button2.setOnClickListener {
            Log.d("Taggy", "Button working")
                scanConnectedBluetoothDevices(this)
        }
    }

    //Bluetooth
    // In your activity or fragment, handle the result of the permission request
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth was successfully enabled by the user
                Toast.makeText(this, "Bluetooth is now enabled", Toast.LENGTH_SHORT).show()
            } else {
                // User denied the Bluetooth permission request, handle this case
                Toast.makeText(this, "Bluetooth permission was denied", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("MissingPermission")
    private fun scanConnectedBluetoothDevices(context: Context) {
        val connectedDevices = bluetoothAdapter.bondedDevices
        if (connectedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in connectedDevices) {
                val deviceName = device.name
                val deviceMacAddress = device.address
                Log.d("Taggy", "Button working2")
                getDeviceSignalStrength(context, device, deviceName, deviceMacAddress)
                Log.d("Taggy", "Button working3")
               /*withContext(Dispatchers.Main) {
                    binding.textView2.append("$deviceName\nMAC Address: $deviceMacAddress\nSignal Strength: $deviceSignalStrength\n\n")
                }*/
            }
        } else {
                binding.textView2.text = "No connected Bluetooth devices found."
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceSignalStrength(context: Context, device: BluetoothDevice, deviceName: String, deviceMacAddress: String ): Int {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothGattCallback = object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {


                // If the connection to the GATT server is successful, discover services
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt?.discoverServices()
                }
                super.onConnectionStateChange(gatt, status, newState)
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int ) {
                // Get the service and characteristic UUIDs
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        val services = gatt?.services
                        services?.let {
                            for (service in services) {
                                val serviceUUID = service.uuid
                                val characteristics = service.characteristics
                                for (characteristic in characteristics) {
                                    val characteristicUUID = characteristic.uuid
                                    // Do something with the serviceUUID and characteristicUUID
                                  //  val serviceUuid = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
                                  //  val characteristicUuid = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
                                    // Get the service and characteristic objects
                                    val service = gatt?.getService(serviceUUID)
                                    val characteristic = service?.getCharacteristic(characteristicUUID)

                                    // Read the characteristic value
                                    gatt?.readCharacteristic(characteristic)
                                }
                            }
                        }
                    }
                    else -> {
                        Log.w("TAGGY", "onServicesDiscovered received status $status")
                    }
                }
                super.onServicesDiscovered(gatt, status)
            }

            @SuppressLint("MissingPermission")
            override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
                // Get the signal strength value
                val signalStrength = characteristic?.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0)

                // Disconnect the GATT client
                gatt?.disconnect()



                // Display the signal strength value
                runOnUiThread {
                    binding.textView2.text = "$deviceName\nMAC Address: $deviceMacAddress\nSignal Strength: $signalStrength\n\n"
                }
                super.onCharacteristicRead(gatt, characteristic, status)
            }
        }
        bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback)
        return 0
    }

    @SuppressLint("MissingPermission")
    private suspend fun updateNetworkInfo() {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        if (networkCapabilities != null) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager =
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiManager.connectionInfo
                withContext(Dispatchers.Main) {
                    binding.textView.text = "WIFI STRENGTH:- ${wifiInfo.rssi.toString()} \nMAC :- ${wifiInfo.macAddress}"
                }
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val allCellInfo = telephonyManager.allCellInfo
                if (allCellInfo.isNotEmpty()) {
                    val signalInfo = allCellInfo[0]
                    if (signalInfo is CellInfoLte) {
                        val signalStrength = signalInfo.cellSignalStrength.dbm
                        //val snr = signalInfo.cellSignalStrength.dbm - signalInfo.cellSignalStrength.cqi
                        val networkType = "LTE"
                        // Display network info in UI
                        withContext(Dispatchers.Main) {
                            binding.textView.text =
                                "MobNet STRENGTH:- ${signalStrength} dBm \nNetwork Type :- ${networkType}"
                        }
                    } else if (signalInfo is CellInfoWcdma) {
                        val signalStrength = signalInfo.cellSignalStrength.dbm
                        //val snr = signalInfo.cellSignalStrength.dbm - signalInfo.cellSignalStrength.r
                        val networkType = "3G"
                        // Display network info in UI
                        withContext(Dispatchers.Main) {
                            binding.textView.text =
                                "MobNet STRENGTH:- ${signalStrength} dBm  \nNetwork Type :- ${networkType}"
                        }
                    }
                }
            }
        }
    }

    private suspend fun clearNetworkInfo() {
        withContext(Dispatchers.Main) {
            binding.textView.text = "NO SIGNAL"
        }
    }

    override fun onResume() {

        GlobalScope.launch {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            withContext(Dispatchers.Main) {
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            }
        }
        super.onResume()
    }
}