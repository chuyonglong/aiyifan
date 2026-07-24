package com.aiyifan.app.feature.home

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aiyifan.app.R
import com.aiyifan.app.feature.main.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRefreshLayoutTest {

    @Test
    fun homeExposesPullToRefreshContainer() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById<SwipeRefreshLayout>(R.id.homeRefresh))
            }
        }
    }
}
