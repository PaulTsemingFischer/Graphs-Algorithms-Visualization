package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import com.fischerabruzese.graph.Graph
import javafx.application.Platform
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import java.text.NumberFormat
import kotlin.math.ln
import kotlin.system.measureNanoTime

class Controller {
    //Constants
    companion object {
        val PATH_START = Color.ORANGE
        val PATH_END = Color.rgb(207, 3, 252)
    }

    //Pane
    @FXML
    private lateinit var pane: Pane
    private lateinit var paneWidth: ReadOnlyDoubleProperty
    private lateinit var paneHeight: ReadOnlyDoubleProperty
    private lateinit var graphicComponents: GraphicComponents<Any>

    //User inputs
    @FXML
    private lateinit var clusterRandomizationSwitchHBox: HBox
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

    private lateinit var switchButton: SwitchButton
    @FXML
    private lateinit var pureRandGridPane: GridPane
    @FXML
    private lateinit var clusterRandGridPane: GridPane
    @FXML
    private lateinit var clusterCountTextBox: TextField
    @FXML
    private lateinit var intraConnectednessSlider: Slider
    @FXML
    private lateinit var interConnectednessSlider: Slider
    @FXML
    private lateinit var allowDisjointSelectionBox: CheckBox
    @FXML
    private lateinit var minWeightTextBox: TextField
    @FXML
    private lateinit var maxWeightTextBox: TextField
    @FXML
    private lateinit var vertexCountField: TextField
    @FXML
    private lateinit var avgConnPerVertexField: TextField
    @FXML
    private lateinit var probOfConnectionsField: TextField

    //Console
    @FXML
    private lateinit var console: TextFlow
    private val CONSOLE_LINE_SEPARATOR = "-".repeat(20) + "\n"

    //Stage
    private lateinit var stage: Stage

    //Data
    private lateinit var graph: Graph<Any>

    private val controllerSubroutines = ThreadGroup("Controller Subroutines")

