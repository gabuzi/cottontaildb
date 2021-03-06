package org.vitrivr.cottontail.model.basics

import org.vitrivr.cottontail.database.column.*
import org.vitrivr.cottontail.model.exceptions.ValidationException
import org.vitrivr.cottontail.model.values.*
import org.vitrivr.cottontail.model.values.types.Value
import org.vitrivr.cottontail.utilities.name.Match
import org.vitrivr.cottontail.utilities.name.Name
import java.util.*

/**
 * A definition class for a Cottontail DB column be it in a DB or in-memory context.  Specifies all the properties of such a and facilitates validation.
 *
 * @author Ralph Gasser
 * @version 1.2
 */
class ColumnDef<T : Value>(val name: Name, val type: ColumnType<T>, val logicalSize: Int = 1, val nullable: Boolean = true) {

    /**
     * Companion object with some convenience methods.
     */
    companion object {
        /**
         * Returns a [ColumnDef] with the provided attributes. The only difference as compared to using the constructor, is that the [ColumnType] can be provided by name.
         *
         * @param name Name of the new [Column]
         * @param type Name of the [ColumnType] of the new [Column]
         * @param size Logical size of the new [Column] (e.g. for vectors), where eligible.
         * @param nullable Whether or not the [Column] should be nullable.
         */
        fun withAttributes(name: Name, type: String, size: Int = -1, nullable: Boolean = true): ColumnDef<*> = ColumnDef(name, ColumnType.forName(type), size, nullable)
    }

    /** The physical size of this [ColumnDef] in bytes. */
    val physicalSize: Int
        get() = this.logicalSize * this.type.size

    /**
     * Validates a value with regard to this [ColumnDef] and throws an Exception, if validation fails.
     *
     * @param value The value that should be validated.
     * @throws ValidationException If validation fails.
     */
    fun validateOrThrow(value: Value?) {
        if (value != null) {
            if (!this.type.compatible(value)) {
                throw ValidationException("The type $type of column '$name' is not compatible with value $value.")
            }
            val cast = this.type.cast(value)
            when {
                cast is DoubleVectorValue && cast.logicalSize != this.logicalSize -> throw ValidationException("The size of column '$name' (sc=${this.logicalSize}) is not compatible with size of value (sv=${cast.logicalSize}).")
                cast is FloatVectorValue && cast.logicalSize != this.logicalSize -> throw ValidationException("The size of column '$name' (sc=${this.logicalSize}) is not compatible with size of value (sv=${cast.logicalSize}).")
                cast is LongVectorValue && cast.logicalSize != this.logicalSize -> throw ValidationException("The size of column '$name' (sc=${this.logicalSize}) is not compatible with size of value (sv=${cast.logicalSize}).")
                cast is IntVectorValue && cast.logicalSize != this.logicalSize -> throw ValidationException("The size of column '$name' (sc=${this.logicalSize}) is not compatible with size of value (sv=${cast.logicalSize}).")
                cast is Complex32VectorValue && cast.logicalSize != this.logicalSize -> throw ValidationException("The size of column '$name' (sc=${this.logicalSize}) is not compatible with size of value (sv=${cast.logicalSize}).")
                cast is Complex64VectorValue && cast.logicalSize != this.logicalSize -> throw ValidationException("The size of column '$name' (sc=${this.logicalSize}) is not compatible with size of value (sv=${cast.logicalSize}).")
            }
        } else if (!this.nullable) {
            throw ValidationException("The column '$name' cannot be null!")
        }
    }

    /**
     * Validates a value with regard to this [ColumnDef] return a flag indicating whether validation was passed.
     *
     * @param value The value that should be validated.
     * @return True if value passes validation, false otherwise.
     */
    fun validate(value: Value?): Boolean {
        if (value != null) {
            if (!this.type.compatible(value)) {
                return false
            }
            val cast = this.type.cast(value)
            return when {
                cast is DoubleVectorValue && cast.logicalSize != this.logicalSize -> false
                cast is FloatVectorValue && cast.logicalSize != this.logicalSize -> false
                cast is LongVectorValue && cast.logicalSize != this.logicalSize -> false
                cast is IntVectorValue && cast.logicalSize != this.logicalSize -> false
                cast is Complex32VectorValue && cast.logicalSize != this.logicalSize -> false
                cast is Complex64VectorValue && cast.logicalSize != this.logicalSize -> false
                else -> true
            }
        } else return this.nullable
    }

    /**
     * Returns the default value for this [ColumnDef].
     *
     * @return Default value for this [ColumnDef].
     */
    fun defaultValue(): Value? = when {
        this.nullable -> null
        this.type is StringColumnType -> StringValue("")
        this.type is FloatColumnType -> FloatValue(0.0f)
        this.type is DoubleColumnType -> DoubleValue(0.0)
        this.type is IntColumnType -> IntValue(0)
        this.type is LongColumnType -> LongValue(0L)
        this.type is ShortColumnType -> ShortValue(0.toShort())
        this.type is ByteColumnType -> ByteValue(0.toByte())
        this.type is BooleanColumnType -> BooleanValue(false)
        this.type is Complex32ColumnType -> Complex32Value.ZERO
        this.type is Complex64ColumnType -> Complex64Value.ZERO
        this.type is DoubleVectorColumnType -> DoubleVectorValue(DoubleArray(this.logicalSize))
        this.type is FloatVectorColumnType -> FloatVectorValue(FloatArray(this.logicalSize))
        this.type is LongVectorColumnType -> LongVectorValue(LongArray(this.logicalSize))
        this.type is IntVectorColumnType -> IntVectorValue(IntArray(this.logicalSize))
        this.type is BooleanVectorColumnType -> BooleanVectorValue(BitSet(this.logicalSize))
        this.type is Complex32VectorColumnType -> Complex32VectorValue(Array(this.logicalSize) { Complex32Value.ZERO })
        this.type is Complex64VectorColumnType -> Complex64VectorValue(Array(this.logicalSize) { Complex64Value.ZERO })
        else -> throw RuntimeException("Default value for the specified type $type has not been specified yet!")
    }

    /**
     * Checks if the provided [ColumnDef] is equivalent to this [ColumnDef]. Equivalence is similar to equality,
     * with the exception, that the [Name] must not necessarily match 1:1.
     *
     * @param other [ColumnDef] to check.
     * @return True if [ColumnDef]s are equivalent, false otherwise.
     */
    fun isEquivalent(other: ColumnDef<*>): Boolean {
        if (other.type != this.type) return false
        if (other.logicalSize != this.logicalSize) return false
        if (other.nullable != this.nullable) return false
        val match = other.name.match(this.name)
        return (match == Match.EQUAL || match == Match.EQUIVALENT)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColumnDef<*>

        if (name != other.name) return false
        if (type != other.type) return false
        if (logicalSize != other.logicalSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + logicalSize.hashCode()
        return result
    }

    override fun toString(): String = "$name(type=$type, size=$logicalSize, nullable=$nullable)"
}