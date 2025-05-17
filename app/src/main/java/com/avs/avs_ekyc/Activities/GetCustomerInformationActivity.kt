package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.avs.avs_ekyc.Activities.GetCustomerInformationActivity
import com.avs.avs_ekyc.Activities.MainActivity
import com.avs.avs_ekyc.Activities.ShowPendingListActivity
import com.avs.avs_ekyc.Adapter.PendingListAdapter
import com.avs.avs_ekyc.Constant.AESCryptoUtil
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.Constant.CustomProgressDialog
import com.avs.avs_ekyc.Model.PendingCustomer
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityGetCustomerInformationBinding
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GetCustomerInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetCustomerInformationBinding

    private lateinit var progressDialog: CustomProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = CustomProgressDialog(this@GetCustomerInformationActivity)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Get Customer Information"


        binding.submit.setOnClickListener {
            val custNo = binding.edtCustomerNo.text.toString()

            if (custNo.isEmpty()) {
                binding.edtCustomerNo.error = "Please Enter Customer No"
            } else {
                fetchEncryptedValue(custNo)
            }
        }

    }

    fun fetchEncryptedValue(custNo: String) {
        Thread {

            runOnUiThread {
                progressDialog.show()
            }

            val params = "CUSTNO=$custNo"
            val encryptedData = AESCryptoUtil.encrypt(params)
            val urlString =
                "https://ckyc.tbsbl.com/TBSBCKYC_APP/frmkycdetails.aspx?data=$encryptedData"

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
                    getCustomerData(encryptedValue,custNo)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HTTP_ERROR", "Error fetching data: ${e.message}")
            }
        }.start()
    }

    private fun getCustomerData(encryptedValue: String , custNo : String) {
        val decryptedResponse = AESCryptoUtil.decrypt((encryptedValue ?: ""))

        Log.d("EncryptedRes", (encryptedValue ?: "null"))
        Log.d("DecryptedRes", decryptedResponse ?: "null")

        if (!decryptedResponse.isNullOrEmpty()) {
            try {
                if (decryptedResponse.trim().startsWith("[")) {
                    try {
                        val jsonArray = JSONArray(decryptedResponse)
                        if (jsonArray.length() > 0) {
                            val jsonObject = jsonArray.getJSONObject(0) // only one object
                            val status = jsonObject.getString("Status")
                            if (status.equals("Success", ignoreCase = true)) {

                                val bundle = Bundle()
                                bundle.putString("name", jsonObject.getString("NAME"))
                                bundle.putString("dob", jsonObject.getString("DOB"))
                                bundle.putString("mobile", jsonObject.getString("MOBNO"))
                                bundle.putString("uid", jsonObject.getString("UID"))
                                bundle.putString("house", jsonObject.getString("HOUSE"))
                                bundle.putString("loc", jsonObject.getString("LOC"))
                                bundle.putString("vtc", jsonObject.getString("VTC"))
                                bundle.putString("district", jsonObject.getString("District"))
                                bundle.putString("sub_district", jsonObject.getString("City"))
                                bundle.putString("state", jsonObject.getString("StateCode"))
                                bundle.putString("pincode", jsonObject.getString("Pin"))
                                bundle.putString("stateCode", jsonObject.getString("StateCode"))
                                bundle.putString("fatherName", jsonObject.getString("FatherName"))
                                bundle.putString("gender", jsonObject.getString("GENDER"))
                                bundle.putString(
                                    "dateOfApplication",
                                    jsonObject.getString("DateOfApplication")
                                )
                                bundle.putString("pan", jsonObject.getString("PAN"))
                                bundle.putString("address1", jsonObject.getString("RELADDLINE1"))
                                bundle.putString("address2", jsonObject.getString("RELADDLINE2"))
                                bundle.putString("address3", jsonObject.getString("RELADDLINE3"))
                                bundle.putString("relDistrict", jsonObject.getString("RELDistrict"))
                                bundle.putString("relCity", jsonObject.getString("RELCity"))
                                bundle.putString("relPin", jsonObject.getString("RELPin"))
                                bundle.putString(
                                    "relStateCode",
                                    jsonObject.getString("RELStateCode")
                                )
                                bundle.putString("type", jsonObject.getString("Type"))
                                bundle.putString("regCerti", jsonObject.getString("RegiCerti"))
                                bundle.putString("certiIncome", jsonObject.getString("Certi_Inco"))
                                bundle.putString("cust_no", custNo)


                                startActivity(
                                    Intent(
                                        this@GetCustomerInformationActivity,
                                        CustomerDataActivity::class.java
                                    )
                                        .putExtra("bundle", bundle)
                                )

                            } else {
                                Constant.error(
                                    this@GetCustomerInformationActivity,
                                    "Status is not success"
                                )
                            }
                        } else {
                            Constant.error(
                                this@GetCustomerInformationActivity,
                                "No data found"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("JSON Error", e.message ?: "Parsing error")
                        Constant.error(
                            this@GetCustomerInformationActivity,
                            "Parsing error"
                        )
                    }
                }
            } catch (e: Exception) {
                Constant.error(this@GetCustomerInformationActivity, "Something went wrong")
                Log.e("Exception", e.message.toString())
            }
        }
    }
}