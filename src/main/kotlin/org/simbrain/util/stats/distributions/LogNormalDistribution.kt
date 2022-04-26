package org.simbrain.util.stats.distributions

import org.apache.commons.math3.distribution.AbstractRealDistribution
import org.simbrain.util.UserParameter
import org.simbrain.util.stats.NegatableDistribution
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.toIntArray

/**
 * https://en.wikipedia.org/wiki/Log-normal_distribution
 */
class LogNormalDistribution(location: Double = 1.0, scale: Double = .5, negate: Boolean = false)
    : NegatableDistribution, ProbabilityDistribution() {

    // TODO: Should this be min 0? The apache requires it but wiki suggests not.
    @UserParameter(
        label = "Location (μ)",
        useSetter = true,
        description = "The mean μ of the logarithm of this distribution.",
        increment = .1,
        order = 1
    )
    var location = location
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.LogNormalDistribution(randomGenerator, scale, value)
        }

    @UserParameter(
        label = "Scale (σ)",
        useSetter = true,
        description = "The standard deviation σ of the logarithm of this distribution.",
        minimumValue = 0.0,
        increment = .1,
        order = 2
    )
    var scale = scale
        set(value) {
            field = value
            dist = org.apache.commons.math3.distribution.LogNormalDistribution(randomGenerator, value, location)
        }

    override var negate: Boolean = negate

    @Transient
    var dist: AbstractRealDistribution =
        org.apache.commons.math3.distribution.LogNormalDistribution(randomGenerator, scale, location)

    override fun sampleDouble(): Double = dist.sample().conditionalNegate()

    override fun sampleDouble(n: Int): DoubleArray = dist.sample(n).conditionalNegate()

    override fun sampleInt(): Int = dist.sample().toInt().conditionalNegate()

    override fun sampleInt(n: Int) = dist.sample(n).toIntArray().conditionalNegate()


    override fun deepCopy(): LogNormalDistribution {
        val copy = LogNormalDistribution()
        copy.randomSeed = randomSeed
        copy.location = location
        copy.scale = scale
        copy.negate = negate
        return copy
    }

    override fun getName(): String {
        return "Log-Normal"
    }

    // Kotlin hack to support "static method in superclass"
    companion object {
        @JvmStatic
        fun getTypes(): List<Class<*>> {
            return ProbabilityDistribution.getTypes()
        }
    }
}