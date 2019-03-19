package com.rdt.one

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import com.rdt.one.MyUtil.Companion.showToast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : PermissionActivity() {

    private val TAG = "MainActivity"
    private lateinit var mMainHandler: MainHandler
    private lateinit var mService: BluetoothService

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mMainHandler = MainHandler()
        setupService()
        setupControlInterface()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        killService()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.BLUETOOTH_ENABLE.i -> {
                if (resultCode == Activity.RESULT_OK) {
                    mService.setupBluetooth()
                }
            }
            RequestCode.CONNECT_DEVICE.i -> {
                if (resultCode == Activity.RESULT_OK) {
                    val address = data?.extras?.getString(BTKey.DEVICE_ADDRESS.s)
                    if (address != null) {
                        mService.connectDevice(address)
                    }
                }
            }
        }
    }

    //
    // SERVICE CONNECTION
    //
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            mService = (binder as BluetoothService.MyBinder).getService()
            mService.setup(mMainHandler)
            if (!mService.isBluetoothEnabled()) {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), RequestCode.BLUETOOTH_ENABLE.i)
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // do nothing
        }

    }

    //
    // PRIVATE FUN
    //
    private fun setupControlInterface() {
        xBack.setOnClickListener {
            finish()
        }
        x1.setOnClickListener {
            showToast("x1 scan")
            scanBluetooth()
        }
        x2.setOnClickListener {
            showToast("x2 make discoverable")
            makeBluetoothDiscoverable()
        }
        x3.setOnClickListener {
            showToast("x3 pressed")
        }
    }

    private fun setupService() {
        startService(BluetoothService.newIntent(this))
        bindService(BluetoothService.newIntent(this), mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun killService() {
        mService.kill()
        stopService(BluetoothService.newIntent(this))
    }

    private fun scanBluetooth() {
        startActivityForResult(ConnectionActivity.newIntent(this), RequestCode.CONNECT_DEVICE.i)
    }

    private fun makeBluetoothDiscoverable() {
        if (mService.getBluetoothScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(intent)
        }
    }

    //
    // HANDLER
    //
    inner class MainHandler : Handler() {

        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                BTState.NONE.i -> {
                    xStatus.append("initialized")
                }
                BTState.LISTEN.i -> {
                    xStatus.append("listen")
                }
                BTState.CONNECT.i -> {
                    xStatus.append("connecting")
                }
                BTState.CHAT.i -> {
                    xStatus.append("chat")
                }
                BTState.CHAT_DATA.i -> {
                    xStatus.append("chat data")
                    if (msg.obj != null) {
                        xStatus.append(msg.obj as String)
                    }
                }
            }
            super.handleMessage(msg)
        }

    }

}
