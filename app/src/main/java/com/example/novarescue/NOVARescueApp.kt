package com.example.novarescue

import android.app.Application

class NOVARescueApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar el repositorio y la persistencia al arrancar la aplicación
        ReportesRepository.inicializar(this)
    }
}