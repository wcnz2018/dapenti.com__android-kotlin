package com.willchou.dapenti.utils

import java.io.Serializable
import java.util.*

class DObservable<T: Any>(@Volatile private var value: T) : Observable(), Serializable {
    fun get(): T = value
    fun set(v: T, notify: Boolean = true) {
        value = v
        setChanged()
        if (notify)
            notifyObservers(v)
    }
}