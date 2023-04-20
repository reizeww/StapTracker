package com.example.steptracker3.ui.step

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.fitness.data.Field
import com.example.steptracker3.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

class StepFragment : Fragment() {

    private lateinit var stepCountText: TextView

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()
    }

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
        private const val UPDATE_INTERVAL = 5000L // 5 seconds
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_step, container, false)
        stepCountText = view.findViewById(R.id.stepCount)

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            startStepsUpdates()
        }

        return view
    }

    private fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startStepsUpdates()
            } else {
                stepCountText.text = "Не удалось получить разрешение"
            }
        }
    }

    private fun getStepsCount() {
        val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)

        Fitness.getHistoryClient(requireContext(), account)
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dailyTotal ->
                val steps = dailyTotal.dataPoints.firstOrNull()?.getValue(Field.FIELD_STEPS)?.asInt() ?: 0
                stepCountText.text = "Количество шагов: $steps"
            }
            .addOnFailureListener {
                stepCountText.text = "Ошибка чтения данных"
            }
    }

    private var handler: Handler? = null
    private val stepsUpdateRunnable = object : Runnable {
        override fun run() {
            getStepsCount()
            handler?.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    private fun startStepsUpdates() {
        handler = Handler(Looper.getMainLooper())
        handler?.post(stepsUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacks(stepsUpdateRunnable)
        handler = null
    }
}
