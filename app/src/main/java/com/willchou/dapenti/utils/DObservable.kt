package com.willchou.dapenti.utils

import java.io.Serializable
import java.util.*

class DObservable<T: Any> : Observable(), Serializable {
    var value: T? = null
    set(v) {
        field = v
        setChanged()
        notifyObservers(field)
    }
}