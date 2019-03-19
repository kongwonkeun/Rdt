package com.rdt.one

enum class RequestCode(val i: Int) {
    PERMISSION(101),
    BLUETOOTH_ENABLE(102),
    CONNECT_DEVICE(103)
}

// Bluetooth Connection State
enum class BTState(val i: Int) {
    NONE(0),
    LISTEN(1),
    CONNECT(2),
    CHAT(3),
    CHAT_DATA(4)
}

// Bluetooth Message Type
enum class BTMessage(val i: Int) {
    NONE(0),
    STATE_CHANGE(1),
    READ(2),
    WRITE(3),
    DEVICE_NAME(4),
    TOAST(5)
}

// Bluetooth Message Key
enum class BTKey(val s: String) {
    DEVICE_NAME("device_name"),
    DEVICE_ADDRESS("device_address"),
    TOAST("toast")
}