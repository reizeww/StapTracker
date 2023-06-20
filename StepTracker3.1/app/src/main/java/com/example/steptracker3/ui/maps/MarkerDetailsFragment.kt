package com.example.steptracker3.ui.maps

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.steptracker3.databinding.FragmentMarkerDetailsBinding
import com.google.android.gms.maps.model.LatLng




class MarkerDetailsFragment : Fragment() {


    private var _binding: FragmentMarkerDetailsBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(
            markerPosition: LatLng,
            markerImageResId: Int,
            markerName: String,
            markerTime: String,
            markerDistance: String,
            markerCalories: String,
            markerStep: String,
            markerInformation: String

        ): MarkerDetailsFragment {
            val fragment = MarkerDetailsFragment()
            val args = Bundle()
            args.putParcelable("markerPosition", markerPosition)
            args.putInt("markerImageResId", markerImageResId)

            args.putString("markerName", markerName)
            args.putString("markerTime", markerTime)
            args.putString("markerDistance", markerDistance)
            args.putString("markerCalories", markerCalories)
            args.putString("markerStep", markerStep)
            args.putString("markerInformation", markerInformation)


            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMarkerDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val markerPosition = arguments?.getParcelable<LatLng>("markerPosition")
        val markerImageResId = arguments?.getInt("markerImageResId")

        val markerName = arguments?.getString("markerName")
        val markerTime = arguments?.getString("markerTime")
        val markerDistance = arguments?.getString("markerDistance")
        val markerCalories = arguments?.getString("markerCalories")
        val markerStep = arguments?.getString("markerStep")
        val markerInformation = arguments?.getString("markerInformation")

        binding.markerImage.setImageResource(markerImageResId ?: 0)

        binding.markerName.text = markerName
        binding.markerTime.text = markerTime
        binding.markerDistance.text = markerDistance
        binding.markerCalories.text = markerCalories
        binding.markerStep.text = markerStep
        binding.markerInformation.text = markerInformation



        binding.buttonBuildRoute.setOnClickListener {
            markerPosition?.let { position ->
                openGoogleMapsForDirections(position)
            }
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }


    private fun openGoogleMapsForDirections(position: LatLng) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${position.latitude},${position.longitude}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }


}
