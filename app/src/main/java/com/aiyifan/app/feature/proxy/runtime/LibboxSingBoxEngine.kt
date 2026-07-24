package com.aiyifan.app.feature.proxy.runtime

import android.content.Context
import io.nekohasekai.libbox.BoxService
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NetworkInterface
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.SetupOptions
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState

class LibboxSingBoxEngine(context: Context) : SingBoxEngine {
    private val applicationContext = context.applicationContext
    private var service: BoxService? = null

    init {
        ensureSetup(applicationContext)
    }

    override fun start(config: String) {
        stop()
        Libbox.checkConfig(config)
        service = Libbox.newService(config, LocalProxyPlatform()).also { it.start() }
    }

    override fun stop() {
        service?.close()
        service = null
    }

    private class LocalProxyPlatform : PlatformInterface {
        override fun autoDetectInterfaceControl(fd: Int) = Unit

        override fun clearDNSCache() = Unit

        override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) = Unit

        override fun findConnectionOwner(
            ipProtocol: Int,
            sourceAddress: String,
            sourcePort: Int,
            destinationAddress: String,
            destinationPort: Int,
        ): Int = 0

        override fun getInterfaces(): NetworkInterfaceIterator = EmptyNetworkInterfaceIterator

        override fun includeAllNetworks(): Boolean = false

        override fun localDNSTransport(): LocalDNSTransport? = null

        override fun openTun(options: TunOptions): Int =
            throw UnsupportedOperationException("TUN is not used by the local proxy")

        override fun packageNameByUid(uid: Int): String = ""

        override fun readWIFIState(): WIFIState? = null

        override fun sendNotification(notification: Notification) = Unit

        override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) = Unit

        override fun systemCertificates(): StringIterator = EmptyStringIterator

        override fun uidByPackageName(packageName: String): Int = -1

        override fun underNetworkExtension(): Boolean = false

        override fun usePlatformAutoDetectInterfaceControl(): Boolean = false

        override fun useProcFS(): Boolean = false

        override fun writeLog(message: String) = Unit
    }

    private object EmptyNetworkInterfaceIterator : NetworkInterfaceIterator {
        override fun hasNext(): Boolean = false

        override fun next(): NetworkInterface? = null
    }

    private object EmptyStringIterator : StringIterator {
        override fun hasNext(): Boolean = false

        override fun len(): Int = 0

        override fun next(): String? = null
    }

    private companion object {
        private var isSetup = false

        @Synchronized
        private fun ensureSetup(context: Context) {
            if (isSetup) return
            val baseDirectory = context.filesDir.resolve("libbox").apply { mkdirs() }
            val tempDirectory = context.cacheDir.resolve("libbox").apply { mkdirs() }
            Libbox.setup(
                SetupOptions().apply {
                    basePath = baseDirectory.absolutePath
                    workingPath = baseDirectory.absolutePath
                    tempPath = tempDirectory.absolutePath
                    fixAndroidStack = true
                },
            )
            isSetup = true
        }
    }
}
