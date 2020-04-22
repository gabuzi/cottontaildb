package ch.unibas.dmi.dbis.cottontail.math.knn.metrics

import ch.unibas.dmi.dbis.cottontail.model.values.types.VectorValue

object ChisquaredDistance : DistanceKernel {
    override val cost: Double
        get() = 1.0

    override fun invoke(a: VectorValue<*>, b: VectorValue<*>): Double = (((b-a).pow(2))/(b+a)).sum().value.toDouble()

    override fun invoke(a: VectorValue<*>, b: VectorValue<*>, weights: VectorValue<*>): Double = ((((b-a).pow(2))/(b+a)) * weights).sum().value.toDouble()
}