package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.piccolo.getTileStackNear
import org.simbrain.util.piccolo.toPixelCoordinate
import org.simbrain.world.odorworld.entities.OdorWorldEntity

/**
 * Sensor that reacts when an object of a given type is near it.
 * <br></br>
 * While the smell framework involves objects emitting smells, object type
 * sensors have a sensitivity, and are more "sensor" or "subject" based than
 * object based.
 * <br></br>
 * The sensor itself is currently fixed at the center of the agent. We may
 * make the location editable at some point, if use-cases emerge.
 */
class TileSensor @JvmOverloads constructor(
    @UserParameter(label = "Tile Type", description = "What type of tile this sensor responds to", order = 3)
    private var tileType: String = "water",
    radius: Double = DEFAULT_RADIUS,
    angle: Double = DEFAULT_THETA
) : SensorWithRelativeLocation(radius, angle), VisualizableEntityAttribute, WithDispersion {

    /**
     * Decay function
     */
    @UserParameter(label = "Decay Function", isObjectType = true, showDetails = false, order = 15)
    override var decayFunction: DecayFunction = LinearDecayFunction(70.0)

    override var showDispersion = false

    override fun update(parent: OdorWorldEntity) {
        currentValue = 0.0
        val sensorLocation = computeAbsoluteLocation(parent)
        currentValue = with(parent.world.tileMap) {
            getTileStackNear(sensorLocation, decayFunction.dispersion)
                .filter { (_, tiles) -> tiles.any { it.type == tileType } }
                .map { (pos) -> pos.toPixelCoordinate().distance(sensorLocation) }
                .sumOf { decayFunction.getScalingFactor(it) * baseValue }
        }
    }

    override fun copy(): TileSensor {
        return TileSensor().applyCommonCopy().apply {
            tileType = this@TileSensor.tileType
            decayFunction = this@TileSensor.decayFunction.copy() as DecayFunction
        }
    }

    override val name: String
        get() = "Tile Sensor"

    override fun getLabel(): String {
        return if (super.getLabel().isEmpty()) {
            "$directionString$tileType Detector"
        } else {
            super.getLabel()
        }
    }
}