package madalv.datastore

import java.util.UUID

class Datastore {
    private val map = HashMap<UUID, ByteArray>()

    fun create(data: ByteArray) {
        try {
            val uuid = UUID.randomUUID()
            map[uuid] = data
            println("CREATED $uuid $data")
        } catch(e: Exception) {
            println(e.message)
        }
    }

    fun read(key: UUID): ByteArray {
        return try {
            println("READ $key")
            map[key]!!

        } catch (e: Exception) {
            println(e.message)
            ByteArray(0)
        }
    }

    fun update(key: UUID, data: ByteArray) {
        try {
            map[key] = data
            println("UPDATED $key")
        } catch (e: Exception) {
            println(e.message)
        }
    }

    fun delete(key: UUID) {
        try {
            map.remove(key)
            println("REMOVED $key")
        } catch (e: Exception) {
            println(e.message)
        }
    }
}