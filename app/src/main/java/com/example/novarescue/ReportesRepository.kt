package com.example.novarescue

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repositorio singleton para la administración de reportes de emergencia.
 *
 * Implementa una estrategia de datos híbrida:
 * 1. Memoria rápida para respuesta inmediata en la UI.
 * 2. Persistencia en disco mediante [android.content.SharedPreferences] para soporte offline.
 * 3. Sincronización remota en tiempo real mediante [FirebaseFirestore].
 */
object ReportesRepository {

    // =========================================================================
    // PROPIEDADES PRIVADAS
    // =========================================================================
    private var db: FirebaseFirestore? = null
    private val _reportes = mutableListOf<Map<String, Any>>()
    private var appContext: Context? = null

    // =========================================================================
    // INICIALIZACIÓN
    // =========================================================================

    /**
     * Inicializa la configuración de persistencia local y la conexión con Firebase Firestore.
     * @param context Contexto de la aplicación.
     */
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

    // =========================================================================
    // MÉTODOS PÚBLICOS DE ACCESO A DATOS
    // =========================================================================

    /**
     * Retorna la lista actual de reportes almacenada en memoria.
     */
    fun obtenerReportes(): List<Map<String, Any>> {
        return _reportes
    }

    /**
     * Agrega un nuevo reporte localmente y procede a subirlo a Firebase Firestore.
     *
     * @param reporteData Mapa con los atributos del reporte.
     * @param onSuccess Callback ejecutado al completar con éxito.
     * @param onFailure Callback ejecutado en caso de error.
     */
    fun agregarReporte(
        reporteData: Map<String, Any>,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        // 1. Guardar en la lista de memoria
        _reportes.add(0, reporteData)

        // 2. Persistir localmente en SharedPreferences
        appContext?.let { guardarEnSharedPreferences(it) }

        // 3. Intentar sincronización con Firebase Firestore
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

    /**
     * Establece un oyente (listener) en tiempo real para escuchar cambios en la colección de Firestore.
     *
     * @param onActualizacion Callback invocado con la lista de reportes actualizada.
     */
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

    // =========================================================================
    // PERSISTENCIA EN ALMACENAMIENTO LOCAL (SharedPreferences)
    // =========================================================================

    /**
     * Serializa la lista de reportes a JSON y la guarda en SharedPreferences.
     */
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

    /**
     * Carga y deserializa los reportes cacheados desde SharedPreferences si existen.
     */
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
