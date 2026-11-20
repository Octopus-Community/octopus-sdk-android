package com.octopuscommunity.sdk

import android.app.Application
import android.content.Context
import android.os.Build
import java.io.File

/** Checks run before creating a graph or opening SDK data stores. */
internal object HostIntegrationGuard {
    private const val GUIDE = "See https://doc.octopuscommunity.com/SDK/quickstart/?platform=android (Android installation and setup)."
    private val compatibleKoin by lazy { probeKoin() }
    private val reported = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    internal fun probeKoin(
        loadClass: (String) -> Class<*> = {
            Class.forName(it, false, HostIntegrationGuard::class.java.classLoader)
        },
    ): Boolean =
        try {
            loadClass("org.koin.core.Koin").getMethod(
                "createScope",
                String::class.java,
                loadClass("org.koin.core.qualifier.Qualifier"),
                Any::class.java,
                loadClass("org.koin.core.qualifier.TypeQualifier"),
            )
            true
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: LinkageError) {
            false
        }

    private val storageOwners = mutableMapOf<String, StorageProcessLease>()

    fun allowsInitialization(context: Context): Boolean = allowsRuntime() && allowsStorage(context)

    fun allowsRuntime(): Boolean {
        if (!compatibleKoin) {
            report(
                "koin",
                "Octopus initialization refused: incompatible Koin runtime. Align all Koin artifacts with the required Koin 4.1.1 APIs.",
            )
            return false
        }
        return true
    }

    fun allowsStorage(context: Context): Boolean =
        try {
            val app = context.applicationContext ?: context
            val processName = currentProcessName()
            val declaredProcess = app.applicationInfo.processName?.takeIf { it.isNotBlank() } ?: app.packageName
            if (processName == null) {
                warn("process-name", "Octopus storage ownership could not determine this process; initialization is allowed.")
                true
            } else {
                val isApplicationProcess = processName == declaredProcess
                val file = app.noBackupFilesDir.resolve("octopus_storage.lock")
                synchronized(storageOwners) {
                    if (storageOwners.containsKey(file.absolutePath)) return@synchronized true
                    val lease = StorageProcessLease.acquire(file)
                    when {
                        lease != null -> {
                            // DataStore instances can outlive stop(); retain any acquired lease until exit.
                            storageOwners[file.absolutePath] = lease
                            true
                        }
                        isApplicationProcess -> true
                        else -> {
                            report(
                                "storage-owner",
                                "Octopus initialization refused: another process owns the SDK storage. Retry initialize after that process exits.",
                            )
                            false
                        }
                    }
                }
            }
        } catch (failure: Exception) {
            warn("storage-lock", "Octopus could not protect SDK storage (${failure.javaClass.simpleName}); initialization is allowed.")
            true
        }

    internal fun parseProcessName(cmdline: String): String? = cmdline.substringBefore('\u0000').trim().takeIf { it.isNotEmpty() }

    private fun currentProcessName(): String? =
        if (Build.VERSION.SDK_INT >= 28) {
            Application.getProcessName()?.takeIf { it.isNotBlank() }
        } else {
            try {
                parseProcessName(File("/proc/self/cmdline").readText())
            } catch (_: Exception) {
                null
            }
        }

    private fun warn(
        key: String,
        message: String,
    ) {
        if (reported.add(key)) android.util.Log.w("OctopusSDK", "$message $GUIDE")
    }

    private fun report(
        key: String,
        message: String,
    ) {
        if (reported.add(key)) android.util.Log.e("OctopusSDK", "$message $GUIDE")
    }
}
