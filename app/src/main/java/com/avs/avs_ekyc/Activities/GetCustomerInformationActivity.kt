package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

class GetCustomerInformationActivity : AppCompatActivity() {

    private lateinit var binding : ActivityGetCustomerInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Get Customer Information"


        binding.submit.setOnClickListener {
            val custNo = binding.edtCustomerNo.text.toString()

            if (custNo.isEmpty())
            {
                binding.edtCustomerNo.error = "Please Enter Customer No"
            }
            else
            {
                callCustomerInfo(custNo)
            }
        }

    }

    private fun callCustomerInfo(custNo : String) {
        val progress = CustomProgressDialog(this)
        progress.show()

        val params = "CUSTNO=$custNo"
        val encryptedData = AESCryptoUtil.encrypt(params)

        try {
            RetrofitInstance.getInstance().getKycDetails(encryptedData)
                .enqueue(object : Callback<UniversalResponseModel> {
                    override fun onResponse(
                        call: Call<UniversalResponseModel>,
                        response: Response<UniversalResponseModel>
                    ) {
                        progress.dismiss()

                        if (response.isSuccessful) {
                            val encryptedResponse = response.body()?.encrypted
                            val decryptedResponse = AESCryptoUtil.decrypt((encryptedResponse ?: ""))

                            Log.d("EncryptedRes", (encryptedResponse ?: "null"))
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
                                                    bundle.putString("name",jsonObject.getString("NAME"))
                                                    bundle.putString("dob",jsonObject.getString("DOB"))
                                                    bundle.putString("mobile",jsonObject.getString("MOBNO"))
                                                    bundle.putString("uid",jsonObject.getString("UID"))
                                                    bundle.putString("house",jsonObject.getString("HOUSE"))
                                                    bundle.putString("loc",jsonObject.getString("LOC"))
                                                    bundle.putString("vtc",jsonObject.getString("VTC"))
                                                    bundle.putString("district",jsonObject.getString("District"))
                                                    bundle.putString("sub_district",jsonObject.getString("City"))
                                                    bundle.putString("state",jsonObject.getString("StateCode"))
                                                    bundle.putString("pincode",jsonObject.getString("Pin"))
                                                    bundle.putString("stateCode",jsonObject.getString("StateCode"))
                                                    bundle.putString("fatherName",jsonObject.getString("FatherName"))
                                                    bundle.putString("gender",jsonObject.getString("GENDER"))
                                                    bundle.putString("dateOfApplication",jsonObject.getString("DateOfApplication"))
                                                    bundle.putString("pan",jsonObject.getString("PAN"))
                                                    bundle.putString("address1",jsonObject.getString("RELADDLINE1"))
                                                    bundle.putString("address2",jsonObject.getString("RELADDLINE2"))
                                                    bundle.putString("address3",jsonObject.getString("RELADDLINE3"))
                                                    bundle.putString("relDistrict",jsonObject.getString("RELDistrict"))
                                                    bundle.putString("relCity",jsonObject.getString("RELCity"))
                                                    bundle.putString("relPin",jsonObject.getString("RELPin"))
                                                    bundle.putString("relStateCode",jsonObject.getString("RELStateCode"))
                                                    bundle.putString("type",jsonObject.getString("Type"))
                                                    bundle.putString("regCerti",jsonObject.getString("RegiCerti"))
                                                    bundle.putString("certiIncome",jsonObject.getString("Certi_Inco"))
                                                    bundle.putString("cust_no",custNo)


                                                    startActivity(Intent(this@GetCustomerInformationActivity, CustomerDataActivity::class.java)
                                                        .putExtra("bundle",bundle)
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
                                    } else {
                                        Constant.error(
                                            this@GetCustomerInformationActivity,
                                            "Invalid response format"
                                        )
                                        Log.e("Error", "Decrypted response is not a JSONArray")
                                    }
                                }catch (e : Exception)
                                {
                                    Constant.error(this@GetCustomerInformationActivity, "Something went wrong")
                                    Log.e("Exception",e.message.toString())
                                }
                            }
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        if (t is IOException) {
                            Constant.error(this@GetCustomerInformationActivity, "Network issue. Check connection.")
                        } else {
                            Constant.error(this@GetCustomerInformationActivity, "API error: ${t.localizedMessage}")
                        }
                        Log.e("Error", t.message ?: "Unknown error")
                    }
                })
        } catch (e: Exception) {
            progress.dismiss()
            Log.e("Exception", e.message ?: "Exception")
            Constant.error(this@GetCustomerInformationActivity, "Unexpected error occurred")
        }
    }
}