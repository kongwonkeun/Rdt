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
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import com.rdt.one.MyUtil.Companion.showLog
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

        initTextInOut()
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
                showLog(TAG, "----2222----")
                if (resultCode == Activity.RESULT_OK) {
                    showLog(TAG, "----3333----")
                    val address = data?.extras?.getString(BTKey.DEVICE_ADDRESS.s)
                    if (address != null) {
                        appendStatus(address)
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
    // EDIT WATCHER
    //
    private val mEditWatcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            val str = s.toString()
            if (str.isNotEmpty()) {
                mService.sendData(str.toByteArray())
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            //
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
            showToast("x1")
            scanBluetooth()
        }
        x2.setOnClickListener {
            showToast("x2")
            val str = xCmd.text.toString()
            if (str.isNotEmpty()) {
                mService.sendData(str.toByteArray())
            }
        }
        x3.setOnClickListener {
            showToast("x3")
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

    private fun initTextInOut() {
        xStatus.movementMethod = ScrollingMovementMethod()
        xStatus.setHorizontallyScrolling(true)
        // xCmd.addTextChangedListener(mEditWatcher)
    }

    private fun appendStatus(str: String) {
        xStatus.append(str + "\n")
        /*
        xStatus.post {
            val scroll = xStatus.layout.getLineTop(xStatus.lineCount) - xStatus.height
            if (scroll > 0) {
                xStatus.scrollTo(0, scroll)
            }
        }
        */
        /**/
        xStatus.post {
            val scroll = xStatus.layout.getLineBottom(xStatus.lineCount - 1) - xStatus.scrollY - xStatus.height
            if (scroll > 0) {
                xStatus.scrollBy(0, scroll)
            }
        }
        /**/
    }

    //
    // HANDLER
    //
    inner class MainHandler : Handler() {

        override fun handleMessage(msg: Message?) {
            when (msg?.what) {
                BTState.NONE.i -> {
                    appendStatus("initialized")
                }
                BTState.LISTEN.i -> {
                    appendStatus("listen")
                }
                BTState.CONNECT.i -> {
                    appendStatus("connecting")
                }
                BTState.CHAT.i -> {
                    appendStatus("chat")
                }
                BTState.CHAT_DATA.i -> {
                    // appendStatus("chat data")
                    if (msg.obj != null) {
                        val str = msg.obj as String
                        xStatus.append(str)
                    }
                }
            }
            super.handleMessage(msg)
        }

    }

}
