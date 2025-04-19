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
import com.avs.avs_ekyc.Constant.SharedPreferenceManager
import com.avs.avs_ekyc.Model.PendingCustomer
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.Model.UpdateDetailsModel
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityCustomerDataBinding
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import okio.IOException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerDataActivity : AppCompatActivity() {

    private lateinit var binding : ActivityCustomerDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Customer Data"

        val bundle = intent.getBundleExtra("bundle")
        val customerNo = bundle?.getString("cust_no")
        val customerName = bundle?.getString("name")
        val dob = bundle?.getString("dob")
        val uid = bundle?.getString("uid")
        val mob = bundle?.getString("mob")
        val house = bundle?.getString("house")
        val loc = bundle?.getString("loc")
        val vtc = bundle?.getString("vtc")
        val district = bundle?.getString("district")
        val subDistrict = bundle?.getString("sub_district")
        val state = bundle?.getString("state")
        val pincode = bundle?.getString("pincode")
        val type = bundle?.getString("type")

       when(type)
       {
           "1" -> binding.type.setText("Individual")
           "2" -> binding.type.setText("Minor")
           "3" -> binding.type.setText("Legal")
       }

        binding.custName.setText(customerName)
        binding.dob.setText(dob)
        binding.uid.setText(uid)
        binding.mob.setText(mob)
        binding.house.setText(house)
        binding.loc.setText(loc)
        binding.vtc.setText(vtc)
        binding.district.setText(district)
        binding.subDistrict.setText(subDistrict)
        binding.state.setText(state)
        binding.pincode.setText(pincode)

        binding.next.setOnClickListener {

            val name = binding.custName.text.toString()
            val dob = binding.dob.text.toString()
            val uid = binding.uid.text.toString()
            val mob = binding.mob.text.toString()
            val house = binding.house.text.toString()
            val loc = binding.loc.text.toString()
            val vtc = binding.vtc.text.toString()
            val district = binding.district.text.toString()
            val subDistrict = binding.subDistrict.text.toString()
            val state = binding.state.text.toString()
            val pincode = binding.pincode.text.toString()

            callupdateDetails(type,name,dob,uid,mob,house,loc,vtc,district,subDistrict,state,pincode,
                customerNo.toString()
            )
        }
    }

    private fun callupdateDetails(type: String?, name: String, dob: String, uid: String, mob: String, house: String, loc: String, vtc: String, district: String, subDistrict: String, state: String, pincode: String , customerNo : String) {
        val progress = CustomProgressDialog(this)
        progress.show()

        val agentNo = SharedPreferenceManager.getString(SharedPreferenceManager.AGENT_NO)

        val model = UpdateDetailsModel(customerNo,name,dob,mob,"",uid,house,loc,vtc,district,subDistrict,state,pincode,"","",agentNo,"","","")
        val encryptedData = AESCryptoUtil.encrypt(model.toString())

        try {
            RetrofitInstance.getInstance().updateDetails(encryptedData)
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

                                        if (status.equals("Success", ignoreCase = true)) {

                                            if (type == "1")
                                            {
                                                startActivity(Intent(this@CustomerDataActivity, IndivdualPhotoActivity::class.java)
                                                    .putExtra("type",type)
                                                    .putExtra("customerNo",customerNo)
                                                )
                                            }
                                            if (type == "2")
                                            {
                                                startActivity(Intent(this@CustomerDataActivity, IndivdualPhotoActivity::class.java)
                                                    .putExtra("type",type)
                                                    .putExtra("customerNo",customerNo)
                                                )
                                            }
                                            if (type == "3")
                                            {
                                                startActivity(Intent(this@CustomerDataActivity, RelatedPersonImageActivity::class.java)
                                                    .putExtra("type",type)
                                                    .putExtra("customerNo",customerNo)
                                                )
                                            }

                                        } else {
                                            Constant.error(this@CustomerDataActivity, "Success failed")
                                        }
                                    } else {
                                        Constant.error(this@CustomerDataActivity, decryptedResponse)
                                        Log.e("LoginError", "Decrypted response is not JSON")
                                    }
                                } catch (e: Exception) {
                                    Log.e("JSON Error", e.message ?: "Parsing error")
                                    Constant.error(this@CustomerDataActivity, "Parsing error")
                                }
                            } else {
                                Constant.error(this@CustomerDataActivity, "Empty response from server")
                            }
                        } else {
                            Constant.error(this@CustomerDataActivity, "Server returned error")
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        if (t is IOException) {
                            Constant.error(this@CustomerDataActivity, "Network issue. Check connection.")
                        } else {
                            Constant.error(this@CustomerDataActivity, "API error: ${t.localizedMessage}")
                        }
                        Log.e("LoginError", t.message ?: "Unknown error")
                    }
                })
        } catch (e: Exception) {
            progress.dismiss()
            Log.e("LoginException", e.message ?: "Exception")
            Constant.error(this@CustomerDataActivity, "Unexpected error occurred")
        }
    }
}