package com.rdt.one

import android.content.Context

class MyConfig {

    companion object {
        val DEBUG = false
        val HOME_PATH = "rdtone"
        val DATE_TIME_PATTERN = "yyyy-MM-dd_HH-mm-ss"
        val SERVICE_RESTART_INTERVAL = 60000L
        val TALK_NONE = 0
        val TALK_TALK = 1
        val TALK_PREFIX = "talk:"
        val TALK_SUFFIX = "[*]"
        var myContext: Context? = null
        var myConnectedDeviceName: String? = null
        var myConnectedDeviceAddress: String? = null
    }

}