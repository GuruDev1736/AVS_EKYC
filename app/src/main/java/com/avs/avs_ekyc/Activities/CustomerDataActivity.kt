package com.avs.avs_ekyc.Activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.avs.avs_ekyc.R
import com.avs.avs_ekyc.databinding.ActivityCustomerDataBinding

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
        val customerNo = bundle?.getString("customerNo")
        val customerName = bundle?.getString("customerName")
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

        }
    }
}