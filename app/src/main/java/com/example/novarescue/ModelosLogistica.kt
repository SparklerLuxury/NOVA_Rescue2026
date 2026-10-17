package com.example.novarescue

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.view.View
import android.widget.*
class ModelosLogistica : AppCompatActivity() {
        data class Voluntario(
            val id: Long,
            val categoria: String,
            val rama: String,
            val nombre: String,
            val email: String,
            val telefono: String,
            var estado: String = "disponible" // "disponible" o "en_operacion"
        )

        data class Asignacion(
            val reporteId: String,
            val voluntarioEmail: String,
            val voluntarioNombre: String,
            var estado: String = "en_operacion" // "en_operacion" o "finalizada"
        )




        // "Estado" equivalente a React
        private var vistaActiva = "registro"
        private var usuarioActual: String? = null

        // Almacenamiento local (Equivalente al localStorage de tu React)
        private val voluntarios = mutableListOf<Voluntario>()
        private val asignaciones = mutableListOf<Asignacion>()

        // Diccionario de categorías (Igual al de tu React)
        private val categoriasMap = mapOf(
            "Medicina y Salud" to listOf("Médico General", "Pediatra", "Cirujano", "Enfermero/a", "Paramédico"),
            "Rescate y Salvamento" to listOf("Búsqueda y Rescate (SAR)", "Bombero", "Rescate Urbano"),
            "Logística y Transporte" to listOf("Conductor (Carga Pesada)", "Organizador de Suministros")
        )

