package org.associations.project

import android.app.Application
import org.associations.project.di.initKoin
import org.koin.android.ext.koin.androidContext

class AssociationsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@AssociationsApplication)
        }
    }
}
