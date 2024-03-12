package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import java.text.NumberFormat
import kotlin.system.measureNanoTime

class Controller<E: Any> {
    //Pane
    @FXML
    private lateinit var pane: Pane
    private lateinit var paneWidth: ReadOnlyDoubleProperty
    private lateinit var paneHeight: ReadOnlyDoubleProperty
    private lateinit var graphicComponents: GraphicComponents<E>

    //User inputs
    @FXML
    private lateinit var clusterRandomizationSwitchHBox: HBox
    @FXML
    private lateinit var probabilityField: TextField
    @FXML
    private lateinit var minWeightField: TextField
    @FXML
    private lateinit var maxWeightField: TextField
    @FXML
    private lateinit var allowDisjointToggle: CheckBox
    @FXML
    private lateinit var physicsSlider: Slider
    @FXML
    private lateinit var fromVertexField: TextField
    @FXML
    private lateinit var toVertexField: TextField
    @FXML
    private lateinit var connectednessSlider: Slider
    @FXML
    private lateinit var clusterColoringToggle: CheckBox

    //Console
    @FXML
    private lateinit var console: TextArea
    private val CONSOLE_LINE_SEPARATOR = "-".repeat(20) + "\n"

    //Data
    private lateinit var graph: Graph<E>
    private val stringToVMap = HashMap<String, GraphicComponents<E>.Vertex>()

    //Window initialization
    @FXML
    fun initialize() {
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    //Initialization for anything involving the graph
    fun initializeGraph(graph: Graph<E>) {
        this.graph = graph
        graphicComponents = GraphicComponents(graph, pane, stringToVMap)
        initializePhysicsSlider()
        initializeVertexSelection()
        clusterRandomizationSwitchHBox.children.addAll(
            Label("Cluster random"),
            SwitchButton(),
            Label("Pure random")

        )
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
    private fun printClusters(clusters: Collection<Graph<E>>, connectedness: Double) {
        console.text = buildString {
            append("Clusters (connectedness: ${NumberFormat.getNumberInstance().format(connectedness)})\n")
            for (cluster in clusters) {
                append("${cluster.getVertices()}\n\n")
            }
            append(CONSOLE_LINE_SEPARATOR)
        } + console.text
    }

    private fun printDijkstra(from: E, to: E, path: List<E>, distance: Int, time: Long) {
        console.text = buildString {
            append("Dijkstra from $from to $to\n")
            append("Path: $path\n")
            append("Distance: $distance\n")
            append("Time(ns): ${NumberFormat.getIntegerInstance().format(time)}\n")
            append(CONSOLE_LINE_SEPARATOR)
        } + console.text
    }

    private fun printBfs(from: E, to: E, path: List<E>, time: Long) {
        console.text = buildString {
            append("Breadth first search from $from to $to\n")
            append(pathingString(path, time))
            append(CONSOLE_LINE_SEPARATOR)
        } + console.text
    }

    private fun printDfs(from: E, to: E, path: List<E>, time: Long) {
        console.text = buildString {
            append("Depth first search from $from to $to\n")
            append(pathingString(path, time))
            append(CONSOLE_LINE_SEPARATOR)
        } + console.text
    }

    private fun pathingString(path: List<E>, time: Long): String{
        return buildString {
            append("Path: $path\n")
            append("Time(ns): ${NumberFormat.getIntegerInstance().format(time)}\n")
        }
    }

    //Randomization
    @FXML
    private fun randomizePressed() {
    }

    //Physics
    private fun initializePhysicsSlider(){
        physicsSlider.valueProperty().addListener { _, _, newValue ->
            newValue?.let {
                if(it.toDouble() < 0.02) graphicComponents.physics.on = false
                else {
                    graphicComponents.physics.speed = it.toDouble()
                    if (!graphicComponents.physics.on) {
                        graphicComponents.physics.on = true
                        graphicComponents.physics.simulate()
                    }
                }
            }
        }
    }

    //Vertex selection
    private fun initializeVertexSelection() {
        fromVertexField.textProperty().addListener { _, oldValue, newValue ->
            stringToVMap[oldValue]?.run{
                clearOutline()
            }
            stringToVMap[newValue]?.run{
                setOutline(Color.ORANGE)
            }
        }
        toVertexField.textProperty().addListener { _, oldValue, newValue ->
            stringToVMap[oldValue]?.run{
                clearOutline()
            }
            stringToVMap[newValue]?.run{
                setOutline(Color.rgb(207, 3, 252))
            }
        }
    }
    private fun retrieveVertexElement(lookupKey: String): E? {
        return stringToVMap[lookupKey]?.v
    }
    //throw InvalidKeyException("user input: \"${fromVertexField.text}\" is not an existing vertex")

    private fun getFromField(): E? {
        return retrieveVertexElement(fromVertexField.text)
    }

    private fun getToField(): E? {
        return retrieveVertexElement(toVertexField.text)
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
        pathingButtonPressed(graph::path)?.let {
            printDijkstra(
                from = it.first.first,
                to = it.first.second,
                path = it.second,
                distance = graph.distance(it.first.first, it.first.second),
                time = it.third
            )
        }
    }

    @FXML
    private fun bfsPressed() {
        pathingButtonPressed(graph::path)?.let { printBfs(it.first.first, it.first.second, it.second, it.third) }
    }

    @FXML
    private fun dfsPressed() {
        pathingButtonPressed(graph::path)?.let { printDfs(it.first.first, it.first.second, it.second, it.third) }
    }

    private fun pathingButtonPressed(algorithm: (E, E) -> List<E>): Triple<Pair<E, E>, List<E>, Long>? {
        val from = getFromField()
        val to = getToField()
        from?:return null; to?:return null
        val path: List<E>

        val time = measureNanoTime {
            path = algorithm(from, to)
        }
        graphicComponents.colorPath(path)
        return Triple((from to to), path, time)
    }



    //Clustering
    @FXML
    private fun printClustersPressed() {
        val connectedness = connectednessSlider.value
        printClusters(graph.getClusters(connectedness), connectedness)
    }
}