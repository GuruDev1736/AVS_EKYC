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

        val message = "IURaC3mQe7H+nK1+VCX7rHls5ziDA6CRIUwxx+USG7ImS0zIwRd6S3iFnQCmWu9+"
        val decryptedMessage = AESCryptoUtil.decrypt(message)
        Log.d("DecryptedMessage", decryptedMessage.toString())

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