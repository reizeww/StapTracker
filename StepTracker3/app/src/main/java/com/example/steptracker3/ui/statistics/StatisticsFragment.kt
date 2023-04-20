package com.example.steptracker3.ui.statistics

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.steptracker3.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import java.util.Calendar
import java.util.concurrent.TimeUnit

class StatisticsFragment : Fragment() {

    private lateinit var dayStepsText: TextView
    private lateinit var weekStepsText: TextView
    private lateinit var monthStepsText: TextView

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()
    }

    companion object {
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        dayStepsText = view.findViewById(R.id.daySteps)
        weekStepsText = view.findViewById(R.id.weekSteps)
        monthStepsText = view.findViewById(R.id.monthSteps)

        if (!hasPermissions()) {
            requestPermissions()
        } else {
            getStepsStatistics()
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
                getStepsStatistics()
            } else {
                dayStepsText.text = "Не удалось получить разрешение"
                weekStepsText.text = "Не удалось получить разрешение"
                monthStepsText.text = "Не удалось получить разрешение"
            }
        }
    }

    private fun getStepsStatistics() {
        val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)

        val endTime = Calendar.getInstance()
        val dayStartTime = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val weekStartTime = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }
        val monthStartTime = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }

        val dayStepsRequest = createStepsRequest(dayStartTime, endTime)
        val weekStepsRequest = createStepsRequest(weekStartTime, endTime)
        val monthStepsRequest = createStepsRequest(monthStartTime, endTime)

        val historyClient = Fitness.getHistoryClient(requireContext(), account)

        historyClient.readData(dayStepsRequest)
            .addOnSuccessListener { response ->
                val steps = extractTotalSteps(response)
                dayStepsText.text = "Шагов за день: $steps"
            }
            .addOnFailureListener {
                dayStepsText.text = "Ошибка чтения данных"
            }

        historyClient.readData(weekStepsRequest)
            .addOnSuccessListener { response ->
                val steps = extractTotalSteps(response)
                weekStepsText.text = "Шагов за неделю: $steps"
            }
            .addOnFailureListener {
                weekStepsText.text = "Ошибка чтения данных"
            }

        historyClient.readData(monthStepsRequest)
            .addOnSuccessListener { response ->
                val steps = extractTotalSteps(response)
                monthStepsText.text = "Шагов за месяц: $steps"
            }
            .addOnFailureListener {
                monthStepsText.text = "Ошибка чтения данных"
            }
    }

    private fun createStepsRequest(startTime: Calendar, endTime: Calendar): DataReadRequest {
        return DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime.timeInMillis, endTime.timeInMillis, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun extractTotalSteps(response: DataReadResponse): Int {
        var totalSteps = 0
        for (bucket in response.buckets) {
            val dataSets = bucket.dataSets
            for (dataSet in dataSets) {
                for (dataPoint in dataSet.dataPoints) {
                    for (field in dataPoint.dataType.fields) {
                        if (field == Field.FIELD_STEPS) {
                            totalSteps += dataPoint.getValue(field).asInt()
                        }
                    }
                }
            }
        }
        return totalSteps
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val updateStatisticsButton: Button = view.findViewById(R.id.updateStatisticsButton)
        updateStatisticsButton.setOnClickListener {
            getStepsStatistics()
        }
    }


}


// Получите статистику
