package com.aiyifan.app.feature.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aiyifan.app.core.ui.applySystemBarsPadding
import com.aiyifan.app.core.ui.setupEdgeToEdge
import com.aiyifan.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(left = true, top = true, right = true, bottom = true)
        binding.backButton.setOnClickListener { finish() }
        binding.loginButton.setOnClickListener {
            if (!binding.agreementCheck.isChecked) {
                Toast.makeText(this, "请先同意用户协议与隐私政策", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val account = binding.accountEdit.text.toString().ifBlank { "爱壹帆用户" }
            getSharedPreferences("auth", MODE_PRIVATE).edit().putString("nickname", account).apply()
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
