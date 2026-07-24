package com.aiyifan.app

import android.app.Application
import com.aiyifan.app.core.data.AppGraph

class AiyifanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.initialize(this)
    }
}
