package com.avs.avs_ekyc.Model

data class UpdateDetailsModel(
    val custNo: String,
    val CUSTNAME: String,
    val DOB: String,
    val MOBNO: String,
    val PANNO: String,
    val AdharNO: String,
    val Adressline1: String,
    val Adressline2: String,
    val Adressline3: String,
    val DISTRICT: String,
    val CITY: String,
    val Statecode: String,
    val Pincode: String,
    val Gender: String,
    val FatherName: String,
    val Agentcode: String,
    val DateOfApplication: String,
    val RegiCerti: String,
    val Certi_Inco: String
)
