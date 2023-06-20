    package com.example.steptracker3.ui.step

    import android.Manifest
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.graphics.Color
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup

    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.app.ActivityCompat
    import androidx.fragment.app.Fragment
    import com.google.android.gms.fitness.data.Field
    import com.example.steptracker3.R
    import com.google.android.gms.auth.api.signin.GoogleSignIn
    import com.google.android.gms.auth.api.signin.GoogleSignInAccount
    import com.google.android.gms.fitness.Fitness
    import com.google.android.gms.fitness.FitnessOptions
    import com.google.android.gms.fitness.data.DataType
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.ktx.Firebase
    import com.mikhaellopez.circularprogressbar.CircularProgressBar
    import java.text.SimpleDateFormat
    import java.util.*
    import kotlin.math.roundToInt
    private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1234

    class StepFragment : Fragment() {

        private lateinit var stepCountText: TextView
        private lateinit var progressBar: CircularProgressBar
        private lateinit var tvPercentage: TextView
        private lateinit var tvDate: TextView
        private lateinit var tvCalories: TextView
        private lateinit var tvDistance: TextView

        private val maxSteps = 10000
        private val userWeight: Double = 70.0 // Вес пользователя(заглушка)

        private lateinit var googleSignInAccount: GoogleSignInAccount
        private val fitnessOptions: FitnessOptions by lazy {
            FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)

                .build()
        }

        companion object {
            private const val REQUEST_PERMISSIONS_REQUEST_CODE = 1
            private const val UPDATE_INTERVAL = 5000L // 5 seconds
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState  : Bundle?
        ): View? {

            val view = inflater.inflate(R.layout.fragment_step, container, false)
            stepCountText = view.findViewById(R.id.stepCount)
            progressBar = view.findViewById(R.id.progress_bar)
            tvPercentage = view.findViewById(R.id.tv_percentage)
            tvDate = view.findViewById(R.id.tv_date)
            tvCalories = view.findViewById(R.id.tv_calories)
            tvDistance = view.findViewById(R.id.tv_distance)

            val currentDate = getCurrentDate()
            tvDate.text = currentDate

            if (!hasPermissions()) {
                requestPermissions()
            } else {
                startStepsUpdates()
            }
            googleSignInAccount = GoogleSignIn.getAccountForExtension(requireActivity(), fitnessOptions)

            if (!GoogleSignIn.hasPermissions(googleSignInAccount, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    googleSignInAccount,
                    fitnessOptions
                )
            } else {
                getStepsCount()
            }

            return view
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    getStepsCount()
                } else {
                    Toast.makeText(requireActivity(), "Не удалось получить разрешения", Toast.LENGTH_SHORT).show()
                }
            }
        }




        private fun updatePercentage(steps: Int) {
            val percentage = (steps.toFloat() / 10000 * 100).toInt()
            tvPercentage.text = "$percentage%"
        }
        private fun getCurrentDate(): String {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return dateFormat.format(Date())
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

        private fun calculateDistance(steps: Int): Int {
            val stepLength = 0.65
            val distanceInMeters = steps * stepLength
            return distanceInMeters.roundToInt()
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
        private fun getCalories() {
            val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)

            Fitness.getHistoryClient(requireContext(), account)
                .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
                .addOnSuccessListener { dailyTotal ->
                    val calories = dailyTotal.dataPoints.firstOrNull()?.getValue(Field.FIELD_CALORIES)?.asFloat() ?: 0f
                    tvCalories.text = " ${calories.roundToInt()}"
                }
                .addOnFailureListener {
                    tvCalories.text = "Ошибка чтения данных"
                }
        }


        private fun getStepsCount() {
            val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)

            Fitness.getHistoryClient(requireContext(), account)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener { dailyTotal ->
                    val steps = dailyTotal.dataPoints.firstOrNull()?.getValue(Field.FIELD_STEPS)?.asInt() ?: 0
                    stepCountText.text = "$steps /10000"
                    updatePercentage(steps)

                    val progressBar = view?.findViewById<CircularProgressBar>(R.id.progress_bar)
                    progressBar?.progress = steps.toFloat()
                    progressBar?.apply {
                        setBackgroundColor(Color.parseColor("#FF8043E3"))
                        progressBarColor = Color.parseColor("#FFE1599C")
                    }

                    val distance = calculateDistance(steps)
                    tvDistance.text = "$distance "

                }
                .addOnFailureListener {
                    stepCountText.text = "Ошибка чтения данных"
                }
        }



        private var handler: Handler? = null

        private val stepsUpdateRunnable = object : Runnable {
            override fun run() {
                getStepsCount()
                getCalories()

                handler?.postDelayed(this, UPDATE_INTERVAL)
            }
        }

        private fun startStepsUpdates() {
            handler = Handler(Looper.getMainLooper())
            handler?.post(stepsUpdateRunnable)
            getCalories()
        }

        override fun onDestroy() {
            super.onDestroy()
            handler?.removeCallbacks(stepsUpdateRunnable)
            handler = null
        }
    }
