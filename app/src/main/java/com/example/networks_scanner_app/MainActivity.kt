package com.example.networks_scanner_app

import android.annotation.SuppressLint
import android.content.Context
import android.net.*
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.*
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.example.networks_scanner_app.databinding.ActivityMainBinding
import com.google.android.gms.common.util.DataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback


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
                val telephonyManager =
                    getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
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