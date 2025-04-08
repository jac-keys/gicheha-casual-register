package com.gichehafarm.registry

import com.google.android.material.tabs.TabLayoutMediator
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gichehafarm.registry.databinding.ActivityRegisterCasualBinding
import android.content.Intent

class RegisterCasualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterCasualBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterCasualBinding.inflate(layoutInflater)
        setContentView(binding.root)

      /*  setupButtons()
    }

    private fun setupButtons() {
        binding.btnAddWorker.setOnClickListener {
            // Navigate to the AddWorkerActivity or AddWorkerFragment
            val intent = Intent(this, RegisterWorkersActivity::class.java)
            startActivity(intent)
        }

        binding.btnViewWorkers?.setOnClickListener { //  Prevent NullPointerException
            val intent = Intent(this, ViewCasualsActivity::class.java)
            startActivity(intent)
        }
    }*/
}}

