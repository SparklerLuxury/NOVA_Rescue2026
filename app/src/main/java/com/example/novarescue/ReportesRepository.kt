package com.example.novarescue

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject

object ReportesRepository {

    private var db: FirebaseFirestore? = null
    private val _reportes = mutableListOf<Map<String, Any>>()
    private var appContext: Context? = null

    fun inicializar(context: Context) {
        appContext = context.applicationContext
        cargarDeSharedPreferences(context.applicationContext)

        if (db != null) return

        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val firestore = FirebaseFirestore.getInstance()
                @Suppress("DEPRECATION")
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                firestore.firestoreSettings = settings
                db = firestore
            } else {
                Log.w("ReportesRepository", "FirebaseApp no está inicializado. Agrega google-services.json al proyecto.")
            }
        } catch (e: Exception) {
            Log.e("ReportesRepository", "Error al inicializar Firebase", e)
        }
    }

    fun obtenerReportes(): List<Map<String, Any>> {
        return _reportes
    }

    fun agregarReporte(
        reporteData: Map<String, Any>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        // Guardar localmente en memoria
        _reportes.add(0, reporteData)

        // Persistir en disco (SharedPreferences) para sobrevivir al reinicio de la app
        appContext?.let { guardarEnSharedPreferences(it) }

        val firestore = db
        if (firestore != null) {
            firestore.collection("reportes_emergencia")
                .add(reporteData)
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("ReportesRepository", "Error en Firebase, mantenido en caché local", e)
                    onFailure(e)
                }
        } else {
            onSuccess()
        }
    }

    fun escucharReportes(onActualizacion: (List<Map<String, Any>>) -> Unit) {
        val firestore = db
        if (firestore != null) {
            firestore.collection("reportes_emergencia")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("ReportesRepository", "Error red Firebase: ${error.message}")
                        onActualizacion(_reportes)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        _reportes.clear()
                        for (doc in snapshots) {
                            _reportes.add(doc.data)
                        }
                        appContext?.let { guardarEnSharedPreferences(it) }
                        onActualizacion(_reportes)
                    }
                }
        } else {
            onActualizacion(_reportes)
        }
    }

    // --- Persistencia en Disco Local (SharedPreferences) ---

    private fun guardarEnSharedPreferences(context: Context) {
        try {
            val jsonArray = JSONArray()
            for (map in _reportes) {
                val jsonObject = JSONObject()
                for ((key, value) in map) {
                    jsonObject.put(key, value)
                }
                jsonArray.put(jsonObject)
            }
            val prefs = context.getSharedPreferences("novarescue_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("reportes_cache", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("ReportesRepository", "Error al guardar en SharedPreferences", e)
        }
    }

    private fun cargarDeSharedPreferences(context: Context) {
        try {
            val prefs = context.getSharedPreferences("novarescue_prefs", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("reportes_cache", null) ?: return
            val jsonArray = JSONArray(jsonString)
            val listaCargada = mutableListOf<Map<String, Any>>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = jsonObject.get(key)
                }
                listaCargada.add(map)
            }
            if (listaCargada.isNotEmpty() && _reportes.isEmpty()) {
                _reportes.addAll(listaCargada)
            }
        } catch (e: Exception) {
            Log.e("ReportesRepository", "Error al cargar de SharedPreferences", e)
        }
    }
}