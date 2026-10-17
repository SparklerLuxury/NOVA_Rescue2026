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

class CrearReportes : AppCompatActivity() {

    private var gravedadSeleccionada = "medio"
    private var latitud: Double? = null
    private var longitud: Double? = null
    private var imagenUri: Uri? = null

    private val MIN_CARACTERES = 10
    private val LIMITE_CARACTERES = 250

    private lateinit var btnBajo: Button
    private lateinit var btnMedio: Button
    private lateinit var btnAlto: Button
    private lateinit var btnEnviarReporte: Button
    private lateinit var tvErrorDescripcion: TextView
    private lateinit var tvUbicacionEstado: TextView
    private lateinit var ivPrevisualizacion: ImageView

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

    private val selectorDeImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imagenUri = it
            ivPrevisualizacion.setImageURI(it)
            ivPrevisualizacion.visibility = View.VISIBLE
        }
    }

    private var alertaPendiente: AlertaTemporal? = null

    data class AlertaTemporal(val tipo: String, val gravedad: String, val lat: Double, val lng: Double)

    private fun tienePermisosSms(): Boolean {
        val sendSmsOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        val phoneStateOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        return sendSmsOk && phoneStateOk
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_reportes)

        inicializarVistas()
        configurarEventos()

        if (!tienePermisosSms()) {
            requestSmsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }
    }

    private fun enviarAlertaSMS(tipo: String, gravedad: String, lat: Double, lng: Double): Boolean {
        // Se tiene que revisar que tenga los permisos para enviar SMS y manejar llamadas
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
            // CORRECCIÓN CLAVE: Se usa 5554 para enviarse el SMS al propio emulador.
            // Si tienes otro emulador abierto (ej: 5556), cámbialo a "5556"
            // El "(650) 555-1212" es por parte del numero telefonico del emulador. Debemos de agregar numeros de emergencia como variables
            val numeroAutoridad = "(650) 555-1212"

            // Este es el contenido del SMS que se va a enviar junto con la localizacion de Google Maps.
            val mensaje = "🚨 EMERGENCIA NOVARescue: $tipo (Gravedad: $gravedad). Ubicación: https://maps.google.com/?q=$lat,$lng"

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(SmsManager::class.java)
            } else {
                // Esto es para que Android Studio deje de hacer la advertencia de la deprecacion del sistema SMSManager
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
        findViewById<Button>(R.id.btnGps).setOnClickListener {
            latitud = 10.3925
            longitud = -71.4439

            tvUbicacionEstado.text = "Ubicación lista para reportar:\nLat: $latitud | Lng: $longitud"
            tvUbicacionEstado.setTextColor(Color.parseColor("#34D399"))
            tvUbicacionEstado.setBackgroundColor(Color.parseColor("#064E3B"))
            Toast.makeText(this, "¡Ubicación GPS obtenida con éxito!", Toast.LENGTH_SHORT).show()
        }

        btnBajo.setOnClickListener { actualizarGravedad("bajo", btnBajo, "#10B981") }
        btnMedio.setOnClickListener { actualizarGravedad("medio", btnMedio, "#F59E0B") }
        btnAlto.setOnClickListener { actualizarGravedad("alto", btnAlto, "#EF4444") }

        findViewById<LinearLayout>(R.id.btnSubirFoto).setOnClickListener {
            selectorDeImagen.launch("image/*")
        }

        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val tvContador = findViewById<TextView>(R.id.tvContador)

        etDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().trim()
                val longitud = texto.length

                tvContador.text = "$longitud / $LIMITE_CARACTERES"
                if (longitud > 230) tvContador.setTextColor(Color.parseColor("#EF4444"))
                else tvContador.setTextColor(Color.parseColor("#6B7280"))

                if (longitud < MIN_CARACTERES) {
                    // CORRECCIÓN CLAVE: Agregado el signo de resta
                    val faltan = MIN_CARACTERES - longitud

                    if (longitud > 0) {
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

        btnEnviarReporte.setOnClickListener {
            if (latitud == null) {
                Toast.makeText(this, "Por favor, usa el botón de GPS antes de enviar.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val tipoDano = findViewById<Spinner>(R.id.spinnerTipoDano).selectedItem.toString()
            val descripcionFinal = etDescripcion.text.toString().trim()

            val reporteData = hashMapOf<String, Any>(
                "tipo_evento" to tipoDano,
                "gravedad" to gravedadSeleccionada,
                "descripcion" to descripcionFinal,
                "latitud" to latitud!!,
                "longitud" to longitud!!,
                "timestamp" to System.currentTimeMillis()
            )

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
}