package com.example.novarescue

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/*
 * Actividad encargada de la creación y envío de reportes de emergencia.
 *
 * Funcionalidades principales:
 * - Selección del nivel de gravedad (Bajo, Medio, Alto).
 * - Captura de ubicación GPS.
 * - Adjuntar previsualización de imágenes.
 * - Validación en tiempo real del tamaño de la descripción.
 * - Registro del reporte en [ReportesRepository] (Firebase / almacenamiento local).
 * - Envío automático de alerta vía SMS como mecanismo de respaldo (fallback).
 */
class CrearReportes : AppCompatActivity() {

    // CONSTANTES Y CONFIGURACIONES
    private val MIN_CARACTERES = 10
    private val LIMITE_CARACTERES = 250

    // VARIABLES DE ESTADO Y DATOS
    private var gravedadSeleccionada = "medio"
    private var latitud: Double? = null
    private var longitud: Double? = null
    private var imagenUri: Uri? = null
    private var alertaPendiente: AlertaTemporal? = null


    // REFERENCIAS A COMPONENTES DE LA INTERFAZ (UI)
    private lateinit var btnBajo: Button
    private lateinit var btnMedio: Button
    private lateinit var btnAlto: Button
    private lateinit var btnEnviarReporte: Button
    private lateinit var tvErrorDescripcion: TextView
    private lateinit var tvUbicacionEstado: TextView
    private lateinit var ivPrevisualizacion: ImageView

    // Esto es para solicitar permisos para enviar mensajes y acceder llamadas
    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        val phoneStateGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: false

