package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.GridPane
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
    private lateinit var intraConnectednessChoiceBox: ChoiceBox<String>
    @FXML
    private lateinit var interConnectednessDropdown: ChoiceBox<String>
    @FXML
    private lateinit var allowDisjointSelectionBox: CheckBox
    @FXML
    private lateinit var minWeightTextBox: TextField
    @FXML
    private lateinit var maxWeightTextBox: TextField
    @FXML
    private lateinit var randomizeButton: Button
    @FXML
    private lateinit var avgConnPerVertexText: TextField
    @FXML
    private lateinit var probOfConnectionsText: TextField

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
            Label("Cluster Rand ").apply { textFill = Color.WHITE },
            SwitchButton().also { switchButton = it },
            Label(" Pure Rand").apply { textFill = Color.WHITE }
        )
        switchButton.switchedEvents.addLast { switchSwitched(it) }
        intraConnectednessChoiceBox.items = FXCollections.observableArrayList("Low", "Med", "High")
        interConnectednessDropdown.items = FXCollections.observableArrayList("Low", "Med", "High")
        switchSwitched(SwitchButton.SwitchButtonState.LEFT) //initialize properties in specific graphic
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


    //Randomization
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
    }

    private fun generateClusteredGraph() {
        val clusterCount = clusterCountTextBox.text.toInt()
        var interConn = 0.0
        var intraConn = 0.0
        when(interConnectednessDropdown.value){
            "Low" -> interConn = 0.003
            "Med" -> interConn = 0.02
            "High" -> interConn = 0.04
        }
        when(intraConnectednessChoiceBox.value){
            "Low" -> intraConn = 0.11
            "Med" -> intraConn = 0.33
            "High" -> intraConn = 0.55
        }
        val min = minWeightTextBox.text.toInt()
        val max = maxWeightTextBox.text.toInt() + 1
        this.graph.randomizeWithCluster(clusterCount, min, max, intraConn, interConn)
        if(!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)
        graphicComponents.draw()
    }

    private fun generateRandomGraph() {
        val probConn = probOfConnectionsText.text.toDouble()
        val min = minWeightTextBox.text.toInt()
        val max = maxWeightTextBox.text.toInt() + 1
        this.graph.randomize(probConn, min, max)
        if(!allowDisjointSelectionBox.isSelected) this.graph.mergeDisjoint(min, max)
        graphicComponents.draw()
    }

    //probOfConnectionsEdited
    //avgConnectionsPerVertexEdited
    @FXML
    private fun probOfConnectionsEdited() {
//        val prevVal = avgConnPerVertexText.text.toDoubleOrNull()?.let {
//            ((it - if(!allowDisjointSelectionBox.isSelected) ((graph.size()-1.0)/graph.size()) else 0.0) / (2*(graph.size()-1))).toString()
//        } ?: ""

        if(probOfConnectionsText.text.toDoubleOrNull() != null) {
            if(probOfConnectionsText.text.toDouble() > 1.0)
                probOfConnectionsText.text = 1.0.toString()

            else if(probOfConnectionsText.text.toDouble() < 0.0)
                probOfConnectionsText.text = 0.0.toString()
        }

        avgConnPerVertexText.text = probOfConnectionsText.text.toDoubleOrNull()?.let {
            ((2*it*(graph.size()-1)) + (if(!allowDisjointSelectionBox.isSelected) ((graph.size()-1)/graph.size()).toDouble() else 0.0)).toString()
        } ?: ""
    }

    @FXML
    private fun avgConnectionsPerVertexEdited() {
//        val prevVal = probOfConnectionsText.text.toDoubleOrNull()?.let {
//            ((2*it*(graph.size()-1)) + (if(!allowDisjointSelectionBox.isSelected) ((graph.size()-1)/graph.size()).toDouble() else 0.0)).toString()
//        } ?: ""

        probOfConnectionsText.text = avgConnPerVertexText.text.toDoubleOrNull()?.let {
            ((it - if(!allowDisjointSelectionBox.isSelected) ((graph.size()-1.0)/graph.size()) else 0.0) / (2*(graph.size()-1))).toString()
        } ?: ""

        if(probOfConnectionsText.text.toDoubleOrNull() != null) {
            if(probOfConnectionsText.text.toDouble() > 1.0 || probOfConnectionsText.text.toDouble() < 0.0) {
                probOfConnectionsEdited() //illegal edit was made, this method will adjust
            }
        }
    }
}