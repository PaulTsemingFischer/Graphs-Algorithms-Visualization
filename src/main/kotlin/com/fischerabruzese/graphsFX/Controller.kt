package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Font
import java.security.InvalidKeyException
import kotlin.math.*
import kotlin.system.measureTimeMillis
import java.util.concurrent.Executors
import kotlin.properties.Delegates

class Controller<E: Any> {
    //Pane
    @FXML
    private lateinit var pane: Pane
    private lateinit var paneWidth: ReadOnlyDoubleProperty
    private lateinit var paneHeight: ReadOnlyDoubleProperty
    private lateinit var graphicComponents: GraphicComponents<E>

    //Fields
    @FXML
    private lateinit var probabilityField: TextField

    @FXML
    private lateinit var minWeightField: TextField

    @FXML
    private lateinit var maxWeightField: TextField

    @FXML
    private lateinit var allowDisjointToggle: CheckBox

    @FXML
    private lateinit var connectedness: TextField

    @FXML
    private lateinit var fromVertexField: TextField

    @FXML
    private lateinit var toVertexField: TextField

    //Console
    @FXML
    private lateinit var console: TextArea
    private val CONSOLE_LINE_SEPARATOR = "-".repeat(20) + "\n"

    //Data
    private lateinit var graph: Graph<E>
    private val stringToVMap = HashMap<String, GraphicComponents<E>.Vertex>()

    //Initialization
    @FXML
    fun initialize() {
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    fun setGraph(graph: Graph<E>) {
        this.graph = graph
        graphicComponents = GraphicComponents(graph, pane, stringToVMap)
    }

    fun draw() {
        graphicComponents.draw()
    }

    @FXML
    private fun redrawPressed() {
        graphicComponents.draw()
    }

    //Graph presets
    @FXML
    private fun preset1Pressed() {
    }

    @FXML
    private fun preset2Pressed() {
    }

    @FXML
    private fun preset3Pressed() {
    }

    @FXML
    private fun preset4Pressed() {
    }

    @FXML
    private fun preset5Pressed() {
    }

    @FXML
    private fun preset6Pressed() {
    }

    //Console
    private fun printClusters(clusters: List<List<E>>, connectedness: Double) {
        console.text += buildString {
            append("Clusters (connectedness: $connectedness)\n")
            for (cluster in clusters) {
                append("$cluster\n")
            }
            append(CONSOLE_LINE_SEPARATOR)
        }
    }

    private fun printDijkstra(from: E, to: E, path: List<E>, distance: Int, time: Long) {
        console.text += buildString {
            append("Dijkstra from $from to $to\n")
            append("Path: $path\n")
            append("Distance: $distance\n")
            append("Time(ms): $time\n")
            append(CONSOLE_LINE_SEPARATOR)
        }
    }

    private fun printBfs(from: E, to: E, path: List<E>, time: Long) {
        console.text += buildString {
            append("Breadth first search from $from to $to\n")
            append("Path: $path\n")
            append("Time(ms): $time\n")
            append(CONSOLE_LINE_SEPARATOR)
        }
    }

    private fun printDfs(from: E, to: E, path: List<E>, time: Long) {
        console.text += buildString {
            append("Depth first search from $from to $to\n")
            append("Path: $path\n")
            append("Time(ms): $time\n")
            append(CONSOLE_LINE_SEPARATOR)
        }
    }

    //Randomization
    @FXML
    private fun randomizePressed() {

    }

    //Clustering
    @FXML
    private fun getClustersPressed() {
        graphicComponents.physics.simulate()
    }

    //Vertex selection
    private fun retrieveVertexElement(lookupKey: String): E? {
        return stringToVMap[lookupKey.trim()]?.v
    }

    private fun getFromField(): E {
        return retrieveVertexElement(fromVertexField.text)
            ?: throw InvalidKeyException("user input: \"${fromVertexField.text}\" is not an existing vertex")
    }

    private fun getToField(): E {
        return retrieveVertexElement(toVertexField.text)
            ?: throw InvalidKeyException("user input: \"${toVertexField.text}\" is not an existing vertex")
    }

    @FXML
    private fun fromVertexChanged() {
    }

    @FXML
    private fun toVertexChanged() {
    }

    //Pathing
    @FXML
    private fun dijkstraPressed() {
        pathingButtonPressed(graph::path).let {
            printDijkstra(
                it.first.first,
                it.first.second,
                it.second,
                graph.distance(it.first.first, it.first.second),
                it.third
            )
        }
    }

    @FXML
    private fun bfsPressed() {
        pathingButtonPressed(graph::path).let { printBfs(it.first.first, it.first.second, it.second, it.third) }
    }

    @FXML
    private fun dfsPressed() {
        pathingButtonPressed(graph::path).let { printDfs(it.first.first, it.first.second, it.second, it.third) }
    }

    private fun pathingButtonPressed(algorithm: (E, E) -> List<E>): Triple<Pair<E, E>, List<E>, Long> {
        val from = getFromField()
        val to = getToField()
        val path: List<E>
        val time = measureTimeMillis {
            path = algorithm(from, to)
        }
        return Triple((from to to), path, time)
    }
}