package com.mag.slam3dvideo.videmodels;

import android.app.Application
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.AndroidViewModel

open class ObservableViewModel(application: Application) : AndroidViewModel(application),
    Observable {

    @delegate:Transient
    protected val mCallBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        mCallBacks.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        mCallBacks.remove(callback)
    }

    fun notifyChange() {
        mCallBacks.notifyChange(this, 0)
    }

    fun notifyChange(viewId: Int) {
        mCallBacks.notifyChange(this, viewId)
    }
}