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

class MainActivity : AppCompatActivity() {

    private lateinit var rvReportes: RecyclerView
    private lateinit var adapter: ReporteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inicializar repositorio
        ReportesRepository.inicializar(applicationContext)

        // 2. Configurar la Lista (RecyclerView)
        rvReportes = findViewById(R.id.rvReportes)
        rvReportes.layoutManager = LinearLayoutManager(this)

        adapter = ReporteAdapter(ReportesRepository.obtenerReportes())
        rvReportes.adapter = adapter

        // Botón Flotante para Nuevo Reporte
        val fabNuevoReporte = findViewById<FloatingActionButton>(R.id.fabNuevoReporte)
        fabNuevoReporte?.setOnClickListener {
            val intent = Intent(this, CrearReportes::class.java)
            startActivity(intent)
        }

        // 3. Escuchar cambios de reportes
        ReportesRepository.escucharReportes { reportes ->
            adapter.actualizarDatos(reportes)
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar vista al regresar
        adapter.actualizarDatos(ReportesRepository.obtenerReportes())
    }

    // --- Adaptador de la Lista ---
    inner class ReporteAdapter(private var reportes: List<Map<String, Any>>) :
        RecyclerView.Adapter<ReporteAdapter.ViewHolder>() {

        fun actualizarDatos(nuevosReportes: List<Map<String, Any>>) {
            this.reportes = nuevosReportes
            notifyDataSetChanged()
        }

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

            val prioridadText = reporte["prioridad_asignada"]?.toString() ?: "N/A"
            holder.tvTipo.text = reporte["tipo_evento"]?.toString() ?: "Desconocido"
            holder.tvPrioridad.text = prioridadText
            holder.tvDescripcion.text = reporte["descripcion"]?.toString() ?: ""
            holder.tvRecursos.text = reporte["recursos_sugeridos"]?.toString() ?: ""

            if (prioridadText.contains("ALTA")) {
                holder.tvPrioridad.setTextColor(Color.parseColor("#D32F2F"))
                holder.tvPrioridad.setBackgroundColor(Color.parseColor("#FFEBEE"))
            } else if (prioridadText.contains("MEDIA")) {
                holder.tvPrioridad.setTextColor(Color.parseColor("#F57F17"))
                holder.tvPrioridad.setBackgroundColor(Color.parseColor("#FFFDE7"))
            } else {
                holder.tvPrioridad.setTextColor(Color.parseColor("#388E3C"))
                holder.tvPrioridad.setBackgroundColor(Color.parseColor("#E8F5E9"))
            }
        }

        override fun getItemCount(): Int {
            return reportes.size
        }
    }
}