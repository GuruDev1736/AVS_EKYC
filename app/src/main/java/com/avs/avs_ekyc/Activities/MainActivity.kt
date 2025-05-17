package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.text.Html
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.avs.avs_ekyc.Constant.AESCryptoUtil
import com.avs.avs_ekyc.Constant.Constant
import com.avs.avs_ekyc.Constant.CustomProgressDialog
import com.avs.avs_ekyc.Constant.SharedPreferenceManager
import com.avs.avs_ekyc.Model.UniversalResponseModel
import com.avs.avs_ekyc.databinding.ActivityMainBinding
import com.taskease.yksfoundation.Retrofit.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.IOException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var progress : CustomProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progress = CustomProgressDialog(this@MainActivity)

        SharedPreferenceManager.init(this)

        binding.edtUsername.setText(SharedPreferenceManager.getString(SharedPreferenceManager.USERNAME))
        binding.edtPassword.setText(SharedPreferenceManager.getString(SharedPreferenceManager.PASSWORD))

//        val message = "IURaC3mQe7H+nK1+VCX7rHls5ziDA6CRIUwxx+USG7ImS0zIwRd6S3iFnQCmWu9+"
//        val decryptedMessage = AESCryptoUtil.decrypt(message)
//        Log.d("DecryptedMessage", decryptedMessage.toString())

        binding.btnLogin.setOnClickListener {
            val username = binding.edtUsername.text.toString()
            val password = binding.edtPassword.text.toString()

            if (valid(username,password))
            {
                fetchEncryptedValue(username,password)
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


    fun fetchEncryptedValue(username: String , password: String) {
        Thread {

            runOnUiThread {
                progress.show()
            }

            val loginParams = "UserName=$username&Password=$password"
            val encryptedData = AESCryptoUtil.encrypt(loginParams)
            val urlString = "https://ckyc.tbsbl.com/TBSBCKYC_APP/Login1.aspx?data=$encryptedData"

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
                    progress.dismiss()
                    callLogin(encryptedValue , username , password)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("HTTP_ERROR", "Error fetching data: ${e.message}")
            }
        }.start()
    }

    private fun callLogin(encryptedValue: String , username: String , password: String) {
        val decryptedResponse = AESCryptoUtil.decrypt((encryptedValue ?: ""))

        Log.d("EncryptedRes", (encryptedValue ?: "null"))
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
    }
}
