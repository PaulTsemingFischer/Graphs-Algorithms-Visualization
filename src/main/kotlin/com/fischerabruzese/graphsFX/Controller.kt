package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import com.fischerabruzese.graph.Graph
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.text.NumberFormat
import kotlin.system.measureNanoTime

class Controller<E: Any> {
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
    private lateinit var graphicComponents: GraphicComponents<E>

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
    private lateinit var randomizeButton: Button
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

    //Data
    private lateinit var graph: Graph<Any>

    //Window initialization
    @FXML
    fun initialize() {
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    //Initialization for anything involving the graph
    fun initializeGraph(graph: Graph<E>) {
        this.graph = graph.mapVertices {it}
        graphicComponents = GraphicComponents(graph, pane) //Create the graphic components
        graphicComponents.draw() //Draw the graphic components
        initializeClusterRandomizationSwitch()
        initializePhysicsSlider()
        initializeVertexSelection()
        initializeClusterConnectednessSlider()
        switchButton.switchedEvents.addLast { switchSwitched(it) }
        switchSwitched(SwitchButton.SwitchButtonState.LEFT) //initialize properties in specific graphic
        updateClusterColoring()
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
    private fun printEntry(title: String? = null, text: String, color: Color = Color.BLACK){
        val coloredTitle = Text(title).apply{fill = color; style = "-fx-font-weight: bold"}
        val coloredText = Text(text).apply{fill = color}
        console.children.addAll(0, listOf(coloredTitle, coloredText, Text(CONSOLE_LINE_SEPARATOR)))
    }

    private fun printClusters(clusters: Collection<Graph<E>>, connectedness: Double, time: Long) {
        val title = "Clusters (connectedness: ${NumberFormat.getNumberInstance().format(connectedness)})\n"
        val text = buildString {
            val sortedClusters = clusters.sortedByDescending { it.size() }
            for (cluster in sortedClusters) {
                val sortedVertices = cluster.getVertices().sortedBy { it.toString() }
                append("\nSize ${sortedVertices.size}: ${sortedVertices}\n")
            }
            append("\nTime(ns): ${NumberFormat.getIntegerInstance().format(time)}\n")
        }
        printEntry(title, text, Color.BLUE)
    }

    private fun printDijkstra(from: E, to: E, path: List<E>, distance: Int, time: Long) {
        val title = "Dijkstra from $from to $to\n"
        val text = buildString {
            append("Path: $path\n")
            append("Distance: $distance\n")
            append("Time(ns): ${NumberFormat.getIntegerInstance().format(time)}\n")
        }
        printEntry(title, text, Color.DEEPSKYBLUE)
    }

    private fun printBfs(from: E, to: E, path: List<E>, time: Long) {
        val title = "Breadth first search from $from to $to\n"
        val text = pathingString(path, time)
        printEntry(title, text, Color.DEEPSKYBLUE)
    }

    private fun printDfs(from: E, to: E, path: List<E>, time: Long) {
        val title = "Depth first search from $from to $to\n"
        val text = pathingString(path, time)
        printEntry(title, text, Color.DEEPSKYBLUE)
    }

    private fun pathingString(path: List<E>, time: Long): String{
        return buildString {
            append("Path: $path\n")
            append("Time(ns): ${NumberFormat.getIntegerInstance().format(time)}\n")
        }
    }

    //Physics
    private fun initializePhysicsSlider(){
        physicsSlider.valueProperty().addListener { _, _, newValue ->
            newValue?.let {
                if(it.toDouble() < 0.02)
                    graphicComponents.physicsC.on = false
                else {
                    graphicComponents.physicsC.speed = it.toDouble()
                    if (!graphicComponents.physicsC.on) {
                        graphicComponents.physicsC.on = true
                        graphicComponents.physicsC.simulate()
                    }
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
    private fun retrieveVertexElement(lookupKey: String): E? {
        return graphicComponents.stringToVMap[lookupKey]?.v
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
    private fun getClusters(): Pair<Collection<Graph<E>>, Double> {
        val connectedness = connectednessSlider.value
        val clusters = graph.getClusters(connectedness)
        return Pair(clusters, connectedness)
    }

    @FXML
    private fun printClustersPressed() {
        val clusters: Pair<Collection<Graph<E>>, Double>
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
        when(state){
            SwitchButton.SwitchButtonState.RIGHT -> {
                generateRandomGraph()
            }
            SwitchButton.SwitchButtonState.LEFT -> {
                generateClusteredGraph()
            }
        }
        updateClusterColoring()
    }

    private fun generateClusteredGraph() {
        if(!vertexCountField.textProperty().isEmpty.get()) {
            this.graph = AMGraph<Any>()
        }
        val clusterCount = clusterCountTextBox.text.toInt()
        val interConn = interConnectednessSlider.value
        val intraConn = intraConnectednessSlider.value

        var unweighted = false
        val max: Int = try { maxWeightTextBox.text.toInt() + 1 } catch (e: NumberFormatException) { 2.also{unweighted = true} }
        val min: Int = try { minWeightTextBox.text.toInt() } catch (e: NumberFormatException) { 1 }

        this.graph.randomizeWithCluster(clusterCount, min, max, intraConn, interConn)
        if(!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)

        graphicComponents.draw()
        if(unweighted)
            graphicComponents.hideWeight()
    }

    private fun generateRandomGraph() {
        val probConn = probOfConnectionsField.text.toDouble()
        var unweighted = false
        val max: Int = try { maxWeightTextBox.text.toInt() + 1 } catch (e: NumberFormatException) { 2.also{unweighted = true} }
        val min: Int = try { minWeightTextBox.text.toInt() } catch (e: NumberFormatException) { 1 }

        this.graph.randomize(probConn, min, max)
        if(!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)

        graphicComponents.draw()
        if(unweighted)
            graphicComponents.hideWeight()
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