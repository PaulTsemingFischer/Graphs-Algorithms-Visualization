package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import com.fischerabruzese.graph.Graph
import javafx.application.Platform
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import java.text.NumberFormat
import kotlin.random.Random
import kotlin.system.measureNanoTime


class Controller {
    //Constants
    companion object {
        val PATH_START: Color = Color.ORANGE
        val PATH_END: Color = Color.rgb(207, 3, 252)
        val CONSOLE_LINE_SEPARATOR = "-".repeat(20) + "\n"

    }

    //Pane
    @FXML
    private lateinit var pane: Pane
    private lateinit var paneWidth: ReadOnlyDoubleProperty
    private lateinit var paneHeight: ReadOnlyDoubleProperty
    internal lateinit var graphicComponents: GraphicComponents<Any>

    //Presets
    @FXML
    private lateinit var copyToClipboardButton: Button

    @FXML
    private lateinit var pastedGraph: Label

    @FXML
    private lateinit var pastedGraphHBox: HBox

    @FXML
    private lateinit var pastedGraphLabel: Label

    //Randomization
    @FXML
    private lateinit var clusterRandomizationSwitchHBox: HBox
    @FXML
    private lateinit var randSwitchButton: SwitchButton
    @FXML
    private lateinit var pureRandGridPane: GridPane
    @FXML
    private lateinit var clusterRandGridPane: GridPane
    @FXML
    private lateinit var vertexCountField: TextField
    @FXML
    private lateinit var clusterCountTextBox: TextField
    @FXML
    private lateinit var avgConnPerVertexField: TextField
    @FXML
    private lateinit var probOfConnectionsField: TextField
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

    //Clustering
    @FXML
    private lateinit var connectednessSlider: Slider
    @FXML
    private lateinit var clusteringToggle: CheckBox
    @FXML
    private lateinit var mergeSinglesToggle: CheckBox
    @FXML
    private lateinit var clusteringProgress: ProgressIndicator

    //Display Settings
    @FXML
    private lateinit var physicsSlider: Slider
    @FXML
    private lateinit var weightVisibilityHBox: HBox
    @FXML
    private lateinit var weightSwitchButton: SwitchButton
    @FXML

    //Pathing
    private lateinit var fromVertexField: TextField
    @FXML
    private lateinit var toVertexField: TextField

    //Console
    @FXML
    private lateinit var console: TextFlow

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
    fun <E : Any> initializeGraph(graph: Graph<E>, stage: Stage) {
        //Stage
        this.stage = stage

        //Graphic components
        this.graph = graph.mapVertices { it }
        graphicComponents = GraphicComponents(this.graph, pane) //Create the graphic components
        graphicComponents.draw() //Draw the graphic components

        //Controls

        emptyPaste()
        initializeClusterRandomizationSwitch()
        initializeWeightDisplaySwitch()
        initializePhysicsSlider()
        initializeVertexSelection()
        initializeClusterConnectednessSlider()
        randSwitchButton.switchedEvents.addLast { randSwitched(it) }
        randSwitchButton.manuallySwitch(newState = SwitchButton.SwitchButtonState.LEFT) //initialize properties in specific graphic
        weightSwitchButton.switchedEvents.addLast { weightSwitched(it) }
        weightSwitchButton.manuallySwitch(newState = SwitchButton.SwitchButtonState.RIGHT)

        //Misc
        updateClusterColoringAsync()
    }


