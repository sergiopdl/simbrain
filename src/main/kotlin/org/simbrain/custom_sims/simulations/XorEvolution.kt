package org.simbrain.custom_sims.simulations

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.workspace.Workspace

fun main() {

    val environment = networkEnvironment {
        val network = Network()
        val neuron1 = Neuron(network)
        templateNetwork = NetworkGenome(
                nodes = listOf(NodeChromosome(
                        listOf(NodeGene(neuron1.apply { label = "Neuron1" }), NodeGene(Neuron(network).apply { label =
                                "Neuron2" }))
                )),
                connections = listOf(ConnectionChromosome(
                        listOf(createConnectionChromosome("Neuron1", "Neuron2"))
                )),
                id = "Network1"
        )
        eval = {
            val workspace = Workspace().also { it.addWorkspaceComponent(NetworkComponent("net", network)) }
            workspace.simpleIterate()
            neuron1.activation + 1.0
        }
    }

    environment.runEvolution()
            .upToGeneration(50)
            .untilFitnessScore { it < 0.1 }
            .onEach { println("${it.best().fitness} -> ${it.best().network}") }
            .last()
            .best()

}

