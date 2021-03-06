package org.vitrivr.cottontail.model.values

import org.vitrivr.cottontail.model.values.types.ComplexVectorValue
import org.vitrivr.cottontail.model.values.types.NumericValue
import org.vitrivr.cottontail.model.values.types.Value
import org.vitrivr.cottontail.model.values.types.VectorValue
import java.util.*

/**
 * This is an abstraction over an [Array] and it represents a vector of [Complex64]s.
 *
 * @author Manuel Huerbin & Ralph Gasser
 * @version 1.1
 */
inline class Complex64VectorValue(val data:  Array<Complex64Value>) : ComplexVectorValue<Double> {

    companion object {
        /**
         * Generates a [Complex32VectorValue] of the given size initialized with random numbers.
         *
         * @param size Size of the new [Complex32VectorValue]
         * @param rnd A [SplittableRandom] to generate the random numbers.
         */
        fun random(size: Int, rnd: SplittableRandom = SplittableRandom(System.currentTimeMillis())) = Complex64VectorValue(Array(size) {
            Complex64Value(DoubleValue(Double.fromBits(rnd.nextLong())), DoubleValue(Double.fromBits(rnd.nextLong())))
        })

        /**
         * Generates a [Complex32VectorValue] of the given size initialized with ones.
         *
         * @param size Size of the new [Complex32VectorValue]
         */
        fun one(size: Int) = Complex64VectorValue(Array(size) {
            Complex64Value.ONE
        })

        /**
         * Generates a [Complex32VectorValue] of the given size initialized with zeros.
         *
         * @param size Size of the new [Complex32VectorValue]
         * @param rnd A [SplittableRandom] to generate the random numbers.
         */
        fun zero(size: Int) = Complex64VectorValue(Array(size) {
            Complex64Value.ZERO
        })
    }


    override val logicalSize: Int
        get() = this.data.size / 2

    /**
     * Returns the i-th entry of  this [Complex64VectorValue]. All entries with i % 2 == 0 correspond
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
     * Returns the indices of this [Complex64VectorValue].
     *
     * @return The indices of this [Complex64VectorValue]
     */
    override val indices: IntRange
        get() = this.data.indices


    /**
     * Returns the i-th entry of  this [Complex64VectorValue] as [Boolean]. All entries with index % 2 == 0 correspond
     * to the real part of the value, whereas entries with i % 2 == 1 correspond to the imaginary part.
     *
     * @param i Index of the entry.
     * @return The value at index i.
     */
    override fun getAsBool(i: Int): Boolean = this.data[i] != Complex64Value.ZERO

    /**
     * Returns true, if this [Complex64VectorValue] consists of all zeroes, i.e. [0, 0, ... 0]
     *
     * @return True, if this [Complex64VectorValue] consists of all zeroes
     */
    override fun allZeros(): Boolean = this.data.all { it == Complex64Value.ZERO }

    /**
     * Returns true, if this [Complex64VectorValue] consists of all ones, i.e. [1, 1, ... 1]
     *
     * @return True, if this [Complex64VectorValue] consists of all ones
     */
    override fun allOnes(): Boolean = this.data.all { it == Complex64Value.ONE }

    /**
     * Creates and returns a copy of this [Complex64VectorValue].
     *
     * @return Exact copy of this [Complex64VectorValue].
     */
    override fun copy(): Complex64VectorValue = Complex64VectorValue(this.data.copyOf())

    override fun plus(other: VectorValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] + other[it].asComplex64()
    })

    override fun minus(other: VectorValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] - other[it].asComplex64()
    })

    override fun times(other: VectorValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] * other[it].asComplex64()
    })

    override fun div(other: VectorValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] / other[it].asComplex64()
    })

    override fun plus(other: NumericValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] + other.asComplex64()
    })

    override fun minus(other: NumericValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] - other.asComplex64()
    })

    override fun times(other: NumericValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] * other.asComplex64()
    })

    override fun div(other: NumericValue<*>) = Complex64VectorValue(Array(this.data.size) {
        this[it] / other.asComplex64()
    })

    override fun pow(x: Int) = Complex64VectorValue(Array(this.data.size) {
        this.data[it].pow(x)
    })

    override fun sqrt() = Complex64VectorValue(Array(this.data.size) {
        this.data[it].sqrt()
    })

    override fun abs() = Complex64VectorValue(Array(this.data.size) {
        this.data[it].abs()
    })

    override fun sum(): Complex64Value {
        var real = 0.0
        var imaginary = 0.0
        this.indices.forEach {
            real += this.real(it).value
            imaginary += this.imaginary(it).value
        }
        return Complex64Value(real, imaginary)
    }

    override fun norm2(): Complex64Value {
        var sum = Complex64Value(0.0, 0.0)
        for (i in this.indices) {
            sum += this[i].pow(2)
        }
        return sum.sqrt()
    }

    override fun dot(other: VectorValue<*>): DoubleValue {
        var sum = 0.0
        for (i in this.indices) {
            sum += (other[i].asComplex64() * this[i].conjugate()).real.value
        }
        return DoubleValue(sum)
    }

    override fun l1(other: VectorValue<*>): Complex64Value {
        var sum = Complex64Value(0.0, 0.0)
        for (i in this.indices) {
            sum += (other[i].asComplex64() - this[i]).abs()
        }
        return sum
    }

    override fun l2(other: VectorValue<*>): Complex64Value {
        var sReal = 0.0
        var sImaginary = 0.0
        when (other) {
            is Complex32VectorValue -> {
                for (i in this.indices) {
                    val d = (other.data[i] - this.data[i]).pow(2)
                    sReal += d.data[0]
                    sImaginary += d.data[1]
                }
            }
            is Complex64VectorValue -> {
                for (i in this.indices) {
                    val d = (other.data[i] - this.data[i]).pow(2)
                    sReal += d.data[0]
                    sImaginary += d.data[1]
                }
            }
            else -> {
                for (i in this.indices) {
                    val d = (other[i] - this[i]).pow(2)
                    sReal += d.real.value
                    sImaginary += d.imaginary.value
                }
            }
        }
        return Complex64Value(sReal, sImaginary).sqrt()
    }

    override fun lp(other: VectorValue<*>, p: Int): Complex64Value {
        var sum = Complex64Value(0.0, 0.0)
        for (i in this.indices) {
            sum += (other[i].asComplex64() - this[i]).pow(p)
        }
        return sum.pow(1.0 / p)
    }
}