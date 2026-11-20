package com.octopuscommunity.sdk

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

/** A kernel-owned lock releases automatically if the owning process exits or crashes. */
internal class StorageProcessLease private constructor(
    private val channel: FileChannel,
    private val lock: FileLock,
) : Closeable {
    override fun close() {
        try {
            lock.release()
        } finally {
            channel.close()
        }
    }

    companion object {
        fun acquire(file: File): StorageProcessLease? {
            file.parentFile?.mkdirs()
            val channel = RandomAccessFile(file, "rw").channel
            try {
                val lock =
                    try {
                        channel.tryLock()
                    } catch (_: OverlappingFileLockException) {
                        null
                    }
                if (lock != null) return StorageProcessLease(channel, lock)
            } catch (failure: Throwable) {
                channel.close()
                throw failure
            }
            channel.close()
            return null
        }
    }
}
