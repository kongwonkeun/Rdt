package com.rdt.one

import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.rdt.one.MyConfig.Companion.myContext
import com.rdt.one.MyConfig.Companion.DATE_TIME_PATTERN
import com.rdt.one.MyConfig.Companion.DEBUG
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MyUtil {

    companion object {

        fun showToast(resId: Int) {
            val toast = Toast.makeText(myContext, resId, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        fun showToast(message: String) {
            val toast = Toast.makeText(myContext, message, Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        fun showLog(tag: String, message: String) {
            if (DEBUG) {
                Log.d(tag, message)
            }
        }

        fun getDateTime(date: Date): String {
            return SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(date)
        }

        fun getTime(msec: Long): String {
            return (
            if (TimeUnit.MICROSECONDS.toHours(msec) % 24 == 0L) {
                if (TimeUnit.MICROSECONDS.toMinutes(msec) % 60 == 0L) {
                    String.format(
                        Locale.getDefault(),
                        "%02ds",
                        TimeUnit.MICROSECONDS.toSeconds(msec) % 60
                    )
                } else {
                    String.format(
                        Locale.getDefault(),
                        "%02dm %02ds",
                        TimeUnit.MICROSECONDS.toMinutes(msec) % 60,
                        TimeUnit.MICROSECONDS.toSeconds(msec) % 60
                    )
                }
            } else {
                String.format(
                    Locale.getDefault(),
                    "%02dh %02dm %02ds",
                    TimeUnit.MICROSECONDS.toHours(msec) % 24,
                    TimeUnit.MICROSECONDS.toMinutes(msec) % 60,
                    TimeUnit.MICROSECONDS.toSeconds(msec) % 60
                )
            })
        }
    }

}