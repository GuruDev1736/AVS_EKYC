package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.os.Bundle
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

class ShowPendingListActivity : AppCompatActivity() {

    private lateinit var binding : ActivityShowPendingListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowPendingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Pending List"


        binding.recyclerView.layoutManager = LinearLayoutManager(this@ShowPendingListActivity)

        callPendingList(11,10603)

        binding.addKyc.setOnClickListener {
            startActivity(Intent(this@ShowPendingListActivity, GetCustomerInformationActivity::class.java))
        }

    }

    private fun callPendingList(brcd: Int, prdCode: Int) {
        val progress = CustomProgressDialog(this)
        progress.show()

        val loginParams = "BRCD=$brcd&PRDCode=$prdCode"
        val encryptedData = AESCryptoUtil.encrypt(loginParams)

        try {
            RetrofitInstance.getInstance().getPendingList(encryptedData)
                .enqueue(object : Callback<UniversalResponseModel> {
                    override fun onResponse(call: Call<UniversalResponseModel>, response: Response<UniversalResponseModel>) {
                        progress.dismiss()

                        if (response.isSuccessful) {
                            val encryptedResponse = response.body()?.encrypted
                            val decryptedResponse = AESCryptoUtil.decrypt((encryptedResponse ?: ""))

                            Log.d("EncryptedRes", (encryptedResponse ?: "null"))
                            Log.d("DecryptedRes", decryptedResponse ?: "null")

                            if (!decryptedResponse.isNullOrEmpty()) {
                                try {
                                    if (decryptedResponse.trim().startsWith("{")) {
                                        val jsonObject = JSONObject(decryptedResponse)
                                        val status = jsonObject.getString("message")
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

                                        if (status.equals("Success", ignoreCase = true)) {
                                            val adapter = PendingListAdapter(customerList)
                                            binding.recyclerView.adapter = adapter
                                        } else {
                                            Constant.error(this@ShowPendingListActivity, "Success failed")
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
                        } else {
                            Constant.error(this@ShowPendingListActivity, "Server returned error")
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        if (t is IOException) {
                            Constant.error(this@ShowPendingListActivity, "Network issue. Check connection.")
                        } else {
                            Constant.error(this@ShowPendingListActivity, "API error: ${t.localizedMessage}")
                        }
                        Log.e("LoginError", t.message ?: "Unknown error")
                    }
                })
        } catch (e: Exception) {
            progress.dismiss()
            Log.e("LoginException", e.message ?: "Exception")
            Constant.error(this@ShowPendingListActivity, "Unexpected error occurred")
        }
    }
}