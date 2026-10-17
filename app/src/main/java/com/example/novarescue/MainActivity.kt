package com.example.novarescue

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/*
 * Esta es la pantalla principal de la aplicación NOVARescue.
 * En un RecyclerView se muestran los registros de los reportes de emergencias.
 */
class MainActivity : AppCompatActivity() {

    // REFERENCIAS A COMPONENTES Y ADAPTADOR
    private lateinit var rvReportes: RecyclerView
    private lateinit var adapter: ReporteAdapter


    // CICLO DE VIDA
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inicializar el repositorio centralizado
        ReportesRepository.inicializar(applicationContext)

        // 2. Configurar la lista RecyclerView y su adaptador
        rvReportes = findViewById(R.id.rvReportes)
        rvReportes.layoutManager = LinearLayoutManager(this)

        adapter = ReporteAdapter(ReportesRepository.obtenerReportes())
        rvReportes.adapter = adapter

        // 3. Configurar el Botón Flotante para ir a la creación de un nuevo reporte
        val fabNuevoReporte = findViewById<FloatingActionButton>(R.id.fabNuevoReporte)
        fabNuevoReporte?.setOnClickListener {
            val intent = Intent(this, CrearReportes::class.java)
            startActivity(intent)
        }

        // 4. Suscribirse a cambios en tiempo real del repositorio
        ReportesRepository.escucharReportes { reportes ->
            adapter.actualizarDatos(reportes)
        }
    }

    override fun onResume() {
        super.onResume()
        // Esto sincroniza los datos que ya hayan sido reportados anteriormente para tener la persistencia
        adapter.actualizarDatos(ReportesRepository.obtenerReportes())
    }


    // ADAPTADOR DEL RECYCLERVIEW


    //Adaptador personalizado para gestionar la presentación de las tarjetas de reportes.
    inner class ReporteAdapter(private var reportes: List<Map<String, Any>>) :
        RecyclerView.Adapter<ReporteAdapter.ViewHolder>() {

        //Reemplaza la lista actual de reportes y notifica al RecyclerView para redibujar las tarjetas.
        fun actualizarDatos(nuevosReportes: List<Map<String, Any>>) {
            this.reportes = nuevosReportes
            notifyDataSetChanged()
        }


        //Contenedor de referencias para las vistas individuales de cada tarjeta de reporte.

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTipo: TextView = view.findViewById(R.id.tvTipo)
            val tvPrioridad: TextView = view.findViewById(R.id.tvPrioridad)
            val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
            val tvRecursos: TextView = view.findViewById(R.id.tvRecursos)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reporte, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val reporte = reportes[position]

            // Extraer y formatear el nivel de gravedad / prioridad
            val rawGravedad = (reporte["gravedad"] ?: reporte["prioridad_asignada"])?.toString() ?: "N/A"
            val gravedadTexto = when (rawGravedad.lowercase()) {
                "bajo", "baja" -> "Bajo"
                "medio", "media" -> "Medio"
                "alto", "alta" -> "Alto"
                else -> rawGravedad
            }

            // Asignar textos a los componentes de la vista
            holder.tvTipo.text = reporte["tipo_evento"]?.toString() ?: "Desconocido"
            holder.tvPrioridad.text = gravedadTexto
            holder.tvDescripcion.text = reporte["descripcion"]?.toString() ?: ""

            // Mostrar u ocultar recursos sugeridos según exista información
            val recursos = reporte["recursos_sugeridos"]?.toString()
            if (recursos.isNullOrBlank()) {
                holder.tvRecursos.visibility = View.GONE
            } else {
                holder.tvRecursos.visibility = View.VISIBLE
                holder.tvRecursos.text = recursos
            }

            // Aplicar estilos y colores distintivos según la gravedad del reporte
            when (gravedadTexto) {
                "Alto" -> {
                    holder.tvPrioridad.setTextColor(Color.parseColor("#D32F2F"))
                    holder.tvPrioridad.setBackgroundColor(Color.parseColor("#FFEBEE"))
                }
                "Medio" -> {
                    holder.tvPrioridad.setTextColor(Color.parseColor("#F57F17"))
                    holder.tvPrioridad.setBackgroundColor(Color.parseColor("#FFFDE7"))
                }
                "Bajo" -> {
                    holder.tvPrioridad.setTextColor(Color.parseColor("#388E3C"))
                    holder.tvPrioridad.setBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                else -> {
                    holder.tvPrioridad.setTextColor(Color.parseColor("#757575"))
                    holder.tvPrioridad.setBackgroundColor(Color.parseColor("#EEEEEE"))
                }
            }
        }

        override fun getItemCount(): Int {
            return reportes.size
        }
    }
}