    //Window title
    private fun setTitle(
        numVerts: Int = graph.size(),
        numEdges: Int = graph.getEdges().size,
        clusterInfo: ClusterInfo? = null
    ) {
        val title = StringBuilder("Vertices: $numVerts | Edges: $numEdges")
        if (clusterInfo != null) title.append(
            " | Clusters: ${clusterInfo.clusters.size} | Kargerness: ${clusterInfo.kargerness} (${
                String.format(
                    "%.3f",
                    clusterInfo.confidence * 100
                )
            }%)"
        )
        stage.title = title.toString()
    }

    //Preset printing
    /**
     * Prints a message that the [presetName] has loaded
     */
    private fun printPreset(presetName: String) {
        queuePrintEntry(Color.PURPLE, title = "Preset '$presetName' loaded")
    }

    //Preset actions
    private fun presetPressed(graph: Graph<Any>, preset: String) {
        this.graph = graph
        tellPlatformRandomizationFinished(false)
        printPreset(preset)
    }

    @FXML //Tree preset
    private fun preset1Pressed() {
        val random = Random(11)
        val vertices = ('A'..'K').toList()
        val presetGraph = AMGraph<Any>(vertices)
        presetGraph.randomizeWithCluster(2, 0, 10, 0.4, 0.02, false, random)
        presetPressed(presetGraph, "Small 2 Cluster")
    }

    @FXML
    private fun preset2Pressed() {
        val random = Random(26)
        val vertices = ('A'..'Z').toList()
        val presetGraph = AMGraph<Any>(vertices)
        presetGraph.randomizeWithCluster(4, 0, 10, 0.4, 0.01, false, random)
        presetPressed(presetGraph, "Medium 4 Cluster")
    }

    @FXML
    private fun preset3Pressed() {
        val random = Random(33)
        val vertices: List<String> =
            (('A'..'Z')
                    + ('A'..'Z').map { "A$it" }
                    + ('A'..'Z').map { "B$it" }
                    + ('A'..'Z').map { "C$it" }
                    ).toList().map { it.toString() }
        val presetGraph = AMGraph<Any>(vertices)
        presetGraph.randomizeWithCluster(4, 0, 10, 0.2, 0.004, false, random)
        presetPressed(presetGraph, "Large 4 Clusters")
    }

    @FXML
    private fun preset4Pressed() {
        val random = Random(42)
        val vertices: List<String> =
            (('A'..'Z')
                    + ('A'..'Z').map { "A$it" }
                    + ('A'..'Z').map { "B$it" }
                    + ('A'..'Z').map { "C$it" }
                    + ('A'..'Z').map { "D$it" }
                    + ('A'..'Z').map { "E$it" }
                    + ('A'..'Z').map { "F$it" }
                    + ('A'..'Z').map { "G$it" }
                    ).toList().map { it.toString() }
        val presetGraph = AMGraph<Any>(vertices)
        presetGraph.randomizeWithCluster(8, 0, 10, 0.2, 0.0003, false, random)
        presetPressed(presetGraph, "Massive 8 Cluster")
    }

    @FXML
    private fun preset5Pressed() {
        val random = Random(54)
        val vertices = ('A'..'K').toList()
        val presetGraph = AMGraph<Any>(vertices)
        presetGraph.randomize(0.15, 0, 10, true, random)
        presetPressed(presetGraph, "Small Pure Random")
    }

    @FXML
    private fun preset6Pressed() {
        val random = Random(66)
        val vertices: List<String> =
            (('A'..'Z')
                    + ('A'..'Z').map { "A$it" }
                    + ('A'..'Z').map { "B$it" }
                    + ('A'..'Z').map { "C$it" }
                    ).toList().map { it.toString() }
        val presetGraph = AMGraph<Any>(vertices)
        presetGraph.randomize(0.05, 0, 10, true, random)
        presetPressed(presetGraph, "Large Pure Random")
    }

    @FXML
    private fun preset7Pressed() {
        val random = Random(77)

        val vertices = ('A'..'S').toList()
        val presetGraph = AMGraph<Any>(vertices)

        for (i in 1 until vertices.size) {
            val parentIndex = (i - 1) / 2
            val vertex = vertices[i]
            val parentVertex = vertices[parentIndex]

            presetGraph[parentVertex, vertex] = random.nextInt(0, 10)
        }
        presetPressed(presetGraph, "Binary Tree")
    }

    @FXML
    private fun preset8Pressed() {
        val random = Random(88)

        val vertices = ('A'..'Z').toList()
        val presetGraph = AMGraph<Any>(vertices)

        for (i in 0 until vertices.size - 1) {
            val currentVertex = vertices[i]
            val nextVertex = vertices[i + 1]

            presetGraph[currentVertex, nextVertex] = random.nextInt(0, 10)
            presetGraph[nextVertex, currentVertex] = random.nextInt(0, 10)
        }

        presetPressed(presetGraph, "Double Linked List")
    }


    //Console Printing
    /**
     * Tells the JavaFX Application Thread to print a new entry to the console
     */
    private fun queuePrintEntry(
        color: Color = Color.BLACK,
        text: String? = null,
        title: String? = null,
        titleSeparator: String = "\n"
    ) {
        Platform.runLater {
            val coloredTitle =
                title?.let { Text(it + titleSeparator).apply { fill = color; style = "-fx-font-weight: bold" } }
            val coloredText = text?.let { Text(it + "\n").apply { fill = color } }
            console.children.addAll(0, listOfNotNull(coloredTitle, coloredText, Text(CONSOLE_LINE_SEPARATOR)))
        }
    }

    /**
     * Represents a type of Error that has occurred in this program
     */
    private enum class ErrorType {
        VERTEX_COUNT, CLUSTER_COUNT, EDGEWEIGHT_MIN, EDGEWEIGHT_MAX, FROM_VERTEX, TO_VERTEX
    }

    /**
     * Prints an error given the error type
     */
    private fun printError(error: ErrorType) {
        val errorName = when (error) {
            ErrorType.VERTEX_COUNT -> "Invalid vertex count"
            ErrorType.CLUSTER_COUNT -> "Invalid cluster count"
            ErrorType.EDGEWEIGHT_MIN -> "Invalid minimum edge weight"
            ErrorType.EDGEWEIGHT_MAX -> "Invalid maximum edge weight"
            ErrorType.FROM_VERTEX -> "Invalid from vertex"
            ErrorType.TO_VERTEX -> "Invalid to vertex"
        }
        val errorDescription = when (error) {
            ErrorType.VERTEX_COUNT -> "'vertex count' requires: Int, >= 0"
            ErrorType.CLUSTER_COUNT -> "'cluster count' requires: Int, >= 0, <= 'vertex count'"
            ErrorType.EDGEWEIGHT_MIN -> "'min edge weight' requires: Int or blank, >= 0"
            ErrorType.EDGEWEIGHT_MAX -> "'max edge weight' requires: Int  or blank, >= 'min edge weight'"
            ErrorType.FROM_VERTEX -> null
            ErrorType.TO_VERTEX -> null
        }
        queuePrintEntry(Color.RED, errorDescription, errorName)
    }


    //Physics
    private fun initializePhysicsSlider() {
        physicsSlider.valueProperty().addListener { _, _, newValue ->
            newValue?.let {
                graphicComponents.physicsC.run {
                    if (it.toDouble() < 0.02) {
                        if (isActive()) stopSimulation()
                    } else {
                        speed = it.toDouble()
                        if (!isActive())
                            startSimulation()
                    }
                }
            }
        }
    }

    //Vertex selection
    private fun initializeVertexSelection() {
        graphicComponents.stringToVMap.run {
            fromVertexField.textProperty().addListener { _, oldValue, newValue ->
                get(oldValue)?.clearOutline()
                get(newValue)?.setOutline(PATH_START)
            }
            toVertexField.textProperty().addListener { _, oldValue, newValue ->
                get(oldValue)?.clearOutline()
                get(newValue)?.setOutline(PATH_END)
            }
        }
    }

    private fun retrieveVertexElement(lookupKey: String): Any? {
        return graphicComponents.stringToVMap[lookupKey]?.v
    }

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

    //Pathing printing
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

    private fun pathingString(path: List<Any>, time: Long): String {
        return buildString {
            append("Path: $path\n")
            append("Time(ns): ${NumberFormat.getIntegerInstance().format(time)}")
        }
    }

    //Pathing actions
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
        if (from == null) {
            printError(ErrorType.FROM_VERTEX)
            return null
        }
        if (to == null) {
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

    //Cluster printing
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

    //Clustering actions
    private var clusteringThread: Thread? = null
    private var numClusterTasks = 0
    private var completedClusterTasks = 0

    private data class ClusterInfo(
        val clusters: Collection<Graph<Any>>,
        val connectedness: Double,
        val kargerness: Int,
        val confidence: Double,
        val time: Long? = null
    )

    private fun initializeClusterConnectednessSlider() {
        connectednessSlider.valueProperty().addListener { _, _, _ ->
            updateClusterColoringAsync()
        }
    }

    private fun getClusters(): ClusterInfo {
        val connectedness = connectednessSlider.value
        val numRuns = graph.estimateRequiredKargerness(0.997).coerceIn(0..10000)
        val clusters: Collection<Graph<Any>>
        val time = measureNanoTime {
            clusters = if (mergeSinglesToggle.isSelected) graph.getClusters(connectedness, numRuns)
            else graph.highlyConnectedSubgraphs(connectedness, numRuns)
        }
        val info = ClusterInfo(clusters, connectedness, numRuns, graph.estimateClusteringConfidence(numRuns), time)
        Platform.runLater {
            setTitle(clusterInfo = info)
        }
        return info
    }

    @FXML
    private fun printClustersPressed() {
        var clustersInfo: ClusterInfo
        clusteringThread?.interrupt()
        clusteringProgress(1)
        Thread(controllerSubroutines, {
            clusteringThread?.join()
            clusteringThread = Thread.currentThread()

            try {
                clustersInfo = getClusters()
            } catch (_: InterruptedException) {
                Platform.runLater {
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

    private fun updateClusterColoringAsync() {
        if (clusteringToggle.isSelected) {
            clusteringThread?.interrupt()
            clusteringProgress(1)
            Thread(controllerSubroutines, {
                clusteringThread?.join()
                clusteringThread = Thread.currentThread()

                val clusters: Collection<Graph<Any>>
                try {
                    clusters = getClusters().clusters
                } catch (_: InterruptedException) {
                    Platform.runLater {
                        clusteringProgress(-1)
                    }
                    return@Thread
                }
                Platform.runLater {
                    graphicComponents.colorClusters(clusters)
                    clusteringProgress(-1)
                }
            }, "Clustering Thread (Coloring)").start()
        } else {
            graphicComponents.clearClusterColoring()
        }
    }

    private fun clusteringProgress(i: Int) {
        when (i) {
            1 -> numClusterTasks++
            -1 -> completedClusterTasks++
        }
        clusteringProgress.progress = completedClusterTasks.toDouble() / numClusterTasks
        if (clusteringProgress.progress == 1.0) {
            completedClusterTasks = 0
            numClusterTasks = 0
        }
    }

    @FXML
    private fun clusteringToggled() {
        updateClusterColoringAsync()
    }

    @FXML
    private fun mergeSinglesToggled() {
        updateClusterColoringAsync()
    }

    //Randomization printing
    private fun printPureRandomization(pureRandomizeInfo: PureRandomizeInfo) {
        val title = "Pure randomization"
        val text = buildString {
            append("Vertex count: ${pureRandomizeInfo.vertexCount}\n")
            append("Average connections per vertex: ${pureRandomizeInfo.avgConnections}\n")
            append("Probability of connection: ${pureRandomizeInfo.probConnection}\n")
            append("Disjoint graph: ${pureRandomizeInfo.disjointGraph}\n")
            if (pureRandomizeInfo.min == 0 && pureRandomizeInfo.max == 1) {
                append("Unweighted")
            } else {
                append("Edge weights: [${pureRandomizeInfo.min} - ${pureRandomizeInfo.max}]")
            }
        }
        queuePrintEntry(Color.rgb(61, 224, 167), text, title)
    }

    private fun printClusterRandomization(clusterRandomizeInfo: ClusterRandomizeInfo) {
        val title = "Cluster randomization"
        val text = buildString {
            append("Vertices: ${clusterRandomizeInfo.vertexCount} | ")
            append("Clusters: ${clusterRandomizeInfo.clusterCount}\n")
            append("Intra-connectedness: ${clusterRandomizeInfo.intraConn}\n")
            append("Inter-connectedness: ${clusterRandomizeInfo.interConn}\n")
            append("Disjoint graph: ${clusterRandomizeInfo.disjointGraph}\n")
            if (clusterRandomizeInfo.min == 0 && clusterRandomizeInfo.max == 1) {
                append("Unweighted")
            } else {
                append("Edge weights: [${clusterRandomizeInfo.min} - ${clusterRandomizeInfo.max}]")
            }

        }
        queuePrintEntry(Color.DEEPPINK, text, title)
    }

    //Randomization Switch
    private fun initializeClusterRandomizationSwitch() {
        clusterRandomizationSwitchHBox.children.addAll(
            Label("Cluster Rand ").apply { textFill = Color.WHITE },
            SwitchButton().also { randSwitchButton = it },
            Label(" Pure Rand").apply { textFill = Color.WHITE }
        )
    }

    private fun initializeWeightDisplaySwitch() {
        weightVisibilityHBox.children.addAll(
            Label("Hide Weights ").apply { textFill = Color.WHITE },
            SwitchButton().also { weightSwitchButton = it },
            Label(" Show Weights").apply { textFill = Color.WHITE }
        )
    }

    private fun randSwitched(state: SwitchButton.SwitchButtonState) {
        when (state) {
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

    private fun weightSwitched(state: SwitchButton.SwitchButtonState) {
        when (state) {
            SwitchButton.SwitchButtonState.LEFT -> {
                graphicComponents.hideWeight()
            }

            SwitchButton.SwitchButtonState.RIGHT -> {
                graphicComponents.showWeight()
            }
        }
    }

    //Randomization actions
    @FXML
    private fun randomizePressed() {
        val state = randSwitchButton.state

        Thread(controllerSubroutines, {
            //Validate vertex count
            val vertexCount = try {
                vertexCountField.text.toInt().also {
                    if (it < 0) throw IllegalArgumentException()
                }
            } catch (e: Exception) {
                printError(ErrorType.VERTEX_COUNT)
                return@Thread
            }

            this.graph = AMGraph((0 until vertexCount).toList())

            when (state) {
                SwitchButton.SwitchButtonState.RIGHT -> {
                    generateRandomGraph()?.let { printPureRandomization(it) }
                }

                SwitchButton.SwitchButtonState.LEFT -> {
                    generateClusteredGraph()?.let { printClusterRandomization(it) }
                }
            }
        }, "Graph Creator").start()
    }

    private data class ClusterRandomizeInfo(
        val vertexCount: Int,
        val clusterCount: Int,
        val intraConn: Double,
        val interConn: Double,
        val disjointGraph: Boolean,
        val min: Int,
        val max: Int
    )

    private data class PureRandomizeInfo(
        val vertexCount: Int,
        val avgConnections: Double,
        val probConnection: Double,
        val disjointGraph: Boolean,
        val min: Int,
        val max: Int
    )

    private fun tellPlatformRandomizationFinished(hideWeight: Boolean) {
        Platform.runLater {
            graphicComponents.graph = this.graph
            graphicComponents.draw()
            if (hideWeight)
                weightSwitchButton.manuallySwitch(newState = SwitchButton.SwitchButtonState.LEFT)
            graphicComponents.physicsC.startSimulation()
            updateClusterColoringAsync()
        }
    }

    //Null represents a failed randomization
    private fun generateClusteredGraph(): ClusterRandomizeInfo? {
        //Validate cluster count
        val clusterCount = try {
            clusterCountTextBox.text.toInt().also {
                if (it < 0 || it > graph.size()) throw IllegalArgumentException()
            }
        } catch (e: Exception) {
            printError(ErrorType.CLUSTER_COUNT)
            return null
        }
        val interConn = interConnectednessSlider.value
        val intraConn = intraConnectednessSlider.value

        val (min, max) = getEdgeWeights() ?: return null
        this.graph.randomizeWithCluster(
            clusterCount,
            min,
            max,
            intraConn,
            interConn,
            allowDisjointSelectionBox.isSelected
        )

        if (!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)

        tellPlatformRandomizationFinished(min == 0 && max == 1)
        return ClusterRandomizeInfo(
            graph.size(),
            clusterCount,
            intraConn,
            interConn,
            allowDisjointSelectionBox.isSelected,
            min,
            max
        )
    }

    //Null represents a failed randomization
    private fun generateRandomGraph(): PureRandomizeInfo? {
        val (min, max) = getEdgeWeights() ?: return null
        val probConn = probOfConnectionsField.text.toDouble()
        this.graph.randomize(probConn, min, max, allowDisjointSelectionBox.isSelected)

        tellPlatformRandomizationFinished(min == 0 && max == 1)
        return PureRandomizeInfo(
            graph.size(),
            probConn,
            probConn,
            allowDisjointSelectionBox.isSelected,
            min,
            max
        )
    }

    //Return: null if error, (0, 1) if unweighted
    private fun getEdgeWeights(): Pair<Int, Int>? {
        //Validate min
        var min: Int = try {
            val text = minWeightTextBox.text
            if (text.isEmpty()) 0
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
            if (text.isEmpty()) 0.also { min = 0 }
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

        if (p.toDoubleOrNull() != null) {
            if (p.toDouble() > 1.0)
                probOfConnectionsField.text = 1.0.toString()
            else if (p.toDouble() < 0.0)
                probOfConnectionsField.text = 0.0.toString()
        }

        val v = vertexCountField.text.toInt()
        avgConnPerVertexField.text = p.toDoubleOrNull()?.let {
            ((2 * it * (v - 1)) + (if (!allowDisjointSelectionBox.isSelected) ((v - 1) / v).toDouble() else 0.0)).toString()
        } ?: ""
    }

    @FXML
    private fun avgConnectionsPerVertexEdited() {
        val v = vertexCountField.text.toInt()
        val a = avgConnPerVertexField.text
        probOfConnectionsField.text = a.toDoubleOrNull()?.let {
            ((it - if (!allowDisjointSelectionBox.isSelected) ((v - 1.0) / v) else 0.0) / (2 * (v - 1))).toString()
        } ?: ""

        val p = probOfConnectionsField.text
        if (p.toDoubleOrNull() != null) {
            if (p.toDouble() > 1.0 || p.toDouble() < 0.0) {
                probOfConnectionsEdited() //illegal edit was made, this method does data validation
            }
        }
    }

    private var currentPastedGraph: AMGraph<*>? = null

    //Copy-paste actions
    @FXML
    private fun copyToClipboardPressed() {
        val content = ClipboardContent()
        content[DataFormat.PLAIN_TEXT] = graph.getKey()
        if (Clipboard.getSystemClipboard().setContent(content))
            copyToClipboardButton.text = "Copied!"
        else
            copyToClipboardButton.text = "Not Copied :("
        Thread {
            Thread.sleep(3000)
            Platform.runLater {
                copyToClipboardButton.text = "Copy to Clipboard"
            }
        }.start()
    }

    @FXML
    private fun pasteGraphPreviewPressed() {
        pastedGraph.text = Clipboard.getSystemClipboard().string
        if (pastedGraph.text.isNotEmpty()) {
            try {
                currentPastedGraph = AMGraph.graphOf(pastedGraph.text)
                pastedGraphLabel.text =
                    "Vertices: ${currentPastedGraph!!.size()}" + "    " + "Edges: ${currentPastedGraph!!.getEdges().size}"
                pastedGraphHBox.isVisible = true
                pastedGraphHBox.prefHeight = pastedGraphLabel.prefHeight
            } catch (e: Exception) {
                invalidPasteFormat()
            }
        } else {
            emptyPaste()
        }
    }

    private fun emptyPaste() {
        pastedGraph.text = ""
        pastedGraphHBox.isVisible = false
        pastedGraphHBox.prefHeight = 0.0
    }

    private fun invalidPasteFormat() {
        pastedGraphLabel.text =
            "Invalid Format"
        pastedGraphHBox.isVisible = true
        pastedGraphHBox.prefHeight = pastedGraphLabel.prefHeight
    }

    @FXML
    private fun clearPastedGraph() {
        emptyPaste()
    }

    @FXML
    private fun pasteGraphPressed() {
        pasteGraphPreviewPressed()
        try {
            val g = AMGraph.graphOf(pastedGraph.text)
            presetPressed(g as Graph<Any>, "from clipboard")
        } catch (_: Exception) {
            invalidPasteFormat()
        }

    }
}