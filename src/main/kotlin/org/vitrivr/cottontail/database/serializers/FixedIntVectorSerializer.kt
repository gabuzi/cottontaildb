package org.vitrivr.cottontail.database.serializers

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import org.vitrivr.cottontail.model.values.IntVectorValue

/**
 * A [Serializer] for [IntVectorValue]s that are fixed in length.
 *
 * @author Ralph Gasser
 * @version 1.0
 */
class FixedIntVectorSerializer(val size: Int) : Serializer<IntVectorValue> {
    override fun serialize(out: DataOutput2, value: IntVectorValue) {
        for (i in 0 until size) {
            out.writeInt(value[i].value)
        }
    }

    override fun deserialize(input: DataInput2, available: Int): IntVectorValue {
        val vector = IntArray(size)
        for (i in 0 until size) {
            vector[i] = input.readInt()
        }
        return IntVectorValue(vector)
    }
}