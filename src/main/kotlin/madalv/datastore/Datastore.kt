package madalv.datastore

import java.nio.charset.Charset
import java.util.NoSuchElementException
import java.util.UUID
import javax.management.openmbean.KeyAlreadyExistsException

class Datastore {
    private val map = HashMap<UUID, ByteArray>()

    @Synchronized
    fun create(key: UUID, data: ByteArray) {
        try {
            if (map.containsKey(key)) {
                throw KeyAlreadyExistsException("Can't create existent key")
            } else {
                map[key] = data
                println("CREATED $key ${String(data, Charset.defaultCharset())}")
            }
        } catch(e: Exception) {
            println(e)
        }
    }

    @Synchronized
    fun read(key: UUID): ByteArray {
        return try {
            //println("READ $key")
            map[key]!!
        } catch (e: Exception) {
            throw e
        }
    }

    @Synchronized
    fun update(key: UUID, data: ByteArray) {
        try {
            if (!map.containsKey(key)) {
                throw NoSuchElementException("Required key does not exist")
            } else {
                map[key] = data
                println("UPDATED $key")
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    @Synchronized
    fun delete(key: UUID) {
        try {
            if (!map.containsKey(key)) {
                throw NoSuchElementException("Required key does not exist")
            } else {
                map.remove(key)
                println("REMOVED $key")
            }
        } catch (e: Exception) {
            println(e)
        }
    }

    @Synchronized
    fun containsKey(key: UUID): Boolean {
        return map.containsKey(key)
    }
}