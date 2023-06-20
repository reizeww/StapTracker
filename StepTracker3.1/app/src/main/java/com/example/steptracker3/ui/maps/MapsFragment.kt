package com.example.steptracker3.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.steptracker3.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions


class MapsFragment : Fragment(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    data class MarkerData(val iconResId: Int,  val name: String, val time: String, val Distance: String, val Calories: String, val Step: String, val Information: String)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        requestLocationPermission()

        mMap.setOnMarkerClickListener { marker ->
            openMarkerDetailsFragment(marker)
            true
        }

        val markerLocation1 = LatLng(56.837667, 60.603268)
        val markerData1 = MarkerData(R.drawable.plotinka,"Плотинка", "12 минут", "600 м", "32", "857",
            "Плотина, расположенная на реке Исеть в Историческом сквере Екатеринбурга. Построена в 1723 году, впоследствии многократно перестраивалась. Жители города называют её «Плотинка». Традиционное место массовых народных гуляний и праздников." )

        addMarkerToMap(markerLocation1, markerData1)


        val markerLocation2 = LatLng(56.837973, 60.598388)
        val markerData2 = MarkerData(R.drawable.lenina, "Центр", "40 минут", "3,3 км", "174", "4330",
            "Проспект застраивался на протяжении всей истории города зданиями самых разных архитектурных стилей. С XVIII века на проспекте устроена центральная аллея (многократно перестраивалась и планировалась к ликвидации, но сохранилась до наших дней).\n" +
                    "\n" +
                    "Образовался при строительстве плотины городского пруда в 1723 году. ")
        addMarkerToMap(markerLocation2, markerData2)
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            enableUserLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation()
            }
        }
    }
    private fun createAndAddPolylineRoute(routePoints: List<LatLng>) {
        val polylineOptions = PolylineOptions().apply {
            addAll(routePoints)
            color(ContextCompat.getColor(requireContext(), R.color.black))
            width(10f)
        }

        mMap.addPolyline(polylineOptions)
    }

    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireActivity())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
            val route1 = listOf(
                LatLng(56.837667, 60.603268),
                LatLng(56.835492, 60.604097),
                LatLng(56.835477, 60.604778),
                LatLng(56.837640, 60.604057),
                LatLng(56.837640, 60.603311)
            )
            val route2 = listOf(
                LatLng(56.837973, 60.598388),
                LatLng(56.836432, 60.582169),
                LatLng(56.836033, 60.582277),
                LatLng(56.838663, 60.607983),
                LatLng(56.838968, 60.607833),
                LatLng(56.838064, 60.599057),
            )

            createAndAddPolylineRoute(route1)
            createAndAddPolylineRoute(route2)
        }
    }

    private fun addMarkerToMap(location: LatLng, markerData: MarkerData) {
        val markerOptions = MarkerOptions()
            .position(location)

        val marker = mMap.addMarker(markerOptions)
        if (marker != null) {
            marker.tag = markerData
        }
    }

    private fun openMarkerDetailsFragment(marker: Marker) {
        val markerData = marker.tag as MarkerData
        val markerPosition = marker.position
        val markerImageResId = markerData.iconResId
        val markerName = markerData.name
        val markerTime = markerData.time
        val markerDistance = markerData.Distance
        val markerCalories = markerData.Calories
        val markerStep = markerData.Step
        val markerInformation = markerData.Information

        val markerDetailsFragment =
            MarkerDetailsFragment.newInstance(markerPosition, markerImageResId, markerName, markerTime, markerDistance, markerCalories, markerStep, markerInformation )
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.container, markerDetailsFragment, "MarkerDetailsFragment")
            .addToBackStack("MarkerDetailsFragment")
            .commit()
    }
}