        if (smsGranted && phoneStateGranted) {
            Toast.makeText(this, "Permisos concedidos. Enviando SMS...", Toast.LENGTH_SHORT).show()
            alertaPendiente?.let {
                enviarAlertaSMS(it.tipo, it.gravedad, it.lat, it.lng)
                alertaPendiente = null
            }
        } else {
            Toast.makeText(this, "Permisos denegados. No se pudo enviar el reporte.", Toast.LENGTH_LONG).show()
        }
    }

    //Launcher para la selección de imágenes desde la galería del dispositivo.
    private val selectorDeImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imagenUri = it
            ivPrevisualizacion.setImageURI(it)
            ivPrevisualizacion.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_reportes)

        // Inicializar componentes de la vista y configurar listeners de eventos
        inicializarVistas()
        configurarEventos()

        // Verificar y solicitar permisos de SMS en el arranque si es necesario
        if (!tienePermisosSms()) {
            requestSmsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }
    }

    // =========================================================================
    // INICIALIZACIÓN Y CONFIGURACIÓN DE LA INTERFAZ
    // =========================================================================

   // Esto es para los elementos de la interfaz a la hora de crear reportes
    private fun inicializarVistas() {
        btnBajo = findViewById(R.id.btnBajo)
        btnMedio = findViewById(R.id.btnMedio)
        btnAlto = findViewById(R.id.btnAlto)
        btnEnviarReporte = findViewById(R.id.btnEnviarReporte)
        tvErrorDescripcion = findViewById(R.id.tvErrorDescripcion)
        tvUbicacionEstado = findViewById(R.id.tvUbicacionEstado)
        ivPrevisualizacion = findViewById(R.id.ivPrevisualizacion)
    }

    private fun configurarEventos() {
        // Evento para obtener coordenadas GPS de la ubicación
        findViewById<Button>(R.id.btnGps).setOnClickListener {
            // Aqui hay que cambiar estos valores para que puedan agarrar datos de GPS
            latitud = 10.3925
            longitud = -71.4439

            tvUbicacionEstado.text = "Ubicación lista para reportar:\nLat: $latitud | Lng: $longitud"
            tvUbicacionEstado.setTextColor(Color.parseColor("#34D399"))
            tvUbicacionEstado.setBackgroundColor(Color.parseColor("#064E3B"))
            Toast.makeText(this, "¡Ubicación GPS obtenida con éxito!", Toast.LENGTH_SHORT).show()
        }

        // Eventos para selección del nivel de gravedad
        btnBajo.setOnClickListener { actualizarGravedad("bajo", btnBajo, "#10B981") }
        btnMedio.setOnClickListener { actualizarGravedad("medio", btnMedio, "#F59E0B") }
        btnAlto.setOnClickListener { actualizarGravedad("alto", btnAlto, "#EF4444") }

        // Evento para adjuntar imagen
        findViewById<LinearLayout>(R.id.btnSubirFoto).setOnClickListener {
            selectorDeImagen.launch("image/*")
        }

        // Validación dinámica de la descripción del reporte
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val tvContador = findViewById<TextView>(R.id.tvContador)

        etDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().trim()
                val longitudText = texto.length

                // Actualizar contador de caracteres
                tvContador.text = "$longitudText / $LIMITE_CARACTERES"
                if (longitudText > 230) {
                    tvContador.setTextColor(Color.parseColor("#EF4444"))
                } else {
                    tvContador.setTextColor(Color.parseColor("#6B7280"))
                }

                // Validar longitud mínima permitida
                if (longitudText < MIN_CARACTERES) {
                    val faltan = MIN_CARACTERES - longitudText

                    if (longitudText > 0) {
                        tvErrorDescripcion.visibility = View.VISIBLE
                        tvErrorDescripcion.text = "Faltan $faltan caracteres para validar el reporte."
                    } else {
                        tvErrorDescripcion.visibility = View.GONE
                    }
                    btnEnviarReporte.isEnabled = false
                    btnEnviarReporte.setBackgroundColor(Color.parseColor("#CBD5E1"))
                    btnEnviarReporte.setTextColor(Color.parseColor("#64748B"))
                } else {
                    tvErrorDescripcion.visibility = View.GONE
                    btnEnviarReporte.isEnabled = true
                    btnEnviarReporte.setBackgroundColor(Color.parseColor("#0284C7"))
                    btnEnviarReporte.setTextColor(Color.WHITE)
                }
            }
        })

        // Evento principal para enviar el reporte registrado
        btnEnviarReporte.setOnClickListener {
            if (latitud == null) {
                Toast.makeText(this, "Por favor, usa el botón de GPS antes de enviar.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val tipoDano = findViewById<Spinner>(R.id.spinnerTipoDano).selectedItem.toString()
            val descripcionFinal = etDescripcion.text.toString().trim()

            // Estructura de datos del reporte de emergencia
            val reporteData = hashMapOf<String, Any>(
                "tipo_evento" to tipoDano,
                "gravedad" to gravedadSeleccionada,
                "prioridad_asignada" to gravedadSeleccionada,
                "descripcion" to descripcionFinal,
                "latitud" to latitud!!,
                "longitud" to longitud!!,
                "timestamp" to System.currentTimeMillis()
            )

            // Registrar reporte en el repositorio y enviar alerta SMS
            ReportesRepository.agregarReporte(reporteData,
                onSuccess = {
                    Toast.makeText(this, "Reporte registrado exitosamente", Toast.LENGTH_SHORT).show()
                    val enviado = enviarAlertaSMS(tipoDano, gravedadSeleccionada, latitud!!, longitud!!)
                    if (enviado) {
                        finish()
                    }
                },
                onFailure = { error ->
                    val enviado = enviarAlertaSMS(tipoDano, gravedadSeleccionada, latitud!!, longitud!!)
                    Toast.makeText(this, "Error en red. Alerta enviada por SMS: ${error.message}", Toast.LENGTH_SHORT).show()
                    if (enviado) {
                        finish()
                    }
                }
            )
        }
    }

    /**
     * Actualiza visualmente la selección de gravedad cambiando el color del botón activo
     * y restableciendo los demás botones al estado inactivo.
     */
    private fun actualizarGravedad(nivel: String, botonActivo: Button, colorHex: String) {
        gravedadSeleccionada = nivel

        val colorInactivo = Color.parseColor("#FFFFFF")
        val textoInactivo = Color.parseColor("#64748B")

        btnBajo.setBackgroundColor(colorInactivo)
        btnBajo.setTextColor(textoInactivo)
        btnMedio.setBackgroundColor(colorInactivo)
        btnMedio.setTextColor(textoInactivo)
        btnAlto.setBackgroundColor(colorInactivo)
        btnAlto.setTextColor(textoInactivo)

        botonActivo.setBackgroundColor(Color.parseColor(colorHex))
        botonActivo.setTextColor(Color.WHITE)
    }

    // =========================================================================
    // LÓGICA DE ALERTAS Y PERMISOS SMS
    // =========================================================================

    /**
     * Verifica si la aplicación posee los permisos necesarios para el envío de SMS.
     * @return `true` si se otorgan los permisos SEND_SMS y READ_PHONE_STATE, `false` en caso contrario.
     */
    private fun tienePermisosSms(): Boolean {
        val sendSmsOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val phoneStateOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return sendSmsOk && phoneStateOk
    }

    /**
     * Envía un mensaje SMS de alerta de emergencia con las coordenadas del evento.
     * Si no se cuentan con los permisos adecuados, guarda la alerta como pendiente y los solicita.
     */
    private fun enviarAlertaSMS(tipo: String, gravedad: String, lat: Double, lng: Double): Boolean {
        if (!tienePermisosSms()) {
            alertaPendiente = AlertaTemporal(tipo, gravedad, lat, lng)
            requestSmsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
            return false
        }

        try {
            val numeroAutoridad = "(650) 555-1212"
            val mensaje = "🚨 EMERGENCIA NOVARescue: $tipo (Gravedad: $gravedad). Ubicación: https://maps.google.com/?q=$lat,$lng"

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val partes = smsManager.divideMessage(mensaje)
            if (partes.size > 1) {
                smsManager.sendMultipartTextMessage(numeroAutoridad, null, partes, null, null)
            } else {
                smsManager.sendTextMessage(numeroAutoridad, null, mensaje, null, null)
            }
            Toast.makeText(this, "SMS enviado al emulador ($numeroAutoridad)", Toast.LENGTH_LONG).show()
            return true

        } catch (e: Exception) {
            Log.e("SMS", "Fallo al enviar SMS", e)
            Toast.makeText(this, "Error al enviar SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    // =========================================================================
    // CLASES DE DATOS
    // =========================================================================

    /**
     * Estructura temporal para conservar los datos de una alerta mientras se conceden los permisos de SMS.
     */
    data class AlertaTemporal(val tipo: String, val gravedad: String, val lat: Double, val lng: Double)
}
