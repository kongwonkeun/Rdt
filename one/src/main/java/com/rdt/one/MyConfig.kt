package com.rdt.one

import android.content.Context
import java.util.*

class MyConfig {

    companion object {

        val DEBUG = true
        val HOME_PATH = "rdtone"
        val MY_DEVICE_NAME = "RDTONE"
        val SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val DATE_TIME_PATTERN = "yyyy-MM-dd_HH-mm-ss"
        val SERVICE_RESTART_INTERVAL = 60000L
        val TALK_NONE = 0
        val TALK_TALK = 1
        val TALK_PREFIX = "[P]"
        val TALK_SUFFIX = "[S]"
        var myContext: Context? = null
        var myConnectedDeviceName: String? = null
        var myConnectedDeviceAddress: String? = null

        fun getConnectedDeviceInfo() {
            if (myContext != null) {
                val prefs = myContext?.getSharedPreferences(BTKey.DEVICE_INFO.s, Context.MODE_PRIVATE)
                if (prefs != null) {
                    myConnectedDeviceAddress = prefs.getString(BTKey.DEVICE_ADDRESS.s, null)
                    myConnectedDeviceName = prefs.getString(BTKey.DEVICE_NAME.s, null)
                }
            }
        }

        fun setConnectedDeviceInfo() {
            if (myContext != null) {
                val prefs = myContext?.getSharedPreferences(BTKey.DEVICE_INFO.s, Context.MODE_PRIVATE)
                if (prefs != null) {
                    val editor = prefs.edit()
                    editor.putString(BTKey.DEVICE_ADDRESS.s, myConnectedDeviceAddress)
                    editor.putString(BTKey.DEVICE_NAME.s, myConnectedDeviceName)
                    editor.apply()
                }
            }
        }

        fun resetConnectedDeviceInfo() {
            myConnectedDeviceAddress = null
            myConnectedDeviceName = null
        }

    }

}