package com.uynaity.ezcc2

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL

@SuppressLint("UseSwitchCompatOrMaterialCode")
class MainActivity : AppCompatActivity() {
    private lateinit var courseCodeEditText: EditText
    private lateinit var refreshButton: Button
    private lateinit var switch1: Switch
    private lateinit var switch2: Switch
    private lateinit var switch3: Switch
    private lateinit var sem1Stat: TextView
    private lateinit var sem1Data: TextView
    private lateinit var sem2Stat: TextView
    private lateinit var sem2Data: TextView
    private lateinit var listView1: RecyclerView
    private lateinit var listView2: RecyclerView
    private lateinit var adapter1: ListAdapter
    private lateinit var adapter2: ListAdapter
    private var backPressedTime: Long = 0
    private val backPressedInterval = 2000
    private var isSorted = false
    private var isExcept = false
    private var isAvailable = false

    private var caches = mutableListOf(
        mutableListOf<String>(), mutableListOf()
    )
    private var cachesSorted = mutableListOf(
        mutableListOf<String>(), mutableListOf()
    )
    private var stats = mutableListOf(
        "", ""
    )
    private var semList = mutableListOf(
        mutableListOf<Map<String, Any>>(), mutableListOf()
    )
    private var sortedSemList = mutableListOf(
        mutableListOf<Map<String, Any>>(), mutableListOf()
    )

    object VibrationUtil {
        fun vibrate(context: Context, milliseconds: Long) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                val vibrationEffect =
                    VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        courseCodeEditText = findViewById(R.id.courseCode)
        refreshButton = findViewById(R.id.refresh)
        switch1 = findViewById(R.id.switch1)
        switch2 = findViewById(R.id.switch2)
        switch3 = findViewById(R.id.switch3)
        sem1Stat = findViewById(R.id.sem1Stat)
        sem1Data = findViewById(R.id.Sem1Data)
        sem2Stat = findViewById(R.id.Sem2Stat)
        sem2Data = findViewById(R.id.Sem2Data)
        listView1 = findViewById(R.id.listView1)
        listView2 = findViewById(R.id.listView2)
        adapter1 = ListAdapter()
        adapter2 = ListAdapter()


        val spinner = findViewById<Spinner>(R.id.spinner)
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.spinner_options, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        Toast.makeText(this, "正在更新数据库", Toast.LENGTH_SHORT).show()
        updateTableData()

        courseCodeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                courseCodeEditText.requestFocus()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                courseCodeEditText.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
                judgement()
                printSem()
            }
        })

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                while (semList[0].isEmpty() || semList[1].isEmpty()) {
                    Thread.sleep(100)
                }
                judgement()
                printSem()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        refreshButton.setOnClickListener {
            updateTableData()
            judgement()
            printSem()
            VibrationUtil.vibrate(this, 50)
            while (semList[0].isEmpty() || semList[1].isEmpty()) {
                Thread.sleep(100)
            }
            Toast.makeText(this, "刷新成功", Toast.LENGTH_SHORT).show()
        }

        switch1.setOnCheckedChangeListener { _, isChecked ->
            isSorted = isChecked
            VibrationUtil.vibrate(this, 50)
            if (semList[0].isNotEmpty() && semList[1].isNotEmpty()) {
                printSem()
            }
        }

        switch2.setOnCheckedChangeListener { _, isChecked ->
            isExcept = isChecked
            VibrationUtil.vibrate(this, 50)
            judgement()
            printSem()
        }

        switch3.setOnCheckedChangeListener { _, isChecked ->
            isAvailable = isChecked
            VibrationUtil.vibrate(this, 50)
            judgement()
            printSem()
        }

        adapter1.setOnItemClickListener(object : ListAdapter.OnItemClickListener {
            override fun onItemClick(item: String) {
                Toast.makeText(this@MainActivity, "功能开发中...", Toast.LENGTH_SHORT).show()
            }
        })

        adapter2.setOnItemClickListener(object : ListAdapter.OnItemClickListener {
            override fun onItemClick(item: String) {
                Toast.makeText(this@MainActivity, "功能开发中...", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (System.currentTimeMillis() - backPressedTime < backPressedInterval) {
            super.onBackPressed()
            finishAffinity()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(this, "再次点击返回键退出", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTableData() {
        Thread {
            try {
                semList = getTableData()
                runOnUiThread {
                    Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "更新失败，请检查网络链接，程序退出", Toast.LENGTH_LONG)
                        .show()
                }
                finish()
            }

            sortedSemList = semList.map { sem ->
                sem.sortedByDescending { it["空余数量"] as Int }.toMutableList()
            }.toMutableList()
        }.start()
    }

    private fun getTableData(): MutableList<MutableList<Map<String, Any>>> {
        val url = URL("https://sweb.hku.hk/ccacad/ccc_appl/enrol_stat.html")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val responseCode = conn.responseCode
        if (responseCode != 200) {
            runOnUiThread {
                Toast.makeText(this, responseCode.toString(), Toast.LENGTH_SHORT).show()
            }
            return mutableListOf()
        }

        val doc = Jsoup.parse(url, 3000)
        val table = doc.select("table").first()
        val semList = mutableListOf(
            mutableListOf<Map<String, Any>>(), mutableListOf()
        )
        var sem = 0
        val tds = table?.select("tr")!!

        tds.removeAt(0)

        for (row in tds) {
            val td = row?.select("td")

            if (td?.size!! < 6) {
                if (row.text() == "First Semester") {
                    sem = 0
                } else if (row.text() == "Second Semester") {
                    sem = 1
                }
                continue
            }

            val data1 = td[0].text()
            val data2 = td[4].text().toInt()
            val data3 = td[5].text().toInt()
            val data4 = td[2].text()
            var stat = ""

            if (data2 > data3) {
                stat = "尚有余，可选"
            } else if (data3 - data2 <= 3) {
                stat = "少量不足，可备选"
            } else if (data3 - data2 > 3) {
                stat = "严重不足，建议更换"
            }
            val dataSet = mapOf(
                "课程代码" to data1,
                "班级代码" to data4,
                "空余数量" to data2,
                "等待批准" to data3,
                "选课建议" to stat
            )
            semList[sem].add(dataSet)
        }
        return semList
    }


    @SuppressLint("SetTextI18n")
    private fun judgement() {
        for (index in semList.indices) {
            val subList = semList[index]
            caches[index] = formatSem(subList)
        }

        for (index in sortedSemList.indices) {
            val subList = sortedSemList[index]
            cachesSorted[index] = formatSem(subList)
        }
    }

    private fun formatCourse(course: Map<String, Any>): String {
        var str = ""
        for (i in course) {
            str += if (i.key == "选课建议") {
                "${i.key}: ${i.value}"
            } else {
                "${i.key}: ${i.value}\n"
            }
        }
        return str
    }

    private fun formatSem(sem: MutableList<Map<String, Any>>): MutableList<String> {
        val courseList = mutableListOf<String>()
        for (i in sem) {
            val spinner = findViewById<Spinner>(R.id.spinner)
            val selectedValue = spinner.selectedItem.toString()
            val c = courseCodeEditText.text.toString()
            val mergeC = "CC${selectedValue}${c}"
            if (!i["课程代码"].toString().contains(mergeC)) {
                continue
            }
            if (isExcept) {
                if (i["班级代码"].toString().contains("X")) {
                    continue
                }
            }
            if (isAvailable) {
                if (i["空余数量"] == 0) {
                    continue
                }
            }
            courseList.add(formatCourse(i))
        }
        return courseList
    }

    private fun printSem() {
        runOnUiThread {
            for (index in stats.indices) {
                if (caches[index].isEmpty()) {
                    stats[index] = "未在Sem ${index + 1}中找到相关课程 :-("
                } else {
                    stats[index] = "Sem ${index + 1}中找到以下课程:"
                }
            }
            sem1Stat.text = stats[0]
            sem2Stat.text = stats[1]

            if (isSorted) {
                adapter1.setDataList(cachesSorted[0])
                this.listView1.layoutManager = LinearLayoutManager(this)
                this.listView1.adapter = adapter1

                adapter2.setDataList(cachesSorted[1])
                listView2.layoutManager = LinearLayoutManager(this)
                listView2.adapter = adapter2

            } else {
                adapter1.setDataList(caches[0])
                listView1.layoutManager = LinearLayoutManager(this)
                listView1.adapter = adapter1

                adapter2.setDataList(caches[1])
                listView2.layoutManager = LinearLayoutManager(this)
                listView2.adapter = adapter2
            }
        }
    }
}
