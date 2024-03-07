package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.Slider
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
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
    private lateinit var connectednessField: TextField

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
            append("Clusters (connectedness: $connectedness)\n")
            for (cluster in clusters) {
                append("${cluster.getVertices()}\n")
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
        colorPath(path)
        return Triple((from to to), path, time)
    }

    private fun colorPath(path: List<E>) {
        graphicComponents.currentPathVertices.clear()
        for(v in graphicComponents.vertices){
            if(v.v in path){
                graphicComponents.currentPathVertices.add(v)
            }
        }

        graphicComponents.currentPathConnections.clear()
        for(edge in graphicComponents.edges){
            val c1 = edge.v1tov2Connection
            val c2 = edge.v2tov1Connection
            for((v1,v2) in graphicComponents.currentPathVertices.dropLast(1).zip(graphicComponents.currentPathVertices.drop(1))){
                if(edge.v1 == v1 && edge.v2 == v2){
                    graphicComponents.currentPathConnections.add(c1)
                }
                else if (edge.v1 == v2 && edge.v2 == v1){
                    graphicComponents.currentPathConnections.add(c2)
                }
            }
        }

        graphicComponents.greyEverything()



        graphicComponents.makePathFancyColors()

        //color graphicComponent lists

        //add listener for clicks on grey nodes

    }


    //Clustering
    @FXML
    private fun getClustersPressed() {
        val connectedness = connectednessField.text.toDouble()
        printClusters(graph.getClusters(connectedness), connectedness)
    }
}