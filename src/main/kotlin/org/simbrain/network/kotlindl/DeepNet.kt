package org.simbrain.network.kotlindl

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.callback.Callback
import org.jetbrains.kotlinx.dl.api.core.history.TrainingHistory
import org.jetbrains.kotlinx.dl.api.core.loss.Losses
import org.jetbrains.kotlinx.dl.api.core.metric.Metrics
import org.jetbrains.kotlinx.dl.dataset.Dataset
import org.jetbrains.kotlinx.dl.dataset.OnHeapDataset
import org.simbrain.network.core.Network
import org.simbrain.network.events.TrainerEvents
import org.simbrain.network.matrix.ArrayLayer
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.AttributeContainer
import smile.math.matrix.Matrix
import java.awt.geom.Rectangle2D
import java.util.*

/**
 * Simbrain representation of KotlinDL sequential networks
 */
class DeepNet(
    private val network: Network,
    val tfLayers: ArrayList<TFLayer<*>>,
    nsamples: Int = 10
): ArrayLayer(network, (tfLayers[0] as TFInputLayer).rows),
    AttributeContainer,
    EditableObject {

    /**
     * Main deep network object.
     */
    lateinit var deepNetLayers: Sequential

    /**
     * Output matrix
     */
    private var outputs: Matrix? = null

    @UserParameter(label = "Output Probabilities", description = "If yes, output probabilities over class labels, " +
            "else output a one-hot encoded class label", order = 10)
    var outputProbabilities: Boolean = false

    /**
     * The data edited.
     */
    var inputData: Array<FloatArray>
    var targetData: FloatArray

    /**
     * The data used internally by KotlinDL.
     */
    lateinit var trainingDataset: Dataset
    lateinit var testingDataset: Dataset

    /**
     * Events specific to training, as contrasted with [events] which are common to all [NetworkModel]s.
     */
    val trainerEvents = TrainerEvents(this)

    /**
     * Parameters editable using an [AnnotatedPropertyEditor]
     */
    var trainingParams = TrainingParameters()
    var optimizerParams = OptimizerParameters()

    /**
     * Current loss.
     */
    var lossValue: Double = 0.0

    /**
     * Width and height are in the model for now because arrows must access them in the model.
     */
    var width: Double = 0.0
    var height: Double = 0.0

    var activations: List<FloatArray> = ArrayList<FloatArray>()

    init {
        label = network.idManager.getProposedId(this.javaClass)
        buildNetwork()
        outputs = Matrix(outputSize(), 1)
        inputData = Array(nsamples) { FloatArray(inputSize()) }
        targetData = FloatArray(nsamples)
    }

    fun buildNetwork() {
        val layers = tfLayers.map { it.create() }.toMutableList()
        deepNetLayers = Sequential.of(layers)
        deepNetLayers.also {
            it.compile(
                optimizer = optimizerParams.optimizerWrapper.optimizer,
                loss = optimizerParams.lossFunction,
                metric = optimizerParams.metric,
                callback = object: Callback() {
                    override fun onTrainBegin() {
                        println("Training begin")
                        trainerEvents.fireBeginTraining()
                    }

                    override fun onTrainEnd(logs: TrainingHistory) {
                        println("Training end:")
                        lossValue = logs.lastBatchEvent().lossValue
                        trainerEvents.fireEndTraining()
                    }
                }
            )
            deepNetLayers.numberOfClasses = outputSize().toLong()
        }
    }

    fun initializeDatasets() {
        val data = OnHeapDataset.create(inputData, targetData)
        // TODO: Make split ratio settable
        data.shuffle()
        val (train, test) = data.split(.7)
        trainingDataset = train
        testingDataset = test
    }

    fun train() {
        // Fixing batch size to 1 to make things simpler
        // TODO: Think about this...
        deepNetLayers.fit(trainingDataset, testingDataset,
            trainingParams.epochs, 1, 1)
    }

    val floatInputs: FloatArray
        get() = toFloatArray(doubleInputs)

    val doubleInputs: DoubleArray
        get() = super.getInputs().col(0)

    override fun update() {
        if (deepNetLayers.isModelInitialized) {
            if (outputProbabilities) {
                val predictions = deepNetLayers.predictSoftly(floatInputs)
                outputs = Matrix(toDoubleArray(predictions))
                val test = deepNetLayers.predictAndGetActivations(floatInputs)
                // println("Output (probabilities):" + predictions.joinToString())
            } else {
                val (prediction, activations) = deepNetLayers.predictAndGetActivations(floatInputs)
                outputs = getOneHotMat(prediction,outputSize())
                this.activations = activations.filterIsInstance<Array<*>>().flatMap { layer ->
                    layer.filterIsInstance<FloatArray>() // TODO: conv2d
                }
            }
        } else {
            outputs = Matrix(outputSize(), 1)
        }
        events.fireUpdated()
        inputs.mul(0.0) // clear inputs
    }

    override fun getOutputs(): Matrix? {
        return outputs
    }

    override fun delete() {
        deepNetLayers.close()
        super.delete()
    }

    override fun inputSize(): Int {
        return deepNetLayers.layers.first().outputShape[1].toInt();
    }

    override fun outputSize(): Int {
        return deepNetLayers.layers.last().outputShape[1].toInt();
    }

    /**
     * See [TFLayer.getRank]
     */
    fun inputRank(): Int? {
        return tfLayers.first().getRank()
    }

    /**
     * See [TFLayer.getRank]
     */
    fun outputRank(): Int? {
        return tfLayers.last().getRank()
    }

    override fun getNetwork(): Network {
        return network
    }

    override fun getId(): String {
        return super<ArrayLayer>.getId()
    }

    override fun toString(): String {
        return "${label}: : ${inputSize()} -> ${outputSize()}\n" +
                deepNetLayers.layers.joinToString("\n") { it.name }
    }

    override fun getBound(): Rectangle2D {
        return Rectangle2D.Double(x - width / 2, y - height / 2, width, height)
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    override fun readResolve(): Any {
        // Probably use deepNetLayers.saveModelConfiguration()
        super.readResolve()
        initializeDatasets()
        return this
    }

    /**
     * Helper class for creating new deep networks.
     */
    class DeepNetCreator(proposedLabel: String) : EditableObject {

        @UserParameter(label = "Label", order = 10)
        private val label = proposedLabel

        override fun getName(): String {
            return "Deep Network"
        }

        fun create(net: Network, layers: ArrayList<TFLayer<*>>): DeepNet {
            return DeepNet(net, layers)
        }
    }

}

class TrainingParameters (

    @UserParameter(label="Epochs", order = 10)
    var epochs: Int = 100,

): EditableObject {
    override fun getName(): String {
        return "Trainer parameters"
    }
}

class OptimizerParameters (

    @UserParameter(label="Optimizer", isObjectType = true, showDetails = false, order = 10)
    var optimizerWrapper: OptimizerWrapper = AdamWrapper(),

    @UserParameter(label="Loss Function", order = 20)
    var lossFunction: Losses = Losses.MSE,

    @UserParameter(label="Metric", order = 30)
    var metric: Metrics = Metrics.MSE,

    ): EditableObject {
    override fun getName(): String {
        return "Optimizer parameters"
    }
}
