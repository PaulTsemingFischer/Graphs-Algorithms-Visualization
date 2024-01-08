package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Polygon

class Controller<E: Any> {
    @FXML
    private lateinit var pane: Pane

    @FXML
    private lateinit var fromVertexField: TextField
    @FXML
    private lateinit var toVertexField: TextField
    @FXML
    private lateinit var weightField: TextField



    private lateinit var paneWidth : ReadOnlyDoubleProperty
    private lateinit var paneHeight : ReadOnlyDoubleProperty
    private val CIRCLE_RADIUS = 20.0

    private lateinit var graph : Graph<E>
    private val stringToEMap = HashMap<String, E>()
    private var edges = ArrayList<Edge>()
    private var vertices = ArrayList<Vertex>()
    private val hitboxes = ArrayList<Circle>()

    @FXML
    fun initialize() {
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    fun graphInit(graph : Graph<E>){
        this.graph = graph
        for(vertex in graph.getVertices()){
            stringToEMap[vertex.toString()] = vertex
        }
    }

    fun draw() {
        val graphVertices = graph.getVertices().toList()
        val verticesElements = Array(graphVertices.size){ index -> Vertex(graphVertices[index].toString(), Math.random(), Math.random())}
        val edgeElements = ArrayList<Edge>()

        for(i in verticesElements.indices){
            for(j in i until verticesElements.size){
                val outBoundWeight = graph[graphVertices[i], graphVertices[j]] ?: continue
                val inBoundWeight = graph[graphVertices[j], graphVertices[i]] ?: continue
                edgeElements.add(Edge(verticesElements[i], verticesElements[j], outBoundWeight, inBoundWeight))
            }
        }
        edges = edgeElements
        vertices = ArrayList(verticesElements.asList())
        pane.children.addAll(edgeElements)
        pane.children.addAll(verticesElements)
        pane.children.addAll(hitboxes)
        for(edge in edgeElements) edge.initialize()
    }


    @FXML
    private fun redrawPressed() {
        pane.children.clear()
        draw()
    }

    @FXML
    private fun editEdgePressed() {
        val from = stringToEMap[fromVertexField.text]!!
        val to = stringToEMap[toVertexField.text]!!
        val weight = weightField.text
        graph[from, to] = weight.toInt()
        for(edge in edges){
            if(edge.checkMatch(from, to)){
                edge.setLabelWeight(weight, true)
            } else if(edge.checkMatch(to, from)){
                edge.setLabelWeight(weight, false)
            }
        }
    }


    //Precondition: x and y are between 0 and 1
    inner class Vertex(val name: String, x : Double, y : Double) : StackPane() {
        //Components
        private val circle = Circle(CIRCLE_RADIUS, Color.BLUE)
        private val label = Label(name)
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
        private var xpos : DoubleProperty = SimpleDoubleProperty(x)
        private var ypos : DoubleProperty = SimpleDoubleProperty(y)

        var vtranslateXProperty : DoubleBinding = paneWidth.multiply(xpos).multiply(usablePercentPaneWidth)
        var vtranslateYProperty : DoubleBinding = paneHeight.multiply(ypos).multiply(usablePercentPaneHeight)

        //Dragging
        private var xDelta : Double = 0.0
        private var yDelta : Double = 0.0

        init{
            //Circle
            circle.translateXProperty().bind(vtranslateXProperty)
            circle.translateYProperty().bind(vtranslateYProperty)

            //Label
            label.translateXProperty().bind(vtranslateXProperty)
            label.translateYProperty().bind(vtranslateYProperty)

            label.textFill = Color.WHITE

            //Hitbox
            hitbox.translateXProperty().bind(vtranslateXProperty)
            hitbox.translateYProperty().bind(vtranslateYProperty)

            hitbox.centerX = CIRCLE_RADIUS
            hitbox.centerY = CIRCLE_RADIUS

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
            xDelta = event.sceneX / pane.width - xpos.get()
            yDelta = event.sceneY / pane.height - ypos.get()
        }
        private fun drag(event : MouseEvent) {
            xpos.set((event.sceneX / pane.width - xDelta).let{if(it > 1) 1.0 else if(it < 0) 0.0 else it})
            ypos.set((event.sceneY / pane.height - yDelta).let{if(it > 1) 1.0 else if(it < 0) 0.0 else it})
        }

        private fun greyNonAttached(vertex: Vertex){
//            for(vert in vertices){
//                vert.circle.fill = Color(0.0, 0.0, 1.0, 0.3)
//            }
//            for(edge in edges){
//                if(edge.from != vertex && edge.to != vertex){
//                    edge.label.textFill = Color.GREY
//                    edge.line.stroke = Color.rgb(192, 192, 192, 0.8)
//                }
//                else{
//                    edge.from.let{if(it != this) it.circle.fill = Color.BLUE}
//                    edge.to.let{if(it != this) it.circle.fill = Color.BLUE}
//                    edge.line.stroke = Color.RED
//                }
//            }
        }

        private fun ungreyEverything(){
//            for(edge in edges){
//                edge.label.textFill = Color.BLACK
//                edge.line.stroke = Color.BLACK
//            }
//            for (vert in vertices){
//                vert.circle.fill = Color.BLUE
//            }
        }
        fun getCenterX() : DoubleBinding {
            return circle.translateXProperty().add(circle.radiusProperty())
        }
        fun getCenterY() : DoubleBinding {
            return circle.translateYProperty().add(circle.radiusProperty())
        }
        fun move(x: Double, y: Double){
            xpos.set(x)
            ypos.set(y)
        }
    }
    inner class Edge(private val from : Vertex, private val to : Vertex, outBoundWeight : Int, inBoundWeight: Int) : Pane() {
//        private val outBound = Line()
//        private val inBound = Line()
//        private var outBoundLabel = Label(outBoundWeight.toString())
//        private var inBoundLabel = Label(outBoundWeight.toString())

        private val Arrow1 = Arrow(from, to, outBoundWeight, true)
        private val Arrow2 = Arrow(to, from, inBoundWeight, false)

        inner class Arrow(fromVertex : Vertex, toVertex : Vertex, weight: Int, isOutbounds: Boolean) : Pane() {
            private val line = Line()
            private var label = Label(weight.toString())
            init{
                line.startXProperty().bind(fromVertex.vtranslateXProperty.add(inverseSlope().multiply(if(isOutbounds) 1 else -1).multiply(CIRCLE_RADIUS).divide(2)))
                line.startYProperty().bind(fromVertex.vtranslateYProperty)
                line.endXProperty().bind(toVertex.vtranslateXProperty)
                line.endYProperty().bind(toVertex.vtranslateYProperty)

                label.translateXProperty().bind(line.startXProperty().add(line.endXProperty()).divide(2))
                label.translateYProperty().bind(line.startYProperty().add(line.endYProperty()).divide(2))

                children.addAll(line, label)
            }

            private fun inverseSlope(): DoubleBinding {
                return Bindings.createDoubleBinding(
                    { (line.endXProperty().get() - line.startXProperty().get()) / (line.endYProperty().get() - line.startYProperty().get()) },
                    line.startXProperty(),
                    line.startYProperty(),
                    line.endXProperty(),
                    line.endYProperty()
                )
            }
        }

//        init{
//            //Initializing triangles
//            outBound.stroke = Color.BLACK
//            inBound.stroke = Color.BLACK
//
//            //Initializing labels
//            outBoundLabel.textFill = Color.BLACK
//            inBoundLabel.textFill = Color.BLACK
//
//            //Binding triangle vertices and labels
//                //Outbound
//            from.vtranslateXProperty.addListener {_ ->updateTrianglePoints(true); updateLabelPosition(outBoundLabel) }
//            from.vtranslateXProperty.addListener {_ -> updateTrianglePoints(true); updateLabelPosition(outBoundLabel) }
//
//            to.vtranslateXProperty.addListener {_ -> updateTrianglePoints(true); updateLabelPosition(outBoundLabel) }
//            to.vtranslateXProperty.addListener {_ -> updateTrianglePoints(true); updateLabelPosition(outBoundLabel) }
//
//                //Inbound
//            from.vtranslateXProperty.addListener {_ -> updateTrianglePoints(false); updateLabelPosition(inBoundLabel) }
//            from.vtranslateXProperty.addListener {_ -> updateTrianglePoints(false); updateLabelPosition(inBoundLabel) }
//
//            to.vtranslateXProperty.addListener {_ -> updateTrianglePoints(false); updateLabelPosition(inBoundLabel) }
//            to.vtranslateXProperty.addListener {_ -> updateTrianglePoints(false); updateLabelPosition(inBoundLabel) }
//
//            children.addAll(outBound, inBound, outBoundLabel, inBoundLabel)
//        }
//
//        fun initialize() {
//            updateTrianglePoints(true)
//            updateTrianglePoints(false)
//            updateLabelPosition(outBoundLabel)
//            updateLabelPosition(inBoundLabel)
//        }
//
//        fun checkMatch(from: E, to: E): Boolean = this.from.name.also{println(it)} == from.toString().also{println(it)} && this.to.name == to.toString()
//
//        fun setLabelColor(color: Color){
//            outBoundLabel.textFill = color
//            inBoundLabel.textFill = color
//        }
//
//        fun setLabelWeight(weight: String, isOutbounds: Boolean){
//            if(isOutbounds) outBoundLabel.text = weight
//            else inBoundLabel.text = weight
//        }
//
//        fun setTriangleColor(color: Color, isOutbounds: Boolean){
//            if(isOutbounds) outBound.fill = color
//            else inBound.fill = color
//        }
//
//        //Label helper methods
//        private fun updateLabelPosition(label: Label){
//            val midpoint = calculateMidpoint(outBound)
//            label.translateX = midpoint.first
//            label.translateY = midpoint.second
//        }
//
//        private fun calculateMidpoint(polygon: Polygon): Pair<Double, Double> {
//            val xSum = polygon.points.subList(0, polygon.points.size / 2).sum()
//            val ySum = polygon.points.subList(polygon.points.size / 2, polygon.points.size).sum()
//
//            val xMidpoint = xSum / (polygon.points.size / 2)
//            val yMidpoint = ySum / (polygon.points.size / 2)
//
//            return Pair(xMidpoint, yMidpoint)
//        }
//
//        //Triangle helper methods
//        private fun updateTrianglePoints(isOutbounds: Boolean){
//            println("From: ${from.vtranslateXProperty.get()} ${from.vtranslateYProperty.get()}")
//            println("To: ${to.vtranslateXProperty.get()} ${to.vtranslateYProperty.get()}")
//            if(isOutbounds){
//                updateTrianglePoints(outBound,
//                    from.vtranslateXProperty.get() to from.vtranslateYProperty.get(),
//                    from.vtranslateXProperty.get() + CIRCLE_RADIUS/2 to from.vtranslateYProperty.get() + CIRCLE_RADIUS/2,
//                    to.vtranslateXProperty.get() + CIRCLE_RADIUS/2 to to.vtranslateYProperty.get() + CIRCLE_RADIUS/2)
//            } else {
//                updateTrianglePoints(inBound,
//                    to.vtranslateXProperty.get() to to.vtranslateYProperty.get(),
//                    to.vtranslateXProperty.get() - CIRCLE_RADIUS/2 to to.vtranslateYProperty.get() - CIRCLE_RADIUS/2,
//                    from.vtranslateXProperty.get() - CIRCLE_RADIUS/2 to from.vtranslateYProperty.get() - CIRCLE_RADIUS/2)
//            }
//        }
//
//        private fun updateTrianglePoints(triangle: Polygon, point1: Pair<Double, Double>, point2: Pair<Double, Double>, point3: Pair<Double, Double>) {
//            triangle.points.clear()
//            triangle.points.addAll(
//                point1.first, point1.second,
//                point2.first, point2.second,
//                point3.first, point3.second
//            )
//            println(triangle.points)
//        }
//    }
}