package com.heysafe.app

import android.app.Application
import com.heysafe.app.di.ServiceLocator

class HeySafeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
