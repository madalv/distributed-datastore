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

        fun getDuplicates(key: String, nrNodes: Int): Set<Int> {
            val originalId = hash(key, nrNodes)
            val set = mutableSetOf(originalId)

            for (i in 0 until 1) {
                var duplicateId = hash(key + secretKey, nrNodes)

                while (duplicateId in set) {
                    val dKey = key + secretKey
                    duplicateId = hash(dKey, nrNodes)
                }
                set.add(duplicateId)
            }

            return set
        }
    }
}