    //Window initialization
    @FXML
    fun initialize() {
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    //Initialization for anything involving the graph
    fun<E: Any> initializeGraph(graph: Graph<E>, stage: Stage) {
        //Stage
        this.stage = stage

        //Graphic components
        this.graph = graph.mapVertices {it as Any}
        graphicComponents = GraphicComponents(this.graph, pane) //Create the graphic components
        graphicComponents.draw() //Draw the graphic components

        //Controls
        initializeClusterRandomizationSwitch()
        initializePhysicsSlider()
        initializeVertexSelection()
        initializeClusterConnectednessSlider()
        switchButton.switchedEvents.addLast { switchSwitched(it) }
        switchSwitched(SwitchButton.SwitchButtonState.LEFT) //initialize properties in specific graphic

        //Misc
        updateClusterColoring()
    }

    //Window title
    private fun setTitle(numVerts: Int = graph.size(),
                         numEdges: Int = graph.getEdges().size,
                         numClusters: Int = graph.getClusters().size,
                         kargerness: Int = calculateNumRuns(graph.size(), 0.995)
                         ) {
        stage.title = "Vertices: $numVerts | Edges: $numEdges | Clusters: $numClusters | Kargerness: $kargerness"
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
        //General console printing
    private fun queuePrintEntry(color: Color = Color.BLACK, text: String?, title: String? = null, titleSeparator: String = "\n"){
        Platform.runLater {
            val coloredTitle =
                title?.let { Text(it + titleSeparator).apply { fill = color; style = "-fx-font-weight: bold" } }
            val coloredText = text?.let { Text(it + "\n").apply { fill = color } }
            console.children.addAll(0, listOfNotNull(coloredTitle, coloredText, Text(CONSOLE_LINE_SEPARATOR)))
        }
    }

        //Error printing
    private enum class ErrorType {
        VERTEX_COUNT, CLUSTER_COUNT, EDGEWEIGHT_MIN, EDGEWEIGHT_MAX, FROM_VERTEX, TO_VERTEX
    }
    private fun printError(error:  ErrorType) {
        val errorName = when(error) {
            ErrorType.VERTEX_COUNT -> "Invalid vertex count"
            ErrorType.CLUSTER_COUNT -> "Invalid cluster count"
            ErrorType.EDGEWEIGHT_MIN -> "Invalid minimum edge weight"
            ErrorType.EDGEWEIGHT_MAX -> "Invalid maximum edge weight"
            ErrorType.FROM_VERTEX -> "Invalid from vertex"
            ErrorType.TO_VERTEX -> "Invalid to vertex"
        }
        val errorDescription = when(error) {
            ErrorType.VERTEX_COUNT -> "'vertex count' requires: Int, >= 0"
            ErrorType.CLUSTER_COUNT -> "'cluster count' requires: Int, >= 0, <= 'vertex count'"
            ErrorType.EDGEWEIGHT_MIN -> "'min edge weight' requires: Int or blank, >= 0"
            ErrorType.EDGEWEIGHT_MAX -> "'max edge weight' requires: Int  or blank, >= 'min edge weight'"
            ErrorType.FROM_VERTEX -> null
            ErrorType.TO_VERTEX -> null
        }
        queuePrintEntry(Color.RED, errorDescription, errorName)
    }

        //Cluster console printing
    private fun printClusters(clusters: Collection<Graph<Any>>, connectedness: Double, time: Long) {
        val title = "Clusters (connectedness: ${NumberFormat.getNumberInstance().format(connectedness)})"
        val text = buildString {
            val sortedClusters = clusters.sortedByDescending { it.size() }
            for (cluster in sortedClusters) {
                val sortedVertices = cluster.getVertices().sortedBy { it.toString() }
                append("\nSize ${sortedVertices.size}: ${sortedVertices}\n")
            }
            append("\nTime(ns): ${NumberFormat.getIntegerInstance().format(time)}")
        }
        queuePrintEntry(Color.BLUE, text, title)
    }

        //Pathing console printing
    private fun printDijkstra(from: Any, to: Any, path: List<Any>, distance: Int, time: Long) {
        val title = "Dijkstra from $from to $to"
        val text = buildString {
            append("Path: $path\n")
            append("Distance: $distance\n")
            append("Time(ns): ${NumberFormat.getIntegerInstance().format(time)}")
        }
        queuePrintEntry(Color.DEEPSKYBLUE, text, title)
    }

    private fun printBfs(from: Any, to: Any, path: List<Any>, time: Long) {
        val title = "Breadth first search from $from to $to"
        val text = pathingString(path, time)
        queuePrintEntry(Color.DEEPSKYBLUE, text, title)
    }

    private fun printDfs(from: Any, to: Any, path: List<Any>, time: Long) {
        val title = "Depth first search from $from to $to"
        val text = pathingString(path, time)
        queuePrintEntry(Color.DEEPSKYBLUE, text, title)
    }

    private fun pathingString(path: List<Any>, time: Long): String{
        return buildString {
            append("Path: $path\n")
            append("Time(ns): ${NumberFormat.getIntegerInstance().format(time)}")
        }
    }

    //Physics
    private fun initializePhysicsSlider(){
        physicsSlider.valueProperty().addListener { _, _, newValue ->
            newValue?.let {
                if(it.toDouble() < 0.02)
                    graphicComponents.physicsC.stopSimulation()
                else {
                    graphicComponents.physicsC.speed = it.toDouble()
                    graphicComponents.physicsC.startSimulation()
                }
            }
        }
    }

    //Vertex selection
    private fun initializeVertexSelection() {
        fromVertexField.textProperty().addListener { _, oldValue, newValue ->
            graphicComponents.stringToVMap[oldValue]?.run{
                clearOutline()
            }
            graphicComponents.stringToVMap[newValue]?.run{
                setOutline(PATH_START)
            }
        }
        toVertexField.textProperty().addListener { _, oldValue, newValue ->
            graphicComponents.stringToVMap[oldValue]?.run{
                clearOutline()
            }
            graphicComponents.stringToVMap[newValue]?.run{
                setOutline(PATH_END)
            }
        }
    }
    private fun retrieveVertexElement(lookupKey: String): Any? {
        return graphicComponents.stringToVMap[lookupKey]?.v
    }
    //throw InvalidKeyException("user input: \"${fromVertexField.text}\" is not an existing vertex")

    private fun getFromField(): Any? {
        return retrieveVertexElement(fromVertexField.text)
    }

    private fun getToField(): Any? {
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

    private fun pathingButtonPressed(algorithm: (Any, Any) -> List<Any>): Triple<Pair<Any, Any>, List<Any>, Long>? {
        val from = getFromField()
        val to = getToField()
        if(from == null){
            printError(ErrorType.FROM_VERTEX)
            return null
        }
        if(to == null){
            printError(ErrorType.TO_VERTEX)
            return null
        }

        val path: List<Any>

        val time = measureNanoTime {
            path = algorithm(from, to)
        }
        graphicComponents.colorPath(path)
        return Triple((from to to), path, time)
    }

    //Clustering
    private fun calculateNumRuns(n: Int, pDesired: Double): Int {
        val p = 1.0 / (n * n / 2 - n / 2)
        val t = ln(1 - pDesired) / ln(1 - p)
        return t.toInt()
    }
    private fun getClusters(): Pair<Collection<Graph<Any>>, Double> {
        val connectedness = connectednessSlider.value
        val numRuns = calculateNumRuns(graph.size(),0.995)
        val clusters = graph.getClusters(connectedness, numRuns)
        setTitle(numClusters = clusters.size, kargerness = numRuns)
        return Pair(clusters, connectedness)
    }



    @FXML
    private fun printClustersPressed() {
        val clusters: Pair<Collection<Graph<Any>>, Double>
        val time = measureNanoTime {
            clusters = getClusters()
        }
        printClusters(clusters.first, clusters.second, time)
    }

    private fun updateClusterColoring(){
        if(clusterColoringToggle.isSelected){
            graphicComponents.colorClusters(getClusters().first)
        }else{
            graphicComponents.clearClusterColoring()
        }
    }

    private fun initializeClusterConnectednessSlider(){
        connectednessSlider.valueProperty().addListener { _, _, _ ->
            updateClusterColoring()
        }
    }

    @FXML
    private fun clusterColoringToggled(){
        updateClusterColoring()
    }

    //Randomization
    private fun initializeClusterRandomizationSwitch(){
        clusterRandomizationSwitchHBox.children.addAll(
            Label("Cluster Rand ").apply { textFill = Color.WHITE },
            SwitchButton().also { switchButton = it },
            Label(" Pure Rand").apply { textFill = Color.WHITE }
        )
    }

    private fun switchSwitched(state: SwitchButton.SwitchButtonState) {
        when(state){
            SwitchButton.SwitchButtonState.LEFT -> {
                pureRandGridPane.opacity = 0.0
                pureRandGridPane.disableProperty().set(true)
                clusterRandGridPane.opacity = 1.0
                clusterRandGridPane.disableProperty().set(false)
            }
            SwitchButton.SwitchButtonState.RIGHT -> {
                clusterRandGridPane.opacity = 0.0
                clusterRandGridPane.disableProperty().set(true)
                pureRandGridPane.opacity = 1.0
                pureRandGridPane.disableProperty().set(false)
            }
        }
    }

    @FXML
    private fun randomizePressed(){
        val state = switchButton.state

        Thread(controllerSubroutines, {
            //Validate vertex count
            val vertexCount = try {
                vertexCountField.text.toInt().also {
                    if (it < 0) throw IllegalArgumentException()
                }
            } catch(e: Exception){
                printError(ErrorType.VERTEX_COUNT)
                return@Thread
            }

            this.graph = AMGraph.fromCollection((0 until vertexCount).toList())
            graphicComponents.graph = this.graph

            when(state){
                SwitchButton.SwitchButtonState.RIGHT -> {
                    generateRandomGraph()
                }
                SwitchButton.SwitchButtonState.LEFT -> {
                    generateClusteredGraph()
                }
            }
            graphicComponents.physicsC.startSimulation()
        }, "Graph Creator").start()
    }

    private fun generateClusteredGraph() {
        //Validate cluster count
        val clusterCount = try{
            clusterCountTextBox.text.toInt().also {
                if(it < 0 || it > graph.size()) throw IllegalArgumentException()
            }
        } catch(e: Exception){
            printError(ErrorType.CLUSTER_COUNT)
            return
        }
        val interConn = interConnectednessSlider.value
        val intraConn = intraConnectednessSlider.value

        val (min, max) = getEdgeWeights() ?: return
        this.graph.randomizeWithCluster(clusterCount, min, max, intraConn, interConn)

        if(!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)

        Platform.runLater { graphicComponents.draw()
            if(min == 0 && max == 1)
                graphicComponents.hideWeight()
            updateClusterColoring()
        }
    }

    private fun generateRandomGraph() {
        val (min, max) = getEdgeWeights() ?: return
        val probConn = probOfConnectionsField.text.toDouble()
        this.graph.randomize(probConn, min, max)

        if(!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)

        Platform.runLater { graphicComponents.draw()
            if(min == 0 && max == 1)
                graphicComponents.hideWeight()
            updateClusterColoring()
        }
    }

    //Return: null if error, (0, 1) if unweighted
    private fun getEdgeWeights(): Pair<Int, Int>? {
        //Validate min
        var min: Int = try {
            val text = minWeightTextBox.text
            if(text.isEmpty()) 0
            else {
                text.toInt().also {
                    if (it < 0) throw IllegalArgumentException()
                }
            }
        } catch (e: Exception) {
            printError(ErrorType.EDGEWEIGHT_MIN)
            return null
        }

        //Validate max
        val max: Int = try {
            val text = maxWeightTextBox.text
            if(text.isEmpty()) 0.also{min = 0}
            else {
                text.toInt().also {
                    if (it < min) throw IllegalArgumentException()
                }
            }
        } catch (e: Exception) {
            printError(ErrorType.EDGEWEIGHT_MAX)
            return null
        }
        return Pair(min, max + 1)
    }
    
    @FXML
    private fun probOfConnectionsEdited() {
        if(probOfConnectionsField.text.toDoubleOrNull() != null) {
            if(probOfConnectionsField.text.toDouble() > 1.0)
                probOfConnectionsField.text = 1.0.toString()

            else if(probOfConnectionsField.text.toDouble() < 0.0)
                probOfConnectionsField.text = 0.0.toString()
        }

        avgConnPerVertexField.text = probOfConnectionsField.text.toDoubleOrNull()?.let {
            ((2*it*(graph.size()-1)) + (if(!allowDisjointSelectionBox.isSelected) ((graph.size()-1)/graph.size()).toDouble() else 0.0)).toString()
        } ?: ""
    }

    @FXML
    private fun avgConnectionsPerVertexEdited() {
        probOfConnectionsField.text = avgConnPerVertexField.text.toDoubleOrNull()?.let {
            ((it - if(!allowDisjointSelectionBox.isSelected) ((graph.size()-1.0)/graph.size()) else 0.0) / (2*(graph.size()-1))).toString()
        } ?: ""

        if(probOfConnectionsField.text.toDoubleOrNull() != null) {
            if(probOfConnectionsField.text.toDouble() > 1.0 || probOfConnectionsField.text.toDouble() < 0.0) {
                probOfConnectionsEdited() //illegal edit was made, this method does data validation
            }
        }
    }
}