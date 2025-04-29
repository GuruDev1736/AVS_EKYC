package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.avs.avs_ekyc.Constant.AESCryptoUtil
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.Constant.CustomProgressDialog
import com.avs.avs_ekyc.Constant.SharedPreferenceManager
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.databinding.ActivityCustomerDataBinding
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.IOException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CustomerDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.actionBar.toolbar.title = "Customer Data"

        val bundle = intent.getBundleExtra("bundle")
        val name = bundle?.getString("name") ?: ""
        val dob = bundle?.getString("dob") ?: ""
        val mobile = bundle?.getString("mobile") ?: ""
        val uid = bundle?.getString("uid") ?: ""
        val house = bundle?.getString("house") ?: ""
        val loc = bundle?.getString("loc") ?: ""
        val vtc = bundle?.getString("vtc") ?: ""
        val district = bundle?.getString("district") ?: ""
        val subDistrict = bundle?.getString("sub_district") ?: ""
        val state = bundle?.getString("state") ?: ""
        val pincode = bundle?.getString("pincode") ?: ""
        val stateCode = bundle?.getString("stateCode") ?: ""
        val fatherName = bundle?.getString("fatherName") ?: ""
        val gender = bundle?.getString("gender") ?: ""
        val dateOfApplication = bundle?.getString("dateOfApplication") ?: ""
        val pan = bundle?.getString("pan") ?: ""
        val address1 = bundle?.getString("address1") ?: ""
        val address2 = bundle?.getString("address2") ?: ""
        val address3 = bundle?.getString("address3") ?: ""
        val relDistrict = bundle?.getString("relDistrict") ?: ""
        val relCity = bundle?.getString("relCity") ?: ""
        val relPin = bundle?.getString("relPin") ?: ""
        val relStateCode = bundle?.getString("relStateCode") ?: ""
        val type = bundle?.getString("type") ?: ""
        val regCerti = bundle?.getString("regCerti") ?: ""
        val certiIncome = bundle?.getString("certiIncome") ?: ""
        val custNo = bundle?.getString("cust_no") ?: ""


        when (type) {
            "1" -> binding.type.setText("Individual")
            "2" -> binding.type.setText("Minor")
            "3" -> binding.type.setText("Legal")
        }

        binding.custName.setText(name)
        binding.dob.setText(dob)
        binding.uid.setText(uid)
        binding.mob.setText(mobile)
        binding.house.setText(house)
        binding.loc.setText(loc)
        binding.vtc.setText(vtc)
        binding.district.setText(relDistrict)
        binding.subDistrict.setText(relCity)
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

            if (valid(
                    name,
                    dob,
                    uid,
                    mob,
                    house,
                    loc,
                    vtc,
                    district,
                    subDistrict,
                    state,
                    pincode,
                    type
                )
            ) {
                callupdateDetails(
                    type,
                    name,
                    dob,
                    uid,
                    mob,
                    house,
                    loc,
                    vtc,
                    district,
                    subDistrict,
                    state,
                    pincode,
                    custNo.toString(),
                    pan,
                    gender,
                    fatherName,
                    dateOfApplication,
                    regCerti,
                    certiIncome
                )
            }
        }
    }

    private fun valid(
        name: String,
        dob: String,
        uid: String,
        mob: String,
        house: String,
        loc: String,
        vtc: String,
        district: String,
        subDistrict: String,
        state: String,
        pincode: String,
        type: String?
    ): Boolean {
        if (name.isEmpty() || name.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter name")
            return false
        }
        if (dob.isEmpty() || dob.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter dob")
            return false
        }
        if (type != "3") {
            if (uid.isEmpty() || uid.isBlank()) {
                Constant.error(this@CustomerDataActivity, "Please enter uid")
                return false
            }
            if (uid.length < 12) {
                Constant.error(this@CustomerDataActivity, "UID is invalid")
                return false
            }
        }
        if (mob.isEmpty() || mob.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter Mobile No")
            return false
        }
        if (mob.length < 10) {
            Constant.error(this@CustomerDataActivity, "Mobile No is not valid")
            return false
        }
        if (house.isEmpty() || house.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter house")
            return false
        }
        if (loc.isEmpty() || loc.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter loc")
            return false
        }
        if (vtc.isEmpty() || vtc.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter vtc")
            return false
        }
        if (district.isEmpty() || district.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter district")
            return false
        }
        if (subDistrict.isEmpty() || subDistrict.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter subDistrict")
            return false
        }
        if (state.isEmpty() || state.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter state")
            return false
        }
        if (pincode.isEmpty() || pincode.isBlank()) {
            Constant.error(this@CustomerDataActivity, "Please enter pincode")
            return false
        }
        return true
    }

    fun String.toPlainRequestBody(): RequestBody {
        return RequestBody.create("text/plain".toMediaTypeOrNull(), this)
    }

    private fun callupdateDetails(
        type: String?,
        name: String,
        dob: String,
        uid: String,
        mob: String,
        house: String,
        loc: String,
        vtc: String,
        district: String,
        subDistrict: String,
        state: String,
        pincode: String,
        customerNo: String,
        pan: String,
        gender: String,
        fatherName: String,
        dateOfApplication: String,
        regCerti: String,
        certiIncome: String
    ) {
        val progress = CustomProgressDialog(this)
        progress.show()

        val agentNo = SharedPreferenceManager.getString(SharedPreferenceManager.AGENT_NO)

        val modelJson = JSONObject().apply {
            put("custNo", customerNo.toString())
            put("CUSTNAME", name.toString())
            put("DOB", dob.toString())
            put("MOBNO", mob.toString())
            put("PANNO", pan.toString())
            put("AdharNO", uid.toString())
            put("Adreessline1", house.toString())
            put("Adreessline2", loc.toString())
            put("Adreessline3", vtc.toString())
            put("DISTRICT", district.toString())
            put("CITY", subDistrict.toString())
            put("Statecode", state.toString())
            put("Pincode", pincode.toString())
            put("Gender", gender.toString())
            put("FatherName", fatherName.toString())
            put("Agentcode", agentNo.toString())
            put("DateOfApplication", dateOfApplication.toString())
            put("RegiCerti", regCerti.toString())
            put("Certi_Inco", certiIncome.toString())
        }

        val data = AESCryptoUtil.encrypt(modelJson.toString().trimIndent())
        val encryptedData = cleanEncryptedString(data).toPlainRequestBody()

        Log.d("EncryptedData", encryptedData.toString())

        try {
            RetrofitInstance.getInstance().updateDetails(encryptedData)
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
                                    if (decryptedResponse.trim().startsWith("{")) {
                                        val jsonObject = JSONObject(decryptedResponse)
                                        val status = jsonObject.getString("message")

                                        if (status.equals("Success", ignoreCase = true)) {

                                            if (type == "1") {
                                                startActivity(
                                                    Intent(
                                                        this@CustomerDataActivity,
                                                        IndivdualPhotoActivity::class.java
                                                    )
                                                        .putExtra("type", type)
                                                        .putExtra("customerNo", customerNo)
                                                )
                                            }
                                            if (type == "2") {
                                                startActivity(
                                                    Intent(
                                                        this@CustomerDataActivity,
                                                        IndivdualPhotoActivity::class.java
                                                    )
                                                        .putExtra("type", type)
                                                        .putExtra("customerNo", customerNo)
                                                )
                                            }
                                            if (type == "3") {
                                                startActivity(
                                                    Intent(
                                                        this@CustomerDataActivity,
                                                        RelatedPersonImageActivity::class.java
                                                    )
                                                        .putExtra("type", type)
                                                        .putExtra("customerNo", customerNo)
                                                )
                                            }

                                        } else {
                                            Constant.error(
                                                this@CustomerDataActivity,
                                                "Success failed"
                                            )
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
                                Constant.error(
                                    this@CustomerDataActivity,
                                    "Empty response from server"
                                )
                            }
                        } else {
                            Constant.error(this@CustomerDataActivity, "Server returned error")
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        Constant.error(
                            this@CustomerDataActivity,
                            "API error: ${t.localizedMessage}"
                        )
                        Log.e("LoginError", t.message ?: "Unknown error")
                    }
                })
        } catch (e: Exception) {
            progress.dismiss()
            Log.e("LoginException", e.message ?: "Exception")
            Constant.error(this@CustomerDataActivity, "Unexpected error occurred")
        }
    }

    fun cleanEncryptedString(dirtyString: String): String {
        return dirtyString
            .replace("\n", "")
            .replace("\\u003d", "=")
            .replace("\"", "") // optional: removes starting/ending quotes
    }
}