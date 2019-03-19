package com.rdt.one

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.rdt.one.MyConfig.Companion.SERVICE_RESTART_INTERVAL

class BluetoothServiceMonitoring {

    private val TAG = "BluetoothServiceMonitoring"

    companion object {

        private fun isServiceAlive(context: Context, clazz: Class<*>): Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
            for (service in runningServices) {
                if (clazz.name == service.service.className) {
                    return true
                }
            }
            return false
        }

        fun startMonitor(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BTServiceMonReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), SERVICE_RESTART_INTERVAL, pendingIntent)
        }

        fun stopMoitor(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, BTServiceMonReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.cancel(pendingIntent)
        }

    }

    class BTServiceMonReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null) {
                if (!isServiceAlive(context, BluetoothService::class.java)) {
                    context.startService(BluetoothService.newIntent(context))
                }
            }
        }

    }

}