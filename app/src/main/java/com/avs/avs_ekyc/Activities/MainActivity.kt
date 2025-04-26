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
import com.avs.avs_ekyc.databinding.ActivityMainBinding
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import okhttp3.ResponseBody
import okio.IOException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SharedPreferenceManager.init(this)

        binding.edtUsername.setText(SharedPreferenceManager.getString(SharedPreferenceManager.USERNAME))
        binding.edtPassword.setText(SharedPreferenceManager.getString(SharedPreferenceManager.PASSWORD))

        val message = "X5GQQibbtZ35AOds+LgTe/E4X71uD+C8rzNOfnEl9IGGBLMwSbhTDcyhRG0BwN1gUdYAuLa0ek813DxCdXeNlwsK536sbCbdEkaO1u39hBnLwNIIsn2qwjneWaCvg2Hgs0xKxSf6wtpvDkfwIVsab/q7SGuXMWg0ES0jkm7k/3e2bjmotsMRcjS2G6BGMaHaqh0/UA6NqFi/ksIlaJjfsAn6A+4+h7UOZRjPIzp1cBqxoKpqWO9wJhJuKsVfx5s608A3RFa57n5E08wupsyh/+K+vWRf4/sOlT7FEwnDCX70SS+M3C9x12eLl4lV58H4BgpnGquzkSIKlt4JmU1ASAEdME9dPhUnrCRFGyDJQTX6fwbfAaLewrh0k2O4ffNriS6pAxEZk6VMDUu3B+o9VQ4rDJd8y9QLeAq4ENf1XAXB61O8MoYvKWdo1DvpmRdXrd5Q5zAV5q2CJzKyYJIpqea3t7nbVw7tmJdt9RhRF1ju4QNsjfQS3DGxn2HB8WRtjjtJi7btM0CqczWAeK++b3plGBfH/XaOXCH6w/BSdgSbrYvTV2LQ/ZxiljBUTP478y/msAmroqk7+KC/cvcTR5L0Q1MZL4GgQxLg1uptXDN6IhtxaYFikL4B9OTR+89APkX8RvRRh2WYIZLttvMeZ6sEpEUQuXybWyTPXDSOZ6m2lwg17EkYXHFTJsL+eYjVFg4WPxOfvEgXo6gsdNQLJRBe6w4RJMaI8Ju+O38DW/+gy2mjUROWnLDkDzdRQT7HV4pQz6EzawucVcw2ZVyD7j5kvPrx29m8VkbXiJk/p8LJfcwIAPql2d4vqjfTmXS8xyfSsy4QnMP03LnZXtysPGt+dHNuRsAEMXQdTOcr1jSupPe4aXwpPrm7rS/VzvDV+xzzjDLuIDLanaau7Z+5/+7dhGVR9220HSjgjlU+FzI\u003d"
        val decryptedMessage = AESCryptoUtil.decrypt(message)
        Log.d("DecryptedMessage", decryptedMessage.toString())


        val jsonData = JSONObject().apply {
            put("AdharNO", "0")
            put("Adressline1", " ")
            put("Adressline2", "  ")
            put("Adressline3", "  ")
            put("Agentcode", "6016")
            put("CITY", "Nagpur")
            put("CUSTNAME", "VARADKAR BHIKAJI YELOJI")
            put("Certi_Inco", "")
            put("DISTRICT", "Nagpur")
            put("DOB", "01/01/1900")
            put("DateOfApplication", "")
            put("FatherName", "")
            put("Gender", "")
            put("MOBNO", "")
            put("PANNO", "")
            put("Pincode", "440003")
            put("RegiCerti", "")
            put("Statecode", "MH")
            put("custNo", "114431")
        }

        val encryptedMessage = AESCryptoUtil.encrypt(jsonData.toString())
        Log.d("EncryptedMessage", encryptedMessage)

        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString()
            val password = binding.edtPassword.text.toString()

            if (valid(username,password))
            {
                callLogin(username,password)
            }
        }
    }

    private fun valid(username: String, password: String): Boolean {
        if (username.isEmpty())
        {
            binding.edtUsername.error = "Enter Username"
            return false
        }
        else if (password.isEmpty())
        {
            binding.edtPassword.error = "Enter Password"
            return false
        }
        return true
    }


    private fun callLogin(username: String, password: String) {
        val progress = CustomProgressDialog(this)
        progress.show()

        val loginParams = "UserName=$username&Password=$password"
        val encryptedData = AESCryptoUtil.encrypt(loginParams)

        try {
            RetrofitInstance.getInstance().login(encryptedData)
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
                                        val status = jsonObject.getString("Status")
                                        val bankName = jsonObject.getString("BankName")
                                        val agentName = jsonObject.getString("Agent_Name")
                                        val agentNo = jsonObject.getString("Agent_No")

                                        if (status.equals("Success", ignoreCase = true)) {

                                            SharedPreferenceManager.saveString(
                                                SharedPreferenceManager.AGENT_NO , agentNo)

                                            SharedPreferenceManager.saveString(
                                                SharedPreferenceManager.USERNAME,username)
                                            SharedPreferenceManager.saveString(
                                                SharedPreferenceManager.PASSWORD,password)

                                            Constant.success(this@MainActivity, "Login successful")
                                            startActivity(Intent(this@MainActivity,
                                                DashboardActivity::class.java)
                                                .putExtra("bank_name",bankName)
                                                .putExtra("agent_name",agentName)
                                            )
                                            finish()
                                        } else {
                                            Constant.error(this@MainActivity, "Login failed")
                                        }
                                    } else {
                                        Constant.error(this@MainActivity, decryptedResponse)
                                        Log.e("LoginError", "Decrypted response is not JSON")
                                    }
                                } catch (e: Exception) {
                                    Log.e("JSON Error", e.message ?: "Parsing error")
                                    Constant.error(this@MainActivity, "Parsing error")
                                }
                            } else {
                                Constant.error(this@MainActivity, "Empty response from server")
                            }
                        } else {
                            Constant.error(this@MainActivity, "Server returned error")
                        }
                    }

                    override fun onFailure(call: Call<UniversalResponseModel>, t: Throwable) {
                        progress.dismiss()
                        if (t is IOException) {
                            Constant.error(this@MainActivity, "Network issue. Check connection.")
                        } else {
                            Constant.error(this@MainActivity, "API error: ${t.localizedMessage}")
                        }
                        Log.e("LoginError", t.message ?: "Unknown error")
                    }
                })
        } catch (e: Exception) {
            progress.dismiss()
            Log.e("LoginException", e.message ?: "Exception")
            Constant.error(this@MainActivity, "Unexpected error occurred")
        }
    }
}