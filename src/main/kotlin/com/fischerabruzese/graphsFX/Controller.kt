package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import com.fischerabruzese.graph.Graph
import javafx.application.Platform
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import java.text.NumberFormat
import kotlin.math.ln
import kotlin.math.pow
import kotlin.system.measureNanoTime


class Controller {
    //Constants
    companion object {
        val PATH_START: Color = Color.ORANGE
        val PATH_END: Color = Color.rgb(207, 3, 252)
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
    private lateinit var clusteringToggle: CheckBox
    @FXML
    private lateinit var mergeSinglesToggle: CheckBox
    @FXML
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
    @FXML
    private lateinit var clusteringProgress: ProgressIndicator

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
        updateClusterColoringAsync()
    }

    //Window title
    private fun setTitle(numVerts: Int = graph.size(),
                         numEdges: Int = graph.getEdges().size,
                         clusterInfo: ClusterInfo? = null
                         ) {
        val title = StringBuilder("Vertices: $numVerts | Edges: $numEdges")
        if(clusterInfo != null) title.append(" | Clusters: ${clusterInfo.clusters.size} | Kargerness: ${clusterInfo.kargerness} (${String.format("%.1f",clusterInfo.confidence*100)}%)")
        stage.title = title.toString()
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
    private fun printClusters(info: ClusterInfo) {
        val title = "Clusters (connectedness: ${NumberFormat.getNumberInstance().format(info.connectedness)})"
        val text = buildString {
            val sortedClusters = info.clusters.sortedByDescending { it.size() }
            for (cluster in sortedClusters) {
                val sortedVertices = cluster.getVertices().sortedBy { it.toString() }
                append("\nSize ${sortedVertices.size}: ${sortedVertices}\n")
            }
            append("\nTime(ns): ${NumberFormat.getIntegerInstance().format(info.time ?: "untimed")}")
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
    private fun calculateNumRuns(numVerts: Int, pDesired: Double): Int {
        val pMinCutSuccess = 1.0 / (numVerts * numVerts / 2 - numVerts / 2)
        val requiredIterations = ln(1 - pDesired) / ln(1 - pMinCutSuccess)
        return requiredIterations.toInt()
    }
    private fun confidenceAfterIterations(numVerts: Int, iterations: Int): Double {
        val pMinCutSuccess = 1.0 / (numVerts * numVerts / 2 - numVerts / 2)
        val confidence = 1 - (1 - pMinCutSuccess).pow(iterations.toDouble())
        return confidence
    }

    private data class ClusterInfo(
        val clusters: Collection<Graph<Any>>,
        val connectedness: Double,
        val kargerness: Int,
        val confidence: Double,
        val time: Long? = null
    )

    private fun getClusters(): ClusterInfo {
        val connectedness = connectednessSlider.value
        val numRuns = 100//calculateNumRuns(graph.size(),0.995).coerceIn(1..100000)
        val clusters: Collection<Graph<Any>>
        val time = measureNanoTime {
            clusters = if(mergeSinglesToggle.isSelected) graph.getClusters(connectedness, numRuns)
            else graph.highlyConnectedSubgraphs(connectedness, numRuns)
        }
        val info = ClusterInfo(clusters, connectedness, numRuns, confidenceAfterIterations(graph.size(), numRuns), time)
        Platform.runLater{
            setTitle(clusterInfo = info)
        }
        return info
    }

    private var clusteringThread: Thread? = null
    @FXML
    private fun printClustersPressed() {
        var clustersInfo: ClusterInfo
        clusteringThread?.interrupt()
        clusteringProgress(1)
        Thread(controllerSubroutines, {
            clusteringThread?.join()
            clusteringThread = Thread.currentThread()

            try{ clustersInfo = getClusters() }
            catch (_: InterruptedException){
                Platform.runLater{
                    clusteringProgress(-1)
                }
                return@Thread
            }
            Platform.runLater {
                printClusters(clustersInfo)
                graphicComponents.colorClusters(clustersInfo.clusters)
                clusteringProgress(-1)
            }
        }, "Clustering Thread (Printing)").start()
    }

    private fun updateClusterColoringAsync(){
        if(clusteringToggle.isSelected){
            clusteringThread?.interrupt()
            clusteringProgress(1)
            Thread(controllerSubroutines, {
                clusteringThread?.join()
                clusteringThread = Thread.currentThread()

                val clusters: Collection<Graph<Any>>
                try{ clusters = getClusters().clusters }
                catch (_: InterruptedException){
                    Platform.runLater{
                        clusteringProgress(-1)
                    }
                    return@Thread
                }
                Platform.runLater{
                    graphicComponents.colorClusters(clusters)
                    clusteringProgress(-1)
                }
            }, "Clustering Thread (Coloring)").start()
        } else {
            graphicComponents.clearClusterColoring()
        }
    }

    private var numClusterTasks = 0
    private var completedClusterTasks = 0
    private fun clusteringProgress(i: Int) {
        when(i){
            1 -> numClusterTasks++
            -1 -> completedClusterTasks++
        }
        clusteringProgress.progress = completedClusterTasks.toDouble()/numClusterTasks
        if(clusteringProgress.progress == 1.0){
            completedClusterTasks = 0
            numClusterTasks = 0
        }
    }

    private fun initializeClusterConnectednessSlider(){
        connectednessSlider.valueProperty().addListener { _, _, _ ->
            updateClusterColoringAsync()
        }
    }

    @FXML
    private fun clusteringToggled(){
        updateClusterColoringAsync()
    }

    @FXML
    private fun mergeSinglesToggled(){
        updateClusterColoringAsync()
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

            when(state){
                SwitchButton.SwitchButtonState.RIGHT -> {
                    generateRandomGraph()
                }
                SwitchButton.SwitchButtonState.LEFT -> {
                    generateClusteredGraph()
                }
            }
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

        tellPlatformRandomizationFinished(min,max)
    }

    private fun tellPlatformRandomizationFinished(min: Int, max: Int) {
        Platform.runLater {
            graphicComponents.graph = this.graph
            graphicComponents.draw()
            if(min == 0 && max == 1)
                graphicComponents.hideWeight()
            graphicComponents.physicsC.startSimulation()
            updateClusterColoringAsync()
        }
    }

    private fun generateRandomGraph() {
        val (min, max) = getEdgeWeights() ?: return
        val probConn = probOfConnectionsField.text.toDouble()
        this.graph.randomize(probConn, min, max)

        if(!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)

        tellPlatformRandomizationFinished(min, max)
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
        val p = probOfConnectionsField.text

        if(p.toDoubleOrNull() != null) {
            if(p.toDouble() > 1.0)
                probOfConnectionsField.text = 1.0.toString()

            else if(p.toDouble() < 0.0)
                probOfConnectionsField.text = 0.0.toString()
        }

        val v = vertexCountField.text.toInt()
        avgConnPerVertexField.text = p.toDoubleOrNull()?.let {
            ((2*it*(v -1)) + (if(!allowDisjointSelectionBox.isSelected) ((v-1)/ v).toDouble() else 0.0)).toString()
        } ?: ""
    }

    @FXML
    private fun avgConnectionsPerVertexEdited() {
        val v = vertexCountField.text.toInt()
        val a = avgConnPerVertexField.text
        probOfConnectionsField.text = a.toDoubleOrNull()?.let {
            ((it - if(!allowDisjointSelectionBox.isSelected) ((v-1.0)/ v) else 0.0) / (2*(v -1))).toString()
        } ?: ""

        val p = probOfConnectionsField.text
        if(p.toDoubleOrNull() != null) {
            if(p.toDouble() > 1.0 || p.toDouble() < 0.0) {
                probOfConnectionsEdited() //illegal edit was made, this method does data validation
            }
        }
    }
}