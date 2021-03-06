package org.vitrivr.cottontail.model.values

import org.vitrivr.cottontail.model.values.types.ComplexVectorValue
import org.vitrivr.cottontail.model.values.types.NumericValue
import org.vitrivr.cottontail.model.values.types.Value
import org.vitrivr.cottontail.model.values.types.VectorValue
import java.util.*

/**
 * This is an abstraction over an [Array] and it represents a vector of [Complex32]s.
 *
 * @author Manuel Huerbin & Ralph Gasser
 * @version 1.1
 */
inline class Complex32VectorValue(val data: Array<Complex32Value>) : ComplexVectorValue<Float> {


    companion object {
        /**
         * Generates a [Complex32VectorValue] of the given size initialized with random numbers.
         *
         * @param size Size of the new [Complex32VectorValue]
         * @param rnd A [SplittableRandom] to generate the random numbers.
         */
        fun random(size: Int, rnd: SplittableRandom = SplittableRandom(System.currentTimeMillis())) = Complex32VectorValue(Array(size) {
            Complex32Value(rnd.nextDouble().toFloat(), rnd.nextDouble().toFloat())
        })

        /**
         * Generates a [Complex32VectorValue] of the given size initialized with ones.
         *
         * @param size Size of the new [Complex32VectorValue]
         */
        fun one(size: Int) = Complex32VectorValue(Array(size) {
            Complex32Value.ONE
        })

        /**
         * Generates a [Complex32VectorValue] of the given size initialized with zeros.
         *
         * @param size Size of the new [Complex32VectorValue]
         * @param rnd A [SplittableRandom] to generate the random numbers.
         */
        fun zero(size: Int) = Complex32VectorValue(Array(size) {
            Complex32Value.ZERO
        })
    }

    override val logicalSize: Int
        get() = this.data.size/2

    /**
     * Returns the i-th entry of  this [Complex32VectorValue]. All entries with i % 2 == 0 correspond
     * to the real part of the value, whereas entries with i % 2 == 1 correspond to the imaginary part.
     *
     * @param i Index of the entry.
     * @return The value at index i.
     */
    override fun get(i: Int) = this.data[i]
    override fun real(i: Int) = this.data[i].real
    override fun imaginary(i: Int) = this.data[i].imaginary


    override fun compareTo(other: Value): Int {
        throw IllegalArgumentException("ComplexVectorValues can can only be compared for equality.")
    }

    /**
     * Returns the indices of this [Complex32VectorValue].
     *
     * @return The indices of this [Complex32VectorValue]
     */
    override val indices: IntRange
        get() = (0 until this.logicalSize)

    /**
     * Returns the i-th entry of  this [Complex32VectorValue] as [Boolean]. All entries with index % 2 == 0 correspond
     * to the real part of the value, whereas entries with i % 2 == 1 correspond to the imaginary part.
     *
     * @param i Index of the entry.
     * @return The value at index i.
     */
    override fun getAsBool(i: Int): Boolean = this.data[i] != Complex32Value.ZERO

    /**
     * Returns true, if this [Complex32VectorValue] consists of all zeroes, i.e. [0, 0, ... 0]
     *
     * @return True, if this [Complex32VectorValue] consists of all zeroes
     */
    override fun allZeros(): Boolean = this.data.all { it == Complex32Value.ZERO  }

    /**
     * Returns true, if this [Complex32VectorValue] consists of all ones, i.e. [1, 1, ... 1]
     *
     * @return True, if this [Complex32VectorValue] consists of all ones
     */
    override fun allOnes(): Boolean = this.data.all { it == Complex32Value.ONE }

    /**
     * Creates and returns a copy of this [Complex32VectorValue].
     *
     * @return Exact copy of this [Complex32VectorValue].
     */
    override fun copy(): Complex32VectorValue = Complex32VectorValue(data.copyOf())

    override fun plus(other: VectorValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] + other[it].asComplex32()
    })

    override fun minus(other: VectorValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] - other[it].asComplex32()
    })

    override fun times(other: VectorValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] * other[it].asComplex32()
    })

    override fun div(other: VectorValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] / other[it].asComplex32()
    })

    override fun plus(other: NumericValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] + other.asComplex32()
    })

    override fun minus(other: NumericValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] - other.asComplex32()
    })

    override fun times(other: NumericValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] * other.asComplex32()
    })

    override fun div(other: NumericValue<*>) = Complex32VectorValue(Array(this.data.size) {
        this.data[it] / other.asComplex32()
    })

    override fun pow(x: Int) = Complex64VectorValue(Array(this.data.size) {
        this.data[it].pow(x)
    })

    override fun sqrt() = Complex64VectorValue(Array(this.data.size) {
        this.data[it].sqrt()
    })

    override fun abs() = Complex32VectorValue(Array(this.data.size) {
        this.data[it].abs()
    })

    override fun sum(): Complex32Value {
        var real = 0.0f
        var imaginary = 0.0f
        this.indices.forEach {
            real += this.data[it].data[0]
            imaginary += this.data[it].data[1]
        }
        return Complex32Value(real, imaginary)
    }

    override fun norm2(): Complex32Value {
        var sum = Complex64Value(0.0, 0.0)
        for (i in this.indices) {
            sum += this[i].pow(2)
        }
        return sum.sqrt().asComplex32()
    }

    override fun dot(other: VectorValue<*>): FloatValue {
        var sum = 0.0f
        for (i in this.indices) {
            sum += (other[i].asComplex32() * this[i].conjugate()).real.value
        }
        return FloatValue(sum)
    }

    override fun l1(other: VectorValue<*>): Complex32Value {
        var sReal = 0.0
        var sImaginary = 0.0
        when (other) {
            is Complex32VectorValue -> {
                for (i in this.indices) {
                    sReal += kotlin.math.abs(this.data[i].data[0] - other.data[i].data[0])
                    sImaginary += kotlin.math.abs(this.data[i].data[1] - other.data[i].data[1])
                }
            }
            is Complex64VectorValue -> {
                for (i in this.indices) {
                    sReal += kotlin.math.abs(this.data[i].data[0] - other.data[i].data[0])
                    sImaginary += kotlin.math.abs(this.data[i].data[1] - other.data[i].data[1])
                }
            }
            else -> {
                for (i in this.indices) {
                    val d = (this[i] - other[i]).abs()
                    sReal += d.data[0]
                    sImaginary += d.data[1]
                }
            }
        }
        return Complex32Value(sReal, sImaginary).asComplex32()
    }

    override fun l2(other: VectorValue<*>): Complex32Value {
        var sum = Complex64Value(0.0, 0.0)
        when (other) {
            is Complex32VectorValue -> {
                for (i in this.indices) {
                    sum += Complex64Value(this.data[i].data[0] - other.data[i].data[0], this.data[i].data[1] - other.data[i].data[1]).pow(2)
                }
            }
            is Complex64VectorValue -> {
                for (i in this.indices) {
                    sum += Complex64Value(this.data[i].data[0] - other.data[i].data[0], this.data[i].data[1] - other.data[i].data[1]).pow(2)
                }
            }
            else -> {
                for (i in this.indices) {
                    sum += (this[i] - other[i]).pow(2)
                }
            }
        }
        return sum.sqrt().asComplex32()
    }

    override fun lp(other: VectorValue<*>, p: Int): Complex32Value {
        var sum = Complex32Value(0.0f, 0.0f)
        for (i in this.indices) {
            sum += (other[i].asComplex32() - this[i]).pow(p)
        }
        return sum.pow(1.0/p).asComplex32()
    }
}