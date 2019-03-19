package com.rdt.one

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import com.rdt.one.MyConfig.Companion.myConnectedDeviceAddress
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothManager(private val mBTHandler: android.os.Handler) {

    private val TAG = "BluetoothManager"
    private val SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val MY_DEVICE_NAME = "RDTONE"
    private val mAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mState = BTState.NONE
    private var mStopped = false

    private var mAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mChatThread: ChatThread? = null

    @Synchronized fun getState(): BTState {
        return mState
    }

    @Synchronized fun setState(state: BTState) {
        mState = state
        if (mState == BTState.CHAT) {
            cancelReconnect()
        }
        mBTHandler.obtainMessage(BTMessage.STATE_CHANGE.i, -1).sendToTarget()
    }

    @Synchronized fun accept() {
        reset()
        mAcceptThread = AcceptThread()
        mAcceptThread!!.start()
        setState(BTState.LISTEN)
        mStopped = false
    }

    @Synchronized fun connect(device: BluetoothDevice) {
        if (mState == BTState.CHAT) {
            return
        }
        reset()
        mConnectThread = ConnectThread(device)
        mConnectThread!!.start()
        setState(BTState.CONNECT)
    }

    @Synchronized fun chat(socket: BluetoothSocket, device: BluetoothDevice) {
        reset()
        mChatThread = ChatThread(socket)
        mChatThread!!.start()
        val message = mBTHandler.obtainMessage(BTMessage.DEVICE_NAME.i)
        val bundle = Bundle()
        bundle.putString(BTKey.DEVICE_ADDRESS.s, device.address)
        bundle.putString(BTKey.DEVICE_NAME.s, device.name)
        message.data = bundle
        mBTHandler.sendMessage(message)
        setState(BTState.CHAT)
    }

    @Synchronized fun stop() {
        reset()
        setState(BTState.NONE)
        mStopped = true
        cancelReconnect()
    }

    fun send(buf: ByteArray) {
        synchronized(this) {
            if (mState != BTState.CHAT) {
                return
            }
        }
        if (mChatThread != null) {
            mChatThread!!.send(buf)
        }
    }

    //
    // PRIVATE FUN
    //
    private val RECONNECT_DELAY_MAX = 3600000L
    private var mDelay = 15000L
    private var mTimer: Timer? = null

    private fun reconnect() {
        if (mStopped) {
            return
        }
        mDelay *= 2
        if (mDelay > RECONNECT_DELAY_MAX) {
            mDelay = RECONNECT_DELAY_MAX
        }
        if (mTimer != null) {
            mTimer!!.cancel()
        }
        mTimer = Timer()
        mTimer!!.schedule(ReconnectTask(), mDelay)
    }

    private fun cancelReconnect() {
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer!!.purge()
        }
        mTimer = null
        mDelay = 15000L
    }

    private fun reset() {
        if (mAcceptThread != null) {
            mAcceptThread!!.close()
            mAcceptThread = null
        }
        if (mConnectThread != null) {
            mConnectThread!!.close()
            mConnectThread = null
        }
        if (mChatThread != null) {
            mChatThread!!.close()
            mChatThread = null
        }
    }

    //
    // INNER CLASS
    //
    inner class AcceptThread : Thread() {

        private val TAG = "AcceptThread"
        private lateinit var uSeverSocket: BluetoothServerSocket
        private lateinit var uSocket: BluetoothSocket
        init {
            try {
                uSeverSocket = mAdapter.listenUsingRfcommWithServiceRecord(MY_DEVICE_NAME, SERIAL_PORT_SERVICE_CLASS_UUID)
            } catch (e: IOException) {
                Log.e(TAG,"server socket listening failed")
            }
        }

        override fun run() {
            name = "AcceptThread"
            while (mState != BTState.CHAT) {
                try {
                    uSocket = uSeverSocket.accept()
                } catch (e: IOException) {
                    Log.e(TAG,"server socket accept failed")
                    break
                }
                synchronized(this@BluetoothManager) {
                    when (mState) {
                        BTState.LISTEN, BTState.CONNECT -> chat(uSocket, uSocket.remoteDevice)
                        BTState.NONE, BTState.CHAT -> {
                            try {
                                uSocket.close()
                            } catch (e: IOException) {
                                Log.e(TAG,"could not close unwanted socket")
                            }
                        }
                        else -> {
                            // do nothing
                        }
                    }
                }
            }
        }

        fun close() {
            try {
                uSeverSocket.close()
            } catch (e: IOException) {
                Log.e(TAG,"server socket close failed")
            }
        }
    }


    //
    // INNER CLASS
    //
    inner class ConnectThread(private val uDevice: BluetoothDevice) : Thread() {

        private val TAG = "ConnectThread"
        private lateinit var uSocket: BluetoothSocket
        init {
            try {
                uSocket = uDevice.createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID)
            } catch (e: IOException) {
                Log.e(TAG,"socket creation failed")
            }
        }

        override fun run() {
            name = "ConnectThread"
            mAdapter.cancelDiscovery()
            try {
                uSocket.connect()
            } catch (e: IOException) {
                Log.e(TAG,"socket connection failed")
                setState(BTState.LISTEN)
                reconnect()
                try {
                    uSocket.close()
                } catch (e: IOException) {
                    Log.e(TAG,"unable to close socket")
                }
                this@BluetoothManager.accept()
                return
            }
            synchronized(this@BluetoothManager) {
                mConnectThread = null
            }
            chat(uSocket, uDevice)
        }

        fun close() {
            try {
                uSocket.close()
            } catch (e: IOException) {
                Log.e(TAG,"socket close failed")
            }
        }

    }

    //
    // INNER CLASS
    //
    inner class ChatThread(private val uSocket: BluetoothSocket) : Thread() {

        private val TAG = "ChatThread"
        private lateinit var uIn: InputStream
        private lateinit var uOut: OutputStream
        init {
            try {
                uIn = uSocket.inputStream
                uOut = uSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG,"io stream not created")
            }
        }

        override fun run() {
            while (true) {
                try {
                    val buf = ByteArray(128) { 0 }
                    val cnt = uIn.read(buf)
                    mBTHandler.obtainMessage(BTMessage.READ.i, cnt, -1, buf).sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG,"disconnected")
                    setState(BTState.LISTEN)
                    reconnect()
                    break
                }
            }
        }

        fun send(buf: ByteArray) {
            try {
                uOut.write(buf)
            } catch (e: IOException) {
                Log.e(TAG,"send error")
            }
        }

        fun close() {
            try {
                uSocket.close()
            } catch (e: IOException) {
                Log.e(TAG,"socket close failed")
            }
        }

    }

    //
    // INNER CLASS
    //
    inner class ReconnectTask : TimerTask() {

        override fun run() {
            if (mStopped) {
                return
            }
            mBTHandler.post(object : Runnable {
                override fun run() {
                    if (getState() == BTState.CHAT || getState() == BTState.CONNECT) {
                        return
                    }
                    if (myConnectedDeviceAddress != null) {
                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        val device = adapter.getRemoteDevice(myConnectedDeviceAddress)
                        if (device != null) {
                            connect(device)
                        }
                    }
                }
            })
        }

    }

}