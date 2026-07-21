package com.aiyifan.app.feature.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aiyifan.app.R
import com.aiyifan.app.core.ui.applySystemBarsPadding
import com.aiyifan.app.core.ui.setupEdgeToEdge
import com.aiyifan.app.databinding.ActivityMainBinding
import com.aiyifan.app.feature.home.HomeFragment
import com.aiyifan.app.feature.hot.HotFragment
import com.aiyifan.app.feature.mine.MineFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.fragmentContainer.applySystemBarsPadding(left = true, top = true, right = true)
        binding.bottomNav.applySystemBarsPadding(left = true, right = true, bottom = true, growHeight = true)

        if (savedInstanceState == null) {
            show(HomeFragment())
        }
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> show(HomeFragment())
                R.id.nav_hot -> show(HotFragment())
                R.id.nav_mine -> show(MineFragment())
            }
            true
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
