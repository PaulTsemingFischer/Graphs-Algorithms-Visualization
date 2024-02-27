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
    private lateinit var paneWidth : ReadOnlyDoubleProperty
    private lateinit var paneHeight : ReadOnlyDoubleProperty

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

    //Graph representation
    private val CIRCLE_RADIUS = 20.0
    private lateinit var graph : Graph<E>
    private val stringToVMap = HashMap<String, Controller<E>.Vertex>()
    private var edges = ArrayList<Edge>()
    private var vertices = ArrayList<Vertex>()
    private val hitboxes = ArrayList<Circle>()

    //Initialization
    @FXML
    fun initialize() {
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    fun setGraph(graph : Graph<E>){
        this.graph = graph
        previousGraphHash = graph.hashCode()
    }

    //Listen for graph changes
    private val executor = Executors.newScheduledThreadPool(1)
    private var previousGraphHash by Delegates.notNull<Int>()

    fun startListening() {
        while (true) {
            val currentHash = graph.hashCode()
            if (currentHash != previousGraphHash) {
                changeReceived()
                previousGraphHash = currentHash
            }
            Thread.sleep(5000)
        }
    }


    fun stopListening() {
        executor.shutdown()
    }

    private fun changeReceived(){
        draw()
    }


//    private fun fromTxtDemo(){
//        vertices.clear()
//        for((x,y) in positions){
//            vertices.add(Vertex(null, x, y))
//        }
//        draw()
//    }

    //Drawing
    fun draw() {
        val graphVertices = graph.getVertices().toList()
        val verticesElements = Array(graphVertices.size){ index -> Vertex(
            graphVertices[index],
            if(vertices.size > index) vertices[index].x.get() else Math.random(),
            if(vertices.size > index) vertices[index].y.get() else Math.random()
        ) }
        val edgeElements = ArrayList<Edge>()

        for(v1 in graphVertices.indices){
            for(v2 in v1 until graphVertices.size){
                val v1tov2 = graph[graphVertices[v1], graphVertices[v2]] ?: -1
                val v2tov1 = graph[graphVertices[v2], graphVertices[v1]] ?: -1
                if(v2tov1 > -1 || v1tov2 > -1)
                    edgeElements.add(Edge(verticesElements[v1], verticesElements[v2], v1tov2, v2tov1))
            }
        }
        edges = edgeElements
        vertices = ArrayList(verticesElements.asList())
        pane.children.addAll(edgeElements)
        pane.children.addAll(verticesElements)
        pane.children.addAll(hitboxes)
    }

    @FXML
    private fun redrawPressed() {
        pane.children.clear()
        draw()
    }

    private fun greyNonAttached(vertex: Vertex){
        for(vert in vertices){
            vert.setColor(Color(0.0, 0.0, 1.0, 0.3))
        }
        for(edge in edges){
            if(edge.v1 != vertex && edge.v2 != vertex){
                edge.setLineColor(Color.rgb(192, 192, 192, 0.8))
                edge.setLabelColor(Color.GREY)
            }
            else{
                edge.v1.let{if(it != vertex) it.setColor(Color.BLUE)}
                edge.v2.let{if(it != vertex) it.setColor(Color.BLUE)}
                edge.setLineColor(Color.GREEN, Color.RED, vertex)
                edge.setLabelColor(Color.GREEN, Color.RED, vertex)
            }
        }
    }

    private fun ungreyEverything(){
        for(edge in edges){
            edge.setLineColor(Color.rgb(0, 0, 0, 0.6))
            edge.setLabelColor(Color.BLACK)
        }
        for (vert in vertices){
            vert.setColor(Color.BLUE)
        }
    }


    //Precondition: x and y are between 0 and 1
    inner class Vertex(val v: E, xInit : Double, yInit : Double) : StackPane() {
        init {
            stringToVMap[v.toString()] = this
        }
        //Components
        private val circle = Circle(CIRCLE_RADIUS, Color.BLUE)
        private val label = Label(v.toString())
        private val hitbox = Circle(CIRCLE_RADIUS, Color.TRANSPARENT)

        //Location Bindings
        private val usablePercentPaneWidth: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * CIRCLE_RADIUS / paneWidth.get() },
            paneWidth
        )
        private val usablePercentPaneHeight: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * CIRCLE_RADIUS / paneHeight.get() },
            paneHeight
        )

        //xpos and ypos are between 0 and 1
        internal var x : DoubleProperty = SimpleDoubleProperty(xInit)
        internal var y : DoubleProperty = SimpleDoubleProperty(yInit)

        var vtranslateXProperty : DoubleBinding = paneWidth.multiply(this.x).multiply(usablePercentPaneWidth).add(CIRCLE_RADIUS)
        var vtranslateYProperty : DoubleBinding = paneHeight.multiply(this.y).multiply(usablePercentPaneHeight).add(CIRCLE_RADIUS)

        //Dragging
        private var xDelta : Double = 0.0
        private var yDelta : Double = 0.0

        init{
            //Circle
            circle.translateXProperty().bind(vtranslateXProperty.subtract(CIRCLE_RADIUS))
            circle.translateYProperty().bind(vtranslateYProperty.subtract(CIRCLE_RADIUS))

            //Label
            label.translateXProperty().bind(vtranslateXProperty.subtract(CIRCLE_RADIUS))
            label.translateYProperty().bind(vtranslateYProperty.subtract(CIRCLE_RADIUS))

            label.textFill = Color.WHITE

            //Hitbox
            hitbox.translateXProperty().bind(vtranslateXProperty)
            hitbox.translateYProperty().bind(vtranslateYProperty)

            //Listeners
            hitbox.setOnMouseEntered { circle.fill = Color.GREEN }
            hitbox.setOnMouseExited { circle.fill = Color.BLUE}

            hitbox.setOnMousePressed { dragStart(it); greyNonAttached(this); circle.fill = Color.RED }
            hitbox.setOnMouseDragged { drag(it) }
            hitbox.setOnMouseReleased { ungreyEverything(); circle.fill = Color.GREEN }
            hitbox.pickOnBoundsProperty().set(true)

            hitboxes.add(hitbox)

            children.addAll(circle, label)
        }

        private fun dragStart(event : MouseEvent) {
            xDelta = event.sceneX / pane.width - x.get()
            yDelta = event.sceneY / pane.height - y.get()
        }
        private fun drag(event : MouseEvent) {
            x.set((event.sceneX / pane.width - xDelta).let{if(it > 1) 1.0 else if(it < 0) 0.0 else it})
            y.set((event.sceneY / pane.height - yDelta).let{if(it > 1) 1.0 else if(it < 0) 0.0 else it})
        }

        fun setColor(color: Color) {
            circle.fill = color
        }

        override fun toString(): String {
            return v.toString()
        }
    }

    inner class Edge(val v1 : Vertex, val v2 : Vertex, v1tov2 : Int, v2tov1: Int) : StackPane() {
        val v1tov2Connection = Connection(v1, v2, v1tov2, true)
        val v2tov1Connection = Connection(v2, v1, v2tov1, false)

        init {
            if(v1tov2 > -1) {
                v1tov2Connection.setLineColor(Color.rgb(0, 0, 0, 0.6))
                children.add(v1tov2Connection)
            }
            if(v2tov1 > -1) {
                v2tov1Connection.setLineColor(Color.rgb(0, 0, 0, 0.6))
                children.add(v2tov1Connection)
            }
        }

        inner class Connection(from : Vertex, to : Vertex, weight: Int, mirror : Boolean) : Pane() {
            private val line = Line()

            private var director1 : Director
            private var director2 : Director
            private var label = Label(weight.toString())

            fun getLabel() : Label {
                return v1tov2Connection.label
            }

            init {
                val dyTotal = to.vtranslateYProperty.subtract(from.vtranslateYProperty)
                val dxTotal = to.vtranslateXProperty.subtract(from.vtranslateXProperty)

                val length = Bindings.createDoubleBinding(
                    { sqrt(dyTotal.get().pow(2) + dxTotal.get().pow(2)) },
                    dyTotal, dxTotal
                )

                val dy = dxTotal.multiply(CIRCLE_RADIUS / 4).divide(length).multiply(-1)
                val dx = dyTotal.multiply(CIRCLE_RADIUS / 4).divide(length)

                line.startXProperty().bind(from.vtranslateXProperty.add(dx))
                line.startYProperty().bind(from.vtranslateYProperty.add(dy))
                line.endXProperty().bind(to.vtranslateXProperty.add(dx))
                line.endYProperty().bind(to.vtranslateYProperty.add(dy))

                director1 = Director(line.startXProperty().add(dxTotal.multiply(0.33)), line.startYProperty().add(dyTotal.multiply(0.33)), mirror)
                director2 = Director(line.startXProperty().add(dxTotal.multiply(0.66)), line.startYProperty().add(dyTotal.multiply(0.66)), mirror)

                //Sets the label to the average of the line endpoints plus some offsets to ensure the label is centered
                label.translateXProperty().bind((line.startXProperty().add(line.endXProperty())).divide(2).subtract(5))
                label.translateYProperty().bind((line.startYProperty().add(line.endYProperty())).divide(2).subtract(10))

                label.textFill = Color.BLACK
                label.font = Font(15.0)

                children.addAll(line, label, director1, director2)
            }

            fun setLineColor(color : Color) {
                line.stroke = color
                director1.setColor(color)
                director2.setColor(color)
            }

            fun setLabelColor(color : Color) {
                label.textFill = color
            }

            fun setWeight(weight : String) {
                label.text = weight
            }

            inner class Director(startposX : DoubleBinding, startposY : DoubleBinding, mirror: Boolean) : Pane() {
                private val line1 = Line()
                private val line2 = Line()

                init {
                    line1.startXProperty().bind(startposX)
                    line1.startYProperty().bind(startposY)
                    line2.startXProperty().bind(startposX)
                    line2.startYProperty().bind(startposY)

                    val dyTotal = v2.vtranslateYProperty.subtract(v1.vtranslateYProperty)
                    val dxTotal = v2.vtranslateXProperty.subtract(v1.vtranslateXProperty)

                    val theta = Bindings.createDoubleBinding(
                        { atan2(dyTotal.get(), dxTotal.get()) },
                        dyTotal, dxTotal
                    )

                    val dx1 = Bindings.createDoubleBinding(
                        { CIRCLE_RADIUS/4.8 * cos(theta.get() + (PI/4)) },
                        theta
                    )
                    val dy1 = Bindings.createDoubleBinding(
                        { CIRCLE_RADIUS/4.8 * sin(theta.get() + (PI/4)) },
                        theta
                    )
                    val dx2 = Bindings.createDoubleBinding(
                        { CIRCLE_RADIUS/4.8 * cos(theta.get() - (PI/4)) },
                        theta
                    )
                    val dy2 = Bindings.createDoubleBinding(
                        { CIRCLE_RADIUS/4.8 * sin(theta.get() - (PI/4)) },
                        theta
                    )
                    val endX1 = startposX.add(dx1.multiply(if(mirror) -1 else 1))
                    val endY1 = startposY.add(dy1.multiply(if(mirror) -1 else 1))
                    val endX2 = startposX.add(dx2.multiply(if(mirror) -1 else 1))
                    val endY2 = startposY.add(dy2.multiply(if(mirror) -1 else 1))

                    line1.endXProperty().bind(endX1)
                    line1.endYProperty().bind(endY1)
                    line2.endXProperty().bind(endX2)
                    line2.endYProperty().bind(endY2)

                    children.addAll(line1, line2)
                }

                fun setColor(color: Color){
                    line1.stroke = color
                    line2.stroke = color
                }
            }
        }

        fun checkMatch(from: Vertex, to: Vertex): Boolean = from == v1 && to == v2

        fun setLabelWeight(weight: String, isOutbounds: Boolean){
            if(isOutbounds) v1tov2Connection.setWeight(weight)
            else v2tov1Connection.setWeight(weight)
        }

        fun setLineColor(color: Color) {
            v1tov2Connection.setLineColor(color)
            v2tov1Connection.setLineColor(color)
        }

        fun setLineColor(outBoundColor: Color, inboundColor: Color, from: Vertex) {
            if(v1 == from){
                v1tov2Connection.setLineColor(outBoundColor)
                v2tov1Connection.setLineColor(inboundColor)
            } else{
                v1tov2Connection.setLineColor(inboundColor)
                v2tov1Connection.setLineColor(outBoundColor)
            }
        }

        fun setLabelColor(color: Color) {
            v1tov2Connection.setLabelColor(color)
            v2tov1Connection.setLabelColor(color)
        }

        fun setLabelColor(outBoundColor: Color, inboundColor: Color, from: Vertex) {
            if(v1 == from){
                v1tov2Connection.setLabelColor(outBoundColor)
                v2tov1Connection.setLabelColor(inboundColor)
            } else{
                v1tov2Connection.setLabelColor(inboundColor)
                v2tov1Connection.setLabelColor(outBoundColor)
            }
        }

    }
    //Clustering
     fun moveClusters(clusters: Collection<Graph<E>>){
        //convert clusters to List<List<Vertex>>
        val clustersGraphic = clusters.map { cluster -> ArrayList<Vertex>().apply {
            for(vert in cluster){
                add(vertices.find { it.v == vert } ?: continue)
            }
        } }


        val numDimensionalSections = ceil(sqrt(clustersGraphic.size.toDouble())).toInt()

        for(xSections in 0 until numDimensionalSections){
            for(ySections in 0 until numDimensionalSections){
                val cluster = try { clustersGraphic[xSections*ySections] } catch (_: Exception) {break}

                for(v in cluster){
                    v.x.set(v.x.get() * (1.0/numDimensionalSections) + (xSections * (numDimensionalSections-1)))
                    v.y.set(v.y.get() * (1.0/numDimensionalSections) + (ySections * (numDimensionalSections-1)))
                }
            }
        }
    }

    //Graph presets
    @FXML
    private fun preset1Pressed(){}
    @FXML
    private fun preset2Pressed(){}
    @FXML
    private fun preset3Pressed(){}
    @FXML
    private fun preset4Pressed(){}
    @FXML
    private fun preset5Pressed(){}
    @FXML
    private fun preset6Pressed(){}

    //Console
    private fun printClusters(clusters: List<List<E>>, connectedness: Double){
        console.text += "Clusters (connectedness: $connectedness)\n"
        for(cluster in clusters){
            console.text += "" + cluster + '\n'
        }
        console.text += CONSOLE_LINE_SEPARATOR
    }
    private fun printDijkstra(from: E, to: E, path: List<E>, distance: Int, time: Long){
        console.text += "Dijkstra from $from to $to\n"
        console.text += "Path: $path\n"
        console.text += "Distance: $distance\n"
        console.text += "Time(ms): $time\n"
        console.text += CONSOLE_LINE_SEPARATOR
    }
    private fun printBfs(from: E, to: E, path: List<E>, time: Long){
        console.text += "Breadth first search from $from to $to\n"
        console.text += "Path: $path\n"
        console.text += "Time(ms): $time\n"
        console.text += CONSOLE_LINE_SEPARATOR
    }
    private fun printDfs(from: E, to: E, path: List<E>, time: Long){
        console.text += "Depth first search from $from to $to\n"
        console.text += "Path: $path\n"
        console.text += "Time(ms): $time\n"
        console.text += CONSOLE_LINE_SEPARATOR
    }

    //Randomization
    @FXML
    private fun randomizePressed(){

    }

    //Clustering
    @FXML
    private fun getClustersPressed(){}

    //Vertex selection
    private fun retrieveVertexElement(lookupKey: String) : E? {
        return stringToVMap[lookupKey.trim()]?.v
    }

    private fun getFromField(): E{
        return retrieveVertexElement(fromVertexField.text) ?: throw InvalidKeyException("user input: \"${fromVertexField.text}\" is not an existing vertex")
    }
    private fun getToField(): E {
        return retrieveVertexElement(toVertexField.text) ?: throw InvalidKeyException("user input: \"${toVertexField.text}\" is not an existing vertex")
    }

    @FXML
    private fun fromVertexChanged(){}

    @FXML
    private fun toVertexChanged(){}

    //Pathing
    @FXML
    private fun dijkstraPressed(){
        pathingButtonPressed(graph::path).let { printDijkstra(it.first.first, it.first.second, it.second, graph.distance(it.first.first, it.first.second), it.third) }
    }

    @FXML
    private fun bfsPressed() {
        pathingButtonPressed(graph::path).let { printBfs(it.first.first, it.first.second, it.second, it.third) }
    }

    @FXML
    private fun dfsPressed() {
        pathingButtonPressed(graph::path).let { printDfs(it.first.first, it.first.second, it.second, it.third) }
    }

    private fun pathingButtonPressed(algorithm: (E, E) -> List<E>) : Triple<Pair<E, E>, List<E>, Long>{
        val from = getFromField()
        val to = getToField()
        val path : List<E>
        val time = measureTimeMillis {
            path = algorithm(from, to)
        }
        return Triple((from to to), path, time)
    }








    //Randomization
//    @FXML
//    private fun mf0Pressed(){
//        graph.getClusters(0.4, 10000).forEach{println(it.getVertices())}
//    }

//    @FXML
//    private fun randomizePressed(){
//        graph.randomize(3, 9, false)
//        redrawPressed()
//    }

}

//Precondition: weight is a positive integer
//    @FXML
//    private fun editEdgePressed() {
//        val from = stringToVMap[fromVertexField.text].let{ it ?: return }
//        val to = stringToVMap[toVertexField.text].let{ it ?: return }
//        val weight = weightField.text
//        weight.toIntOrNull()?.takeIf { it > 0 }?.let { graph[from.v, to.v] = it }
//        for(edge in edges){
//            if(edge.checkMatch(from, to)){
//                edge.setLabelWeight(weight, true)
//            } else if(edge.checkMatch(to, from)){
//                edge.setLabelWeight(weight, false)
//            }
//        }
//    }