package madalv.datastore

import java.nio.charset.Charset
import java.util.UUID

class Datastore {
    private val map = HashMap<UUID, ByteArray>()

    @Synchronized
    fun create(data: ByteArray): UUID {
        return try {
            val uuid = UUID.randomUUID()
            map[uuid] = data
            println("CREATED $uuid ${String(data, Charset.defaultCharset())}")
            uuid
        } catch(e: Exception) {
            throw e
        }
    }

    @Synchronized
    fun read(key: UUID): ByteArray {
        return try {
            println("READ $key")
            map[key]!!
        } catch (e: Exception) {
            throw e
        }
    }

    @Synchronized
    fun update(key: UUID, data: ByteArray) {
        try {
            map[key] = data
            println("UPDATED $key")
        } catch (e: Exception) {
            println(e.message)
        }
    }

    @Synchronized
    fun delete(key: UUID) {
        try {
            map.remove(key)
            println("REMOVED $key")
        } catch (e: Exception) {
            println(e.message)
        }
    }
}