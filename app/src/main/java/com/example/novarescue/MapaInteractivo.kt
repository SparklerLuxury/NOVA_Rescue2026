package com.example.novarescue

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.math.pow

/**
 * Actividad que muestra un mapa interactivo con la simulación del impacto sísmico
 * sobre la ubicación del usuario o sobre un punto seleccionado en el mapa.
 */
class MapaInteractivo : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapa: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Posición inicial por defecto (Coordenadas predeterminadas)
    private var posicionActual = LatLng(10.3925, -71.4439)
    private var magnitud = 6.5
    private var profundidad = 30.0

    /**
     * Contrato para solicitar el permiso de ubicación GPS.
     */
    private val pedirPermisoGps = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            if (::mapa.isInitialized && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    mapa.isMyLocationEnabled = true
                    mapa.uiSettings.isMyLocationButtonEnabled = true
                } catch (e: SecurityException) {
                    Log.e("MapaInteractivo", "Error al habilitar la capa de ubicación", e)
                }
            }
            obtenerUbicacionGPS()
        } else {
            Toast.makeText(this, "Permiso de GPS denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_interactivo)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapaGoogle) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapa = googleMap
        mapa.uiSettings.isZoomControlsEnabled = true

        // Listener para actualizar el epicentro al tocar en el mapa
        mapa.setOnMapClickListener { latLng ->
            posicionActual = latLng
            dibujarImpactoSismico()
            mapa.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mapa.isMyLocationEnabled = true
                mapa.uiSettings.isMyLocationButtonEnabled = true
            } catch (e: SecurityException) {
                Log.e("MapaInteractivo", "Error de seguridad al habilitar ubicación en mapa", e)
            }
            obtenerUbicacionGPS()
        } else {
            pedirPermisoGps.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Obtiene la última ubicación conocida del dispositivo usando FusedLocationProviderClient.
     */
    private fun obtenerUbicacionGPS() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        posicionActual = LatLng(location.latitude, location.longitude)
                    }
                    if (::mapa.isInitialized) {
                        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 10f))
                        dibujarImpactoSismico()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MapaInteractivo", "Error al obtener la última ubicación GPS", e)
                    if (::mapa.isInitialized) {
                        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(posicionActual, 10f))
                        dibujarImpactoSismico()
                    }
                }
        } catch (e: SecurityException) {
            Log.e("MapaInteractivo", "Error de seguridad al acceder a GPS", e)
        }
    }

    /**
     * Dibuja los círculos concéntricos representativos de las ondas del impacto sísmico.
     */
    private fun dibujarImpactoSismico() {
        if (!::mapa.isInitialized) return

        mapa.clear()

        val radioBase = 1.8.pow(magnitud) * 1000
        val modificadorProfundidad = 1 + (profundidad / 150)
        val radioMaximo = radioBase * modificadorProfundidad

        val esCritico = magnitud >= 7.0 || (magnitud >= 6.0 && profundidad <= 40)
        val esModerado = magnitud >= 5.0 && !esCritico

        val colorExterior = if (esCritico) Pair("#f97316", "#26F97316") else if (esModerado) Pair("#facc15", "#26FACC15") else Pair("#60a5fa", "#2660A5FA")
        val colorIntermedio = if (esCritico) Pair("#ef4444", "#40EF4444") else if (esModerado) Pair("#fb923c", "#40FB923C") else Pair("#3b82f6", "#403B82F6")
        val colorCentro = if (esCritico) Pair("#b91c1c", "#73B91C1C") else if (esModerado) Pair("#ef4444", "#73EF4444") else Pair("#1d4ed8", "#731D4ED8")

        try {
            mapa.addCircle(
                CircleOptions()
                    .center(posicionActual)
                    .radius(radioMaximo)
                    .strokeColor(Color.parseColor(colorExterior.first))
                    .strokeWidth(2f)
                    .fillColor(Color.parseColor(colorExterior.second))
            )
            mapa.addCircle(
                CircleOptions()
                    .center(posicionActual)
                    .radius(radioMaximo * 0.6)
                    .strokeColor(Color.parseColor(colorIntermedio.first))
                    .strokeWidth(3f)
                    .fillColor(Color.parseColor(colorIntermedio.second))
            )
            mapa.addCircle(
                CircleOptions()
                    .center(posicionActual)
                    .radius(radioMaximo * 0.25)
                    .strokeColor(Color.parseColor(colorCentro.first))
                    .strokeWidth(4f)
                    .fillColor(Color.parseColor(colorCentro.second))
            )
        } catch (e: Exception) {
            Log.e("MapaInteractivo", "Error al dibujar círculos de impacto sísmico", e)
        }

        mapa.addMarker(
            MarkerOptions()
                .position(posicionActual)
                .title("Epicentro NOVARescue")
                .snippet("Mag: $magnitud Mw | Prof: $profundidad km")
        )?.showInfoWindow()
    }
}
