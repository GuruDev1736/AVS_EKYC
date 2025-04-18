package com.avs.avs_ekyc.Activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding : ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.actionBar.toolbar.setNavigationOnClickListener {
            finishAffinity()
        }

        binding.actionBar.toolbar.title = "Dashboard"

        val bankName = intent.getStringExtra("bank_name")
        val agentName = intent.getStringExtra("agent_name")

        binding.bankName.text = bankName
        binding.agentName.text = agentName

        binding.kyc.setOnClickListener {
            startActivity(Intent(this, ShowPendingListActivity::class.java))
        }

        binding.logout.setOnClickListener {
            startActivity(Intent(this@DashboardActivity, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }


    }
}