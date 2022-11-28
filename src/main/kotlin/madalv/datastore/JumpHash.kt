package madalv.datastore

import kotlin.random.Random

class JumpHash {
    companion object {
        private const val secretKey: String = "m-am_zb"

        fun hash(key: String, nrNodes: Int): Int {
            val random = Random(key.hashCode())
            var b = 0

            for (j in 1 until nrNodes) {
                if (random.nextDouble() < 1.0 / (j + 1)) b = j
            }
            return b
        }

        fun getDuplicateId(key: String, nrNodes: Int): Int {

            val originalId = hash(key, nrNodes)
            var duplicateId = hash(key + secretKey, nrNodes)

            while (duplicateId == originalId) {
                val dKey = key + secretKey
                duplicateId = hash(dKey, nrNodes)
            }

            return duplicateId
        }
    }
}