package com.rdt.one

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import com.rdt.one.MyUtil.Companion.showLog
import kotlinx.android.synthetic.main.activity_connection.*

class ConnectionActivity : PermissionActivity() {

    private val TAG = "ConnectionActivity"
    private lateinit var mPairedDeviceAdapter: ArrayAdapter<String>
    private lateinit var mNewDeviceAdapter: ArrayAdapter<String>
    private lateinit var mAdapter: BluetoothAdapter

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ConnectionActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        setResult(Activity.RESULT_CANCELED)

        mPairedDeviceAdapter = ArrayAdapter(this, R.layout.adapter_device)
        xPairedList.adapter = mPairedDeviceAdapter
        xPairedList.setOnItemClickListener(mItemClickListener)
        mNewDeviceAdapter = ArrayAdapter(this, R.layout.adapter_device)
        xNewList.adapter = mNewDeviceAdapter
        xNewList.setOnItemClickListener(mItemClickListener)

        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        mAdapter = BluetoothAdapter.getDefaultAdapter()

        val pairedDevices = mAdapter.bondedDevices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                mPairedDeviceAdapter.add(device.name + " " + device.address)
            }
        } else {
            mPairedDeviceAdapter.add("no device")
        }
        setupControlInterface()
    }

    override fun onDestroy() {
        mAdapter.cancelDiscovery()
        this.unregisterReceiver(mReceiver)
        super.onDestroy()
    }

    //
    //
    //
    private fun setupControlInterface() {
        xBack.setOnClickListener {
            finish()
        }
        xScan.setOnClickListener {
            mNewDeviceAdapter.clear()
            discovery()
        }
    }

    private fun discovery() {
        if (mAdapter.isDiscovering) {
            mAdapter.cancelDiscovery()
        }
        mAdapter.startDiscovery()
    }

    //
    // LISTENER FOR CLICK
    //
    /*
    private val mItemClickListener = object : AdapterView.OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            mAdapter.cancelDiscovery()
            val info = (view as TextView).text.toString()
            if (info.length > 16) {
                val address = info.substring(info.length - 17) // address is last 17 chars
                val intent = Intent()
                intent.putExtra(EXTRA_DEVICE, address)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }
    convert to lambda expression */
    private val mItemClickListener = {
        _: AdapterView<*>?, view: View?, _: Int, _: Long ->
        mAdapter.cancelDiscovery()
        val info = (view as TextView).text.toString()
        if (info.length > 16)
        {
            showLog(TAG, "----1111----")
            val address = info.substring(info.length - 17)
            val intent = Intent()
            intent.putExtra(BTKey.DEVICE_ADDRESS.s, address)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    //
    // RECEIVER FOR BLUETOOTH ACTION
    //
    private val mReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        mNewDeviceAdapter.add(device.name + " " + device.address)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (mNewDeviceAdapter.count == 0) {
                        mNewDeviceAdapter.add("no devices")
                    }
                }
                else -> {
                    // do nothing
                }
            }
        }

    }


}