package com.example.networks_scanner_app

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.*
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.telephony.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.networks_scanner_app.databinding.ActivityMainBinding
import com.google.android.gms.common.util.DataUtils
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : AppCompatActivity() {

    //Firebase
    private lateinit var database: DatabaseReference

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    //Bluetooth
    private lateinit var bluetoothAdapter: BluetoothAdapter
    var bluetoothGatt: BluetoothGatt? = null

    //GPS
    private lateinit var locationManager: LocationManager
    companion object {
        const val REQUEST_ENABLE_BT = 1
        //GPS
        const val REQUEST_LOCATION_PERMISSION = 1
        const val MIN_TIME_BETWEEN_UPDATES: Long = 5000 // milliseconds
        const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10.0f // meters
    }

    //TODO: Stop using Globalscope.launch(), it is bad practice use lifecycleScope.launch() instead
    @SuppressLint("MissingPermission", "SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        //Firebase
        database = Firebase.database.reference

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                binding.button.setOnClickListener {
                    lifecycleScope.launch{
                        updateNetworkInfo()
                    }
                }
                super.onAvailable(network)
            }

            override fun onLost(network: Network) {
                lifecycleScope.launch{
                    clearNetworkInfo()
                }
                super.onLost(network)
            }
        }

        //Bluetooth
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_ENABLE_BT)
        } else {
            // Permission is already granted, proceed with Bluetooth operations
            // ...
            Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show()
        }
        binding.button2.setOnClickListener {
                scanConnectedBluetoothDevices(this)
        }

        //GPS
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Check for location permission
        // Check if the app has location permission and turn on location if permission is granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            turnOnLocation()
        }
        binding.button3.setOnClickListener {
            Toast.makeText(this, "This may take some time..!", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.VISIBLE
                startLocationUpdates()
        }
    }
    //GPS
    private fun turnOnLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            // Location is already turned on
            Toast.makeText(this, "Location is now enabled", Toast.LENGTH_SHORT).show()
        }
    }
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        //TODO: Always use try and catch to listeners that have exceptions that might likely crash the app in runtime.
        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener)
        }catch (e: Exception){
            /*  IllegalArgumentException – if listener is null
                RuntimeException – if the calling thread has no Looper
                SecurityException – if no suitable permission is present
             */
            e.printStackTrace()
        }

    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val latitude = location.latitude
            val longitude = location.longitude
            val accuracy = location.accuracy
            binding.progressBar.visibility = View.GONE
            binding.textView3.text = "Latitude: $latitude\nLongitude: $longitude\nAccuracy: $accuracy meters"
        }

        override fun onProviderEnabled(provider: String) {
            // Do nothing
        }

        override fun onProviderDisabled(provider: String) {
            // Do nothing
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            // Do nothing
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                // If request is cancelled, the grantResults array is empty
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted, proceed with Bluetooth operations
                    // ...
                    Toast.makeText(this, "Bluetooth is now enabled", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission is denied, disable Bluetooth functionality or show a message
                    // ...
                    Toast.makeText(this, "Bluetooth is denied.", Toast.LENGTH_SHORT).show()
                }
                return
            }
            REQUEST_LOCATION_PERMISSION -> {
                if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                }else{
                    Toast.makeText(this, "Location is denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //Bluetooth
    // In your activity or fragment, handle the result of the permission request

    @SuppressLint("MissingPermission")
    private fun scanConnectedBluetoothDevices(context: Context) {
        val connectedDevices = bluetoothAdapter.bondedDevices
        if (connectedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in connectedDevices) {
                val deviceName = device.name
                val deviceMacAddress = device.address
                getDeviceSignalStrength(context, device, deviceName, deviceMacAddress)
               /*withContext(Dispatchers.Main) {
                    binding.textView2.append("$deviceName\nMAC Address: $deviceMacAddress\nSignal Strength: $deviceSignalStrength\n\n")
                }*/
            }
        } else {
                binding.textView2.text = "CONNECT BLUETOOTH"
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
                lifecycleScope.launch {
                    binding.textView2.text = "$deviceName\nMAC Address: $deviceMacAddress\nSignal Strength: $signalStrength\n\n"
                }
                super.onCharacteristicRead(gatt, characteristic, status)
            }
        }
        bluetoothGatt = device.connectGatt(context, false, bluetoothGattCallback)
        return 0
    }

    @SuppressLint("MissingPermission")
    private fun updateNetworkInfo() {
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        if (networkCapabilities != null) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val wifiManager =
                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiManager.connectionInfo
                    binding.textView.text = "WIFI STRENGTH:- ${wifiInfo.rssi.toString()} \nMAC :- ${wifiInfo.macAddress}"
                val signalData = SignalData(wifiInfo.rssi.toString(), wifiInfo.macAddress)
                database.child("signalData").setValue(signalData)
                    .addOnSuccessListener {
                        Log.d(TAG, "First child write succeeded")
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "First child write failed: $it")
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
                        lifecycleScope.launch {
                            binding.textView.text =
                                "MobNet STRENGTH:- ${signalStrength} dBm \nNetwork Type :- ${networkType}"
                            val signalData = SignalData(signalStrength.toString(), networkType)
                            database.child("signalData").setValue(signalData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "First child write succeeded")
                                }
                                .addOnFailureListener {
                                    Log.e(TAG, "First child write failed: $it")
                                }
                        }
                    } else if (signalInfo is CellInfoWcdma) {
                        val signalStrength = signalInfo.cellSignalStrength.dbm
                        //val snr = signalInfo.cellSignalStrength.dbm - signalInfo.cellSignalStrength.r
                        val networkType = "3G"
                        // Display network info in UI
                        lifecycleScope.launch{
                            binding.textView.text =
                                "MobNet STRENGTH:- ${signalStrength} dBm  \nNetwork Type :- ${networkType}"
                            val signalData = SignalData(signalStrength.toString(), networkType)
                            database.child("signalData").setValue(signalData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "First child write succeeded")
                                }
                                .addOnFailureListener {
                                    Log.e(TAG, "First child write failed: $it")
                                }
                        }
                    }
                }
            }
        }
    }

    private fun clearNetworkInfo() {
        lifecycleScope.launch {
            binding.textView.text = "NO SIGNAL"
        }

    }

    @SuppressLint("SuspiciousIndentation")
    override fun onResume() {
       lifecycleScope.launch {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        }
        super.onResume()
    }
}