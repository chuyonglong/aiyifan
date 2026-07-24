package com.aiyifan.app.feature.main

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element

class MainNavigationLayoutTest {

    @Test
    fun `main navigation uses three fixed text tabs`() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layoutFile())
        val tabLayout = viewWithId(document.documentElement, "bottomTabs")

        assertNotNull(tabLayout)
        assertEquals("fixed", tabLayout!!.getAttribute("app:tabMode"))
        assertEquals("fill", tabLayout.getAttribute("app:tabGravity"))
        assertEquals(3, tabLayout.getElementsByTagName("com.google.android.material.tabs.TabItem").length)
    }

    private fun layoutFile(): File = sequenceOf(
        File("src/main/res/layout/activity_main.xml"),
        File("app/src/main/res/layout/activity_main.xml"),
    ).first(File::isFile)

    private fun viewWithId(root: Element, id: String): Element? =
        (0 until root.getElementsByTagName("*").length).asSequence()
            .map(root.getElementsByTagName("*")::item)
            .map { it as Element }
            .firstOrNull {
                it.getAttribute("android:id").substringAfter("@+id/", "") == id
            }
}
