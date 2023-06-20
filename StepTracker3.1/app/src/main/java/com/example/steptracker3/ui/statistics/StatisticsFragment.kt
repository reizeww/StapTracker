    package com.example.steptracker3.ui.statistics

    import android.os.Bundle
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Toast
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
    import com.example.steptracker3.R
    import com.example.steptracker3.databinding.FragmentStatisticsBinding
    import com.github.mikephil.charting.components.XAxis
    import com.github.mikephil.charting.components.YAxis
    import com.github.mikephil.charting.data.BarData
    import com.github.mikephil.charting.data.BarDataSet
    import com.github.mikephil.charting.data.BarEntry
    import com.github.mikephil.charting.data.Entry
    import com.github.mikephil.charting.formatter.ValueFormatter
    import com.github.mikephil.charting.listener.OnChartValueSelectedListener
    import com.github.mikephil.charting.highlight.Highlight
    import com.google.android.gms.auth.api.signin.GoogleSignIn
    import com.google.android.gms.fitness.Fitness
    import com.google.android.gms.fitness.data.DataType
    import com.google.android.gms.fitness.request.DataReadRequest
    import java.text.SimpleDateFormat
    import java.util.*
    import java.util.concurrent.TimeUnit
    import com.google.android.gms.fitness.data.Field


    class StatisticsFragment : Fragment(), OnChartValueSelectedListener {
        private lateinit var binding: FragmentStatisticsBinding
        private var days = 7


        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentStatisticsBinding.inflate(inflater, container, false)
            return binding.root
        }

        class DateAxisValueFormatter(private val dates: List<Date>) : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                if (index < 0 || index >= dates.size) {
                    return ""
                }

                return dateFormat.format(dates[index])
            }
        }

        private fun updateStepsInfo(stepsData: List<Int>, days: Int) {
            val totalSteps = stepsData.sum()
            val averageSteps = if (days != 0) totalSteps / days else 0

            binding.textViewTotalSteps.text = getString(R.string.total_steps_format, totalSteps)
            binding.textViewAverageSteps.text = getString(R.string.average_steps_format, averageSteps)
        }


        class StepsAndDateValueFormatter(private val days: Int) : ValueFormatter() {
            private val calendar = Calendar.getInstance()
            private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            override fun getBarLabel(barEntry: BarEntry): String {
                val steps = barEntry.y.toInt()
                calendar.add(Calendar.DAY_OF_YEAR, -days + barEntry.x.toInt() + 1)
                val date = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, days - barEntry.x.toInt() - 1)
                return "$steps шагов, $date"
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

            binding.button7Days.setOnClickListener { updateStatistics(7) }
            binding.button30Days.setOnClickListener { updateStatistics(30) }
            binding.button90Days.setOnClickListener { updateStatistics(90) }
            binding.button7Days.isChecked = true


            setupChart()
            updateStatistics(7)
        }

        override fun onValueSelected(e: Entry?, h: Highlight?) {
            val steps = e?.y?.toInt() ?: 0
            Toast.makeText(requireContext(), "Шаги: $steps", Toast.LENGTH_SHORT).show()
        }

        override fun onNothingSelected() {
        }

        private fun timestampToDateLabel(index: Int): String {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -days + index + 1)
            }
            val dateFormat = SimpleDateFormat("dd MMMM EEEE", Locale.getDefault())
            return dateFormat.format(calendar.time)
        }



        private fun updateStatistics(days: Int) {
            this.days = days
            readStepsData(days)
        }

        private fun setupChart() {
            val chart = binding.barChart

            chart.description.isEnabled = false
            chart.setTouchEnabled(true)
            chart.setDrawGridBackground(false)
            chart.setPinchZoom(false)
            chart.isHighlightFullBarEnabled = true
            chart.setOnChartValueSelectedListener(this)

            chart.axisLeft.isEnabled = true
            chart.axisRight.isEnabled = false
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.setDrawGridLines(false)

            val leftAxis: YAxis = chart.axisLeft
            leftAxis.setLabelCount(5, false)
            leftAxis.axisMinimum = 0f
            leftAxis.textColor = ContextCompat.getColor(requireContext(), R.color.white) //


            chart.legend.isEnabled = false
            chart.setOnChartValueSelectedListener(object :
                OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val barEntry = e as BarEntry
                    val steps = barEntry.y.toInt()
                    val date = timestampToDateLabel(barEntry.x.toInt())
                    Toast.makeText(requireContext(), "$steps шагов, $date", Toast.LENGTH_SHORT).show()
                }
                override fun onNothingSelected() {}
            })
        }

        private fun updateChartData(stepsData: List<Int>, dates: List<Date>) {
            val chart = binding.barChart

            val entries = mutableListOf<BarEntry>()
            stepsData.forEachIndexed { index, steps ->
                entries.add(BarEntry(index.toFloat(), steps.toFloat()))
            }

            val barDataSet = BarDataSet(entries, "Steps")
            barDataSet.color = ContextCompat.getColor(requireContext(), R.color.pink)
            barDataSet.setDrawValues(false)
            barDataSet.valueFormatter = StepsAndDateValueFormatter(stepsData.size)

            val barData = BarData(barDataSet)
            chart.data = barData
            chart.invalidate()


            val xAxis = chart.xAxis
            xAxis.valueFormatter = DateAxisValueFormatter(dates)
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.white)

            updateStepsInfo(stepsData, days)


        }
        private fun readStepsData(days: Int) {
            val endTime = Calendar.getInstance().apply {

                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val startTime = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -days + 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.timeInMillis, endTime.timeInMillis, TimeUnit.MILLISECONDS)
                .build()

            val account = GoogleSignIn.getLastSignedInAccount(requireContext())

            if (account == null) {
                Log.e("StatisticsFragment", "Google account not found")
                return
            }
            Fitness.getHistoryClient(requireContext(), account)
                .readData(readRequest)
                .addOnSuccessListener { response ->
                    val stepsData = mutableListOf<Int>()
                    val dates = mutableListOf<Date>()

                    for (bucket in response.buckets) {
                        var steps = 0
                        val dataSets = bucket.dataSets

                        for (dataSet in dataSets) {
                            for (dataPoint in dataSet.dataPoints) {
                                steps += dataPoint.getValue(Field.FIELD_STEPS).asInt()

                            }
                        }

                        stepsData.add(steps)
                        dates.add(Date(bucket.getStartTime(TimeUnit.MILLISECONDS)))
                    }

                    updateChartData(stepsData, dates)
                }
                .addOnFailureListener { exception ->
                    Log.e("StatisticsFragment", "Unable to read steps data", exception)
                    Toast.makeText(requireContext(), "Не удалось прочитать данные о шагах", Toast.LENGTH_SHORT).show()
                }
        }


    }



