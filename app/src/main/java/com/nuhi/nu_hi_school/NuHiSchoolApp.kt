package com.nuhi.nu_hi_school

import android.app.Application

class NuHiSchoolApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NuHiSchoolApp
            private set
    }
}
