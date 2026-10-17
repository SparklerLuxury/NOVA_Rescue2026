package com.example.novarescue

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// 1. Modelo de Datos para los refugios
data class Refugio(
    val nombre: String,
    val tipo: String, // "Salud", "Acopio", "Refugio"
    val direccion: String,
    val porcentajeLleno: Int // 0 a 100
)

class RefugiosActivity : AppCompatActivity() {

    private lateinit var rvRefugios: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_refugios)

        rvRefugios = findViewById(R.id.rvRefugios)
        rvRefugios.layoutManager = LinearLayoutManager(this)

        // 2. Lista de datos simulados para presentar en el Rally
        val listaDeRefugios = listOf(
            Refugio("Hospital General Dr. Adolfo D'Empaire", "Salud", "Av. Andrés Bello, a 1.2 km de tu ubicación", 92),
            Refugio("Polideportivo Central", "Refugio", "Sector Casco Central, a 3.5 km de tu ubicación", 45),
            Refugio("Escuela Básica Dr. Rafael Caldera", "Acopio", "Zona Norte, a 4.1 km de tu ubicación", 15),
            Refugio("Estadio Municipal", "Refugio", "Av. Intercomunal, a 6.0 km de tu ubicación", 78)
        )

        // 3. Conectar los datos con la interfaz
        val adapter = RefugiosAdapter(listaDeRefugios)
        rvRefugios.adapter = adapter
    }

    // --- ADAPTADOR PARA PINTAR LAS TARJETAS ---
    inner class RefugiosAdapter(private val refugios: List<Refugio>) :
        RecyclerView.Adapter<RefugiosAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(R.id.tvNombreRefugio)
            val tvTipo: TextView = view.findViewById(R.id.tvTipoCentro)
            val tvCapacidad: TextView = view.findViewById(R.id.tvCapacidad)
            val tvDireccion: TextView = view.findViewById(R.id.tvDireccion)
            val btnIr: Button = view.findViewById(R.id.btnIrAlLugar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_refugio, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val refugio = refugios[position]

            holder.tvNombre.text = refugio.nombre
            holder.tvDireccion.text = refugio.direccion
            holder.tvCapacidad.text = "${refugio.porcentajeLleno}% Lleno"

            // Estilos dinámicos según el tipo de lugar
            when (refugio.tipo) {
                "Salud" -> {
                    holder.tvTipo.text = "🏥 CENTRO DE SALUD"
                    holder.tvTipo.setTextColor(Color.parseColor("#DC2626")) // Rojo médico
                }
                "Acopio" -> {
                    holder.tvTipo.text = "📦 ZONA DE ACOPIO (Insumos)"
                    holder.tvTipo.setTextColor(Color.parseColor("#D97706")) // Naranja logístico
                }
                "Refugio" -> {
                    holder.tvTipo.text = "⛺ REFUGIO TEMPORAL (Vivienda)"
                    holder.tvTipo.setTextColor(Color.parseColor("#D15CE3")) // Morado NOVARescue
                }
            }

            // Estilos dinámicos según la capacidad (Para evitar saturar centros)
            if (refugio.porcentajeLleno >= 85) {
                holder.tvCapacidad.setBackgroundColor(Color.parseColor("#FEE2E2"))
                holder.tvCapacidad.setTextColor(Color.parseColor("#DC2626")) // Rojo (Casi lleno)
            } else if (refugio.porcentajeLleno >= 50) {
                holder.tvCapacidad.setBackgroundColor(Color.parseColor("#FEF3C7"))
                holder.tvCapacidad.setTextColor(Color.parseColor("#D97706")) // Amarillo (Medio)
            } else {
                holder.tvCapacidad.setBackgroundColor(Color.parseColor("#D1FAE5"))
                holder.tvCapacidad.setTextColor(Color.parseColor("#059669")) // Verde (Disponible)
            }

            // Botón de acción simulado
            holder.btnIr.setOnClickListener {
                Toast.makeText(holder.itemView.context, "Trazando ruta segura hacia: ${refugio.nombre}...", Toast.LENGTH_SHORT).show()
            }
        }

        override fun getItemCount(): Int = refugios.size
    }
}