        // Referencias a Vistas
        private lateinit var vistaRegistro: LinearLayout
        private lateinit var vistaAcceso: LinearLayout
        private lateinit var vistaDespacho: LinearLayout
        private lateinit var btnTabRegistro: Button
        private lateinit var btnTabAcceso: Button
        private lateinit var btnTabDespacho: Button

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_modelos_logistica)

            inicializarVistas()
            configurarPestanas()
            configurarFormulario()
            actualizarEstadisticas()
        }

        private fun inicializarVistas() {
            vistaRegistro = findViewById(R.id.vistaRegistro)
            vistaAcceso = findViewById(R.id.vistaAcceso)
            vistaDespacho = findViewById(R.id.vistaDespacho)

            btnTabRegistro = findViewById(R.id.btnTabRegistro)
            btnTabAcceso = findViewById(R.id.btnTabAcceso)
            btnTabDespacho = findViewById(R.id.btnTabDespacho)

            // Botón Ingresar (Login)
            findViewById<Button>(R.id.btnIngresar).setOnClickListener {
                val email = findViewById<EditText>(R.id.etEmailAcceso).text.toString().trim()
                val voluntarioExiste = voluntarios.find { it.email.equals(email, ignoreCase = true) }

                if (voluntarioExiste != null) {
                    usuarioActual = voluntarioExiste.email
                    Toast.makeText(this, "Bienvenido, \${voluntarioExiste.nombre}", Toast.LENGTH_SHORT).show()
                    cambiarVista("despacho")
                } else {
                    Toast.makeText(this, "Correo no encontrado. Regístrese primero.", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Cambia la pestaña activa (Equivalente a setVistaActiva en React)
        private fun configurarPestanas() {
            btnTabRegistro.setOnClickListener { cambiarVista("registro") }
            btnTabAcceso.setOnClickListener { cambiarVista("acceso") }
            btnTabDespacho.setOnClickListener { cambiarVista("despacho") }
        }

        private fun cambiarVista(nuevaVista: String) {
            vistaActiva = nuevaVista

            // Ocultar todo primero
            vistaRegistro.visibility = View.GONE
            vistaAcceso.visibility = View.GONE
            vistaDespacho.visibility = View.GONE

            // Resetear colores de botones a gris claro
            val colorInactivo = Color.parseColor("#FFFFFF")
            val colorTextoInactivo = Color.parseColor("#6B7280")

            btnTabRegistro.setBackgroundColor(colorInactivo)
            btnTabRegistro.setTextColor(colorTextoInactivo)
            btnTabAcceso.setBackgroundColor(colorInactivo)
            btnTabAcceso.setTextColor(colorTextoInactivo)
            btnTabDespacho.setBackgroundColor(colorInactivo)
            btnTabDespacho.setTextColor(colorTextoInactivo)

            // Mostrar la vista activa y pintar su botón
            when (nuevaVista) {
                "registro" -> {
                    vistaRegistro.visibility = View.VISIBLE
                    btnTabRegistro.setBackgroundColor(Color.parseColor("#D15CE3")) // Morado NOVARescue
                    btnTabRegistro.setTextColor(Color.WHITE)
                }
                "acceso" -> {
                    vistaAcceso.visibility = View.VISIBLE
                    btnTabAcceso.setBackgroundColor(Color.parseColor("#D15CE3"))
                    btnTabAcceso.setTextColor(Color.WHITE)
                }
                "despacho" -> {
                    vistaDespacho.visibility = View.VISIBLE
                    btnTabDespacho.setBackgroundColor(Color.parseColor("#D15CE3"))
                    btnTabDespacho.setTextColor(Color.WHITE)
                    actualizarEstadisticas()
                    pintarVoluntarios() // Equivalente a hacer el .map() en React
                }
            }
        }

        private fun configurarFormulario() {
            val spinnerCategoria = findViewById<Spinner>(R.id.spinnerCategoria)
            val spinnerRama = findViewById<Spinner>(R.id.spinnerRama)

            // Llenar Spinner Categoría
            val adapterCat = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriasMap.keys.toList())
            adapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapterCat

            // Actualizar Spinner Rama cuando cambia Categoría
            spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val categoriaSeleccionada = spinnerCategoria.selectedItem.toString()
                    val ramas = categoriasMap[categoriaSeleccionada] ?: emptyList()

                    val adapterRama = ArrayAdapter(this@ModelosLogistica, android.R.layout.simple_spinner_item, ramas)
                    adapterRama.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerRama.adapter = adapterRama
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            // Lógica de Registro (Equivalente a registrarVoluntario en React)
            findViewById<Button>(R.id.btnCompletarRegistro).setOnClickListener {
                val email = findViewById<EditText>(R.id.etEmailRegistro).text.toString().trim()
                val nombre = findViewById<EditText>(R.id.etNombre).text.toString().trim()
                val telefono = findViewById<EditText>(R.id.etTelefono).text.toString().trim()
                val categoria = spinnerCategoria.selectedItem.toString()
                val rama = spinnerRama.selectedItem?.toString() ?: ""

                if (voluntarios.any { it.email.equals(email, ignoreCase = true) }) {
                    Toast.makeText(this, "El correo ya existe. Vaya a Acceso.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                if (telefono.length != 7) {
                    Toast.makeText(this, "El teléfono local debe tener 7 dígitos.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val nuevoVoluntario = Voluntario(
                    id = System.currentTimeMillis(),
                    categoria = categoria,
                    rama = rama,
                    nombre = nombre,
                    email = email,
                    telefono = "+58 0412-\$telefono"
                )

                voluntarios.add(nuevoVoluntario)
                usuarioActual = email

                Toast.makeText(this, "¡Registro exitoso! Bienvenido \$nombre", Toast.LENGTH_SHORT).show()
                cambiarVista("despacho")

                // Limpiar formulario
                findViewById<EditText>(R.id.etEmailRegistro).text.clear()
                findViewById<EditText>(R.id.etNombre).text.clear()
                findViewById<EditText>(R.id.etTelefono).text.clear()
            }
        }

        // Equivalente a calcular los `.length` del React
        private fun actualizarEstadisticas() {
            val disponibles = voluntarios.count { it.estado == "disponible" }
            val enOperacion = voluntarios.count { it.estado == "en_operacion" }

            findViewById<TextView>(R.id.tvDisponibles)?.text = disponibles.toString()
            findViewById<TextView>(R.id.tvEnOperacion)?.text = enOperacion.toString()
        }

        // Equivalente a {voluntarios.map(v => ...)} en el JSX
        private fun pintarVoluntarios() {
            val contenedor = findViewById<LinearLayout>(R.id.contenedorVoluntarios)
            contenedor.removeAllViews()

            if (voluntarios.isEmpty()) {
                val tvVacio = TextView(this)
                tvVacio.text = "No hay voluntarios registrados."
                tvVacio.setPadding(0, 16, 0, 0)
                contenedor.addView(tvVacio)
                return
            }

            for (v in voluntarios) {
                val vistaVoluntario = TextView(this)
                vistaVoluntario.text = "🧑‍⚕️ \${v.nombre} (${v.rama}) - Estado:${v.estado.uppercase()}"
                vistaVoluntario.setPadding(16, 16, 16, 16)
                vistaVoluntario.setBackgroundColor(Color.parseColor("#FFFFFF"))

                contenedor.addView(vistaVoluntario)

                // Un pequeño separador
                val separador = View(this)
                separador.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                separador.setBackgroundColor(Color.parseColor("#E5E7EB"))
                contenedor.addView(separador)
            }
        }
    }