package com.rdt.one

import android.app.Application

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MyConfig.myContext = applicationContext
    }

    override fun onTerminate() {
        super.onTerminate()
    }

}