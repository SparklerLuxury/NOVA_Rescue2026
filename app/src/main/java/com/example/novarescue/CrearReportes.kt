package com.example.novarescue

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class CrearReportes : AppCompatActivity() {

    // Equivalentes a tus `useState` en React
    private var gravedadSeleccionada = "medio"
    private var latitud: Double? = null
    private var longitud: Double? = null
    private var imagenUri: Uri? = null

    private val MIN_CARACTERES = 10
    private val LIMITE_CARACTERES = 250

    // Vistas
    private lateinit var btnBajo: Button
    private lateinit var btnMedio: Button
    private lateinit var btnAlto: Button
    private lateinit var btnEnviarReporte: Button
    private lateinit var tvErrorDescripcion: TextView
    private lateinit var tvUbicacionEstado: TextView
    private lateinit var ivPrevisualizacion: ImageView

    // Simulador de carga de imagen (Equivalente al input type="file")
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

        inicializarVistas()
        configurarEventos()
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
        // 1. Geolocalización (Simulada para el Rally como tu alert)
        findViewById<Button>(R.id.btnGps).setOnClickListener {
            // Simulamos obtener el GPS (ej. coordenadas de Cabimas)
            latitud = 10.3925
            longitud = -71.4439

            tvUbicacionEstado.text = "Ubicación lista para reportar:\nLat: $latitud | Lng: $longitud"
            tvUbicacionEstado.setTextColor(Color.parseColor("#34D399")) // Emerald text
            tvUbicacionEstado.setBackgroundColor(Color.parseColor("#064E3B")) // Emerald bg oscuro
            Toast.makeText(this, "¡Ubicación GPS obtenida con éxito!", Toast.LENGTH_SHORT).show()
        }

        // 2. Botones de Gravedad
        btnBajo.setOnClickListener { actualizarGravedad("bajo", btnBajo, "#10B981") }
        btnMedio.setOnClickListener { actualizarGravedad("medio", btnMedio, "#F59E0B") }
        btnAlto.setOnClickListener { actualizarGravedad("alto", btnAlto, "#EF4444") }

        // 3. Subir Foto
        findViewById<LinearLayout>(R.id.btnSubirFoto).setOnClickListener {
            selectorDeImagen.launch("image/*") // Abre la galería
        }

        // 4. Validación de Descripción (Equivalente a tu descripcionInvalida en React)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcion)
        val tvContador = findViewById<TextView>(R.id.tvContador)

        etDescripcion.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString().trim()
                val longitud = texto.length

                // Actualizar contador
                tvContador.text = "$longitud / $LIMITE_CARACTERES"
                if (longitud > 230) tvContador.setTextColor(Color.parseColor("#EF4444"))
                else tvContador.setTextColor(Color.parseColor("#6B7280"))

                // Validar botón y mensaje de error
                if (longitud < MIN_CARACTERES) {
                    val faltan = MIN_CARACTERES - longitud
                    if (longitud > 0) {
                        tvErrorDescripcion.visibility = View.VISIBLE
                        tvErrorDescripcion.text = "Faltan $faltan caracteres para validar el reporte."
                    } else {
                        tvErrorDescripcion.visibility = View.GONE
                    }
                    // Desactivar botón (Azul grisáceo claro)
                    btnEnviarReporte.isEnabled = false
                    btnEnviarReporte.setBackgroundColor(Color.parseColor("#CBD5E1"))
                    btnEnviarReporte.setTextColor(Color.parseColor("#64748B"))
                } else {
                    tvErrorDescripcion.visibility = View.GONE
                    // Activar botón (Azul vibrante)
                    btnEnviarReporte.isEnabled = true
                    btnEnviarReporte.setBackgroundColor(Color.parseColor("#0284C7")) // Sky Blue
                    btnEnviarReporte.setTextColor(Color.WHITE)
                }
            }
        })

        // 5. Submit Handle
        btnEnviarReporte.setOnClickListener {
            if (latitud == null) {
                Toast.makeText(this, "Por favor, usa el botón de GPS antes de enviar.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val tipoDano = findViewById<Spinner>(R.id.spinnerTipoDano).selectedItem.toString()
            val descripcionFinal = etDescripcion.text.toString().trim()

            // Armar el mapa de datos para tu ReportesRepository
            val reporteData = hashMapOf<String, Any>(
                "tipo_evento" to tipoDano,
                "gravedad" to gravedadSeleccionada,
                "descripcion" to descripcionFinal,
                "latitud" to latitud!!,
                "longitud" to longitud!!,
                "timestamp" to System.currentTimeMillis()
                // La IA de prioridad que creamos antes puede ir aquí
            )

            ReportesRepository.agregarReporte(reporteData,
                onSuccess = {
                    Toast.makeText(this, "Reporte enviado exitosamente", Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFailure = { error ->
                    Toast.makeText(this, "Error al enviar: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // Función para manejar el estilo dinámico de los botones de gravedad (como tus templates strings en React)
    private fun actualizarGravedad(nivel: String, botonActivo: Button, colorHex: String) {
        gravedadSeleccionada = nivel

        // Resetear todos a Blanco con texto azul grisáceo (para que no queden negros en el fondo pastel)
        val colorInactivo = Color.parseColor("#FFFFFF")
        val textoInactivo = Color.parseColor("#64748B")

        btnBajo.setBackgroundColor(colorInactivo)
        btnBajo.setTextColor(textoInactivo)
        btnMedio.setBackgroundColor(colorInactivo)
        btnMedio.setTextColor(textoInactivo)
        btnAlto.setBackgroundColor(colorInactivo)
        btnAlto.setTextColor(textoInactivo)

        // Pintar el activo (con el color que le pases: verde, ámbar o rojo)
        botonActivo.setBackgroundColor(Color.parseColor(colorHex))
        botonActivo.setTextColor(Color.WHITE)
    }
}