package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.avs.avs_ekyc.Activities.MainActivity
import com.avs.avs_ekyc.Adapter.PendingListAdapter
import com.avs.avs_ekyc.Constant.AESCryptoUtil
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.Constant.CustomProgressDialog
import com.avs.avs_ekyc.Constant.SharedPreferenceManager
import com.avs.avs_ekyc.Model.PendingCustomer
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityShowPendingListBinding
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import okio.IOException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ShowPendingListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowPendingListBinding
    private lateinit var progressDialog: CustomProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowPendingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = CustomProgressDialog(this@ShowPendingListActivity)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Pending List"

        binding.recyclerView.layoutManager = LinearLayoutManager(this@ShowPendingListActivity)

        fetchEncryptedValue(11, 10603)

        binding.addKyc.setOnClickListener {
            startActivity(
                Intent(
                    this@ShowPendingListActivity, GetCustomerInformationActivity::class.java
                )
            )
        }
    }

    fun fetchEncryptedValue(brcd: Int, prdCode: Int) {
        Thread {

            runOnUiThread {
                progressDialog.show()
            }
            val loginParams = "BRCD=$brcd&PRDCode=$prdCode"
            val encryptedData = AESCryptoUtil.encrypt(loginParams)
            val urlString =
                "https://ckyc.tbsbl.com/TBSBCKYC_APP/FrmPending_List.aspx?data=$encryptedData"

            Log.d("URL", urlString)

            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val html = response.toString()

                // Extract JSON string from HTML <p> tag
                val start = html.indexOf("{")
                val end = html.lastIndexOf("}") + 1
                val encodedJson = html.substring(start, end)

                // Decode HTML entities like &quot; => "
                val decodedJson = Html.fromHtml(encodedJson, Html.FROM_HTML_MODE_LEGACY).toString()

                // Parse JSON
                val jsonObject = JSONObject(decodedJson)
                val encryptedValue = jsonObject.getString("encrypted")

                // Use result on UI thread
                runOnUiThread {
                    progressDialog.dismiss()
                    getPendingList(encryptedValue)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HTTP_ERROR", "Error fetching data: ${e.message}")
            }
        }.start()
    }


    private fun getPendingList(encryptedValue: String) {

        val decryptedResponse = AESCryptoUtil.decrypt((encryptedValue ?: ""))

        Log.d("EncryptedRes", (encryptedValue ?: "null"))
        Log.d("DecryptedRes", decryptedResponse ?: "null")

        if (!decryptedResponse.isNullOrEmpty()) {
            try {
                if (decryptedResponse.trim().startsWith("{")) {
                    val jsonObject = JSONObject(decryptedResponse)
                    val status = jsonObject.getString("message")

                    if (status.equals("Success", ignoreCase = true)) {
                        val dataArray = jsonObject.getJSONArray("Data")
                        val customerList = mutableListOf<PendingCustomer>()
                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val customer = PendingCustomer(
                                CustNo = item.getString("CustNo"),
                                CustName = item.getString("CustName"),
                                AccNo = item.getString("AccNo")
                            )
                            customerList.add(customer)
                        }
                        val adapter = PendingListAdapter(customerList)
                        binding.recyclerView.adapter = adapter
                    } else {
                        Constant.error(this@ShowPendingListActivity, status)
                    }
                } else {
                    Constant.error(this@ShowPendingListActivity, decryptedResponse)
                    Log.e("LoginError", "Decrypted response is not JSON")
                }
            } catch (e: Exception) {
                Log.e("JSON Error", e.message ?: "Parsing error")
                Constant.error(this@ShowPendingListActivity, "Parsing error")
            }
        } else {
            Constant.error(this@ShowPendingListActivity, "Empty response from server")
        }
    }
}