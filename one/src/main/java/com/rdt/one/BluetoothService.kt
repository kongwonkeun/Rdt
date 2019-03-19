package com.rdt.one

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import com.rdt.one.BluetoothServiceMonitoring.Companion.startMonitor
import com.rdt.one.MyConfig.Companion.TALK_NONE
import com.rdt.one.MyConfig.Companion.TALK_PREFIX
import com.rdt.one.MyConfig.Companion.TALK_SUFFIX
import com.rdt.one.MyConfig.Companion.TALK_TALK
import com.rdt.one.MyConfig.Companion.myConnectedDeviceAddress
import com.rdt.one.MyConfig.Companion.myConnectedDeviceName
import com.rdt.one.MyUtil.Companion.showToast
import java.lang.StringBuilder

class BluetoothService : Service() {

    private val TAG = "BluetoothService"
    private val myBinder: IBinder = MyBinder()
    private val mBTHandler = BTHandler()
    private lateinit var mMainHandler: Handler
    private lateinit var mParser: MessageParser
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var mBluetoothManager: BluetoothManager? = null

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, BluetoothService::class.java)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startMonitor(applicationContext)
        mParser = MessageParser()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            // need to turn on. activity will do this.
        } else {
            setupBluetooth()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return myBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onDestroy() {
        kill()
        super.onDestroy()
    }

    //
    // PUBLIC FUN
    //
    fun setup(handler: Handler) {
        mMainHandler = handler
        setupBluetooth()
        if (myConnectedDeviceAddress != null && myConnectedDeviceName != null) {
            connectDevice(myConnectedDeviceAddress!!)
        } else {
            if (mBluetoothManager!!.getState() == BTState.NONE) {
                mBluetoothManager!!.accept() // wait in listening mode
            }
        }
    }

    fun kill() {
        if (mBluetoothManager != null) {
            mBluetoothManager!!.stop()
        }
    }

    fun setupBluetooth() {
        if (mBluetoothManager == null) {
            mBluetoothManager = BluetoothManager(mBTHandler)
        }
    }

    fun connectDevice(address: String) {
        val device = mBluetoothAdapter.getRemoteDevice(address)
        if (device != null && mBluetoothManager != null) {
            mBluetoothManager!!.connect(device)
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        if (mBluetoothManager != null) {
            mBluetoothManager!!.connect(device)
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return mBluetoothAdapter.isEnabled
    }

    fun getBluetoothScanMode(): Int {
        return mBluetoothAdapter.scanMode
    }

    //
    // HANDLER FOR THE MESSAGE FROM BLUETOOTH MANAGER
    //
    inner class BTHandler : Handler() {

        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                BTMessage.STATE_CHANGE.i -> {
                    when (msg.arg1) {
                        BTState.NONE.i -> mMainHandler.obtainMessage(BTState.NONE.i).sendToTarget()
                        BTState.LISTEN.i -> mMainHandler.obtainMessage(BTState.LISTEN.i).sendToTarget()
                        BTState.CONNECT.i -> mMainHandler.obtainMessage(BTState.CONNECT.i).sendToTarget()
                        BTState.CHAT.i -> mMainHandler.obtainMessage(BTState.CHAT.i).sendToTarget()
                    }
                }
                BTMessage.WRITE.i -> {
                    showToast("data sent")
                }
                BTMessage.READ.i -> {
                    val buf = msg.obj as ByteArray
                    val cnt = msg.arg1
                    if (cnt > 0) {
                        val str = String(buf, 0, cnt)
                        mMainHandler.obtainMessage(BTState.CHAT_DATA.i, str).sendToTarget() // to do something in ui activity
                        val talk = mParser.addTalkMessage(str)
                        if (talk == TALK_TALK) {
                            // val talkInfo = mParser.getTalkInfo()
                            // do something with talk info string
                            mParser.reset()
                        }
                    }
                }
                BTMessage.DEVICE_NAME.i -> {
                    val address = msg.data.getString(BTKey.DEVICE_ADDRESS.s,null)
                    val name = msg.data.getString(BTKey.DEVICE_NAME.s,null)
                    if (address != null && name !== null) {
                        myConnectedDeviceAddress = address
                        myConnectedDeviceName = name
                        showToast(String.format("connected to %s", name))
                    }
                }
                BTMessage.TOAST.i -> {
                    showToast(msg.data.getString(BTKey.TOAST.s,"toast error"))
                }
                BTMessage.NONE.i -> {
                    //
                }
            }
            super.handleMessage(msg)
        }

    }

    //
    // INNER CLASS
    //
    inner class MessageParser {

        private val TALK_BUF_MAX = 1000
        private val TALK_BUF_MIN = 200
        private var uStrBuilder = StringBuilder()
        private var uTalk = TALK_NONE
        private var uTalkInfo: String? = null

        fun addTalkMessage(message: String): Int {
            uStrBuilder.append(message)
            return parse()
        }

        fun getTalk(): Int {
            return uTalk
        }

        fun getTalkInfo(): String? {
            return uTalkInfo
        }

        fun reset() {
            uTalk = TALK_NONE
            uTalkInfo = null
        }

        private fun parse(): Int {
            uTalk = TALK_NONE
            val prefix = uStrBuilder.lastIndexOf(TALK_PREFIX)
            if (prefix > -1) {
                val surfix = uStrBuilder.lastIndexOf(TALK_SUFFIX)
                if (surfix > -1) {
                    if (prefix + TALK_PREFIX.length <= surfix) {
                        uTalk = TALK_TALK
                        uTalkInfo = uStrBuilder.substring(prefix + TALK_PREFIX.length, surfix)
                        uStrBuilder = StringBuilder()
                        return uTalk
                    }
                }
            }
            if (uStrBuilder.length > TALK_BUF_MAX) {
                uStrBuilder = StringBuilder(uStrBuilder.substring(uStrBuilder.length - TALK_BUF_MIN))
            }
            return uTalk
        }

    }

    //
    // INNER CLASS
    //
    inner class MyBinder : Binder() {

        fun getService(): BluetoothService {
            return this@BluetoothService
        }

    }

}
