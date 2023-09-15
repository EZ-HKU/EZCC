package com.uynaity.ezcc2

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {
    private lateinit var courseCodeEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var sem1Stat: TextView
    private lateinit var sem1Data: TextView
    private lateinit var sem2Stat: TextView
    private lateinit var sem2Data: TextView
    private var backPressedTime: Long = 0
    private val backPressedInterval = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        courseCodeEditText = findViewById(R.id.courseCode)
        searchButton = findViewById(R.id.search)
        sem1Stat = findViewById(R.id.sem1Stat)
        sem1Data = findViewById(R.id.Sem1Data)
        sem2Stat = findViewById(R.id.Sem2Stat)
        sem2Data = findViewById(R.id.Sem2Data)
        searchButton.setOnClickListener {
            Thread {
                try {
                    getTableData(courseCodeEditText.text.toString())
                } catch (e: Exception) {
                    sem1Stat.text = e.message
                }
            }.start()
        }
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

    private fun getTableData(c: String?): MutableList<MutableList<Map<String, Any>>> {
        if (c == "") {
            runOnUiThread {
                Toast.makeText(this, "请输入课程代码", Toast.LENGTH_SHORT).show()
            }
            return mutableListOf()
        }
        val upC = c?.uppercase()
        val url = URL("https://sweb.hku.hk/ccacad/ccc_appl/enrol_stat.html")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val responseCode = conn.responseCode
        if (responseCode != 200) {
            runOnUiThread {
                sem1Stat.text = responseCode.toString()
            }
            return mutableListOf()
        }

        val doc = Jsoup.parse(url, 3000)
        val table = doc.select("table").first()

        val semList = mutableListOf(
            mutableListOf<Map<String, Any>>(), mutableListOf()
        )

        var sem = 0

        for (row in table?.select("tr")!!) {
            val tds = row?.select("td")

            if (tds?.size!! < 6) {
                if (row.text() == "First Semester") {
                    sem = 0
                } else if (row.text() == "Second Semester") {
                    sem = 1
                }
                continue
            }

            if (!tds[0].text().contains(upC!!)) {
                continue
            }

            if (!tds[2].text().contains("X")) {
                val data1 = tds[0].text()
                val data2 = tds[4].text().toInt()
                val data3 = tds[5].text().toInt()
                var stat = ""

                if (data2 > data3) {
                    stat = "尚有余，可选\n"
                } else if (data3 - data2 <= 3) {
                    stat = "少量不足，可备选\n"
                } else if (data3 - data2 > 3) {
                    stat = "严重不足，建议更换\n"
                }
                val dataSet = mapOf(
                    "课程代码" to data1,
                    "空余数量" to data2,
                    "等待批准" to data3,
                    "选课建议" to stat
                )
                semList[sem].add(dataSet)
            }
        }
        judgement(semList)
        return semList
    }


    @SuppressLint("SetTextI18n")
    private fun judgement(re: MutableList<MutableList<Map<String, Any>>>) {
        val semList = mutableListOf(
            sem1Data, sem2Data
        )
        val semStats = mutableListOf(
            sem1Stat, sem2Stat
        )
        for (index in re.indices) {
            val subList = re[index]
            if (subList.isEmpty()) {
                runOnUiThread {
                    semStats[index].text = "未在Sem ${index + 1}中找到相关课程 :-("
                }
            } else {
                runOnUiThread {
                    semStats[index].text = "Sem ${index + 1}中找到以下相关课程:"
                }
            }
            runOnUiThread {
                semList[index].text = printSem(subList)
            }
        }
    }

    private fun printSem(sem: MutableList<Map<String, Any>>): String {
        var str = ""
        for (i in sem) {
            for (j in i) {
                str += "${j.key}: ${j.value}\n"
            }
        }
        str += "\n"
        return str
    }

}
