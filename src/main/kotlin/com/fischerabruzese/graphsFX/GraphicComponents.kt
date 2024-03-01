package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.text.Font
import kotlin.math.*
import kotlin.math.pow

internal class GraphicComponents<E: Any>(val graph: Graph<E>, val pane: Pane, val stringToVMap: HashMap<String, GraphicComponents<E>.Vertex>) {
    private val CIRCLE_RADIUS = 20.0
    private var edges = ArrayList<Edge>()
    private fun ArrayList<Edge>.dumpPositions() = this.map {
        Position(((it.v2.x.get()+it.v1.x.get())/2.0), ((it.v2.y.get()+it.v1.y.get())/2.0))
    }.toTypedArray()
    private var vertices = ArrayList<Vertex>()
    private fun ArrayList<Vertex>.dumpPositions() = this.map { it.pos }.toTypedArray()
    private val hitboxes = ArrayList<Circle>()

    fun draw() {
        pane.children.clear()

        val verticesElements = ArrayList(graph.getVertices())
        val graphicVertices = verticesElements.mapIndexed { index, vertex ->
            Vertex(
                vertex,
                vertices.getOrNull(index)?.x?.get() ?: Math.random(),
                vertices.getOrNull(index)?.y?.get() ?: Math.random()
            )
        }
        val edgeElements = ArrayList<Edge>()

        for((v1pos, vertex1) in verticesElements.withIndex()){
            for(vertex2 in verticesElements.subList(v1pos, verticesElements.size)){ //can't get index because we have a sublist
                val v1tov2Weight = graph[vertex1, vertex2] ?: -1
                val v2tov1Weight = graph[vertex2, vertex1] ?: -1
                if(v2tov1Weight > -1 || v1tov2Weight > -1)
                    edgeElements.add(Edge(graphicVertices[v1pos], graphicVertices[verticesElements.indexOf(vertex2)], v1tov2Weight, v2tov1Weight))
            }
        }
        edges = edgeElements
        vertices = ArrayList(graphicVertices)
        pane.children.addAll(edgeElements)
        pane.children.addAll(graphicVertices)
        pane.children.addAll(hitboxes)
    }

    /**
     * Represents a graphical vertex in the window.
     *
     * @property v The value of the vertex.
     * @property x The x position of the vertex, between 0 and 1.
     * @property y The y position of the vertex, between 0 and 1.
     */
    inner class Vertex(val v: E, xInit: Double, yInit: Double): StackPane() {
        internal val pos = Position(xInit, yInit)
        init {
            stringToVMap[v.toString()] = this
        }

        //Components
        private val circle = Circle(CIRCLE_RADIUS, Color.BLUE)
        private val label = Label(v.toString())
        private val hitbox = Circle(CIRCLE_RADIUS, Color.TRANSPARENT)

        //Location Bindings
        private val usablePercentPaneWidth: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * CIRCLE_RADIUS / pane.widthProperty().get() },
            pane.widthProperty()
        )
        private val usablePercentPaneHeight: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * CIRCLE_RADIUS / pane.heightProperty().get() },
            pane.heightProperty()
        )

        //x and y are between 0 and 1, everything should be modified in terms of these
        internal var x : DoubleProperty = SimpleDoubleProperty(xInit)
        internal var y : DoubleProperty = SimpleDoubleProperty(yInit)

        //These are actually what get read by the components
        var vtranslateXProperty : DoubleBinding = pane.widthProperty().multiply(this.x).multiply(usablePercentPaneWidth).add(CIRCLE_RADIUS)
        var vtranslateYProperty : DoubleBinding = pane.heightProperty().multiply(this.y).multiply(usablePercentPaneHeight).add(CIRCLE_RADIUS)

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

            hitbox.setOnMousePressed { dragStart(it); greying.greyDetached(this); circle.fill = Color.RED }
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

    /**
     * Represents a graphical edge in the window.
     *
     * @property v1 The first vertex of the edge.
     * @property v2 The second vertex of the edge.
     * @property v1tov2 The weight of the edge between v1 and v2.
     * @property v2tov1 The weight of the edge between v2 and v1.
     */
    inner class Edge(val v1 : Vertex, val v2 : Vertex, v1tov2 : Int, v2tov1: Int) : StackPane(){
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
                        { CIRCLE_RADIUS/4.8 * cos(theta.get() + (PI /4)) },
                        theta
                    )
                    val dy1 = Bindings.createDoubleBinding(
                        { CIRCLE_RADIUS/4.8 * sin(theta.get() + (PI /4)) },
                        theta
                    )
                    val dx2 = Bindings.createDoubleBinding(
                        { CIRCLE_RADIUS/4.8 * cos(theta.get() - (PI /4)) },
                        theta
                    )
                    val dy2 = Bindings.createDoubleBinding(
                        { CIRCLE_RADIUS/4.8 * sin(theta.get() - (PI /4)) },
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

        operator fun component1(): Vertex {
            return v1
        }
        operator fun component2(): Vertex {
            return v2
        }

    }

    val physics = object: Physics() {
        private fun calculateForce(pos1 : Position, pos2 : Position, scaleFactor: Double) : Position{
            val dx = pos2.x - pos1.x
            val dy = pos2.y - pos1.y
            val magnitude = scaleFactor / ((dx).pow(2) + (dy).pow(2)) // sc/ sqrt(dx^2 + dy^2)^2
            val angle = atan2(dy, dx)
            val fdx = magnitude * cos(angle)
            val fdy = magnitude * sin(angle)
            return Position(fdx, fdy)
        }

        //Calculates the position adjustments at a position from a list of positions
        fun calculateAdjustmentAtPos(at: Position, from: List<Position>, scale: Double, forceCapPerPos: Double = 0.1): Position{
            val scaleFactor = scale * 0.0000006 / (vertices.size + edges.size)

            var tdx = 0.0
            var tdy = 0.0

            for(pos in from){
                if(at == pos) continue
                val(fdx, fdy) = calculateForce(at, pos, scaleFactor)
                tdx += fdx
                tdy += fdy
            }

            //Capping the force
            if(tdx > forceCapPerPos) tdx = forceCapPerPos
            else if(tdx < -forceCapPerPos) tdx = -forceCapPerPos
            if(tdy > forceCapPerPos) tdy = forceCapPerPos
            else if(tdy < -forceCapPerPos) tdy = -forceCapPerPos

            return Position(tdx, tdy)
        }

        override fun calculatePosAdjustmentFromEdges(scale : Double): Position {

        }

        override fun forceUpdate(dx: Double, dy: Double) {
            if(x.get() + dx < 0 || x.get() + dx > 1 || y.get() + dy < 0 || y.get() + dy > 1) {
                println("out of bounds force: $dx, $dy")
                if     (x.get() + dx < 0) x.set(0.0)
                else if(x.get() + dx > 1) x.set(1.0)
                if     (y.get() + dy < 0) y.set(0.0)
                else if(y.get() + dy > 1) y.set(1.0)
                return
            }
            x.set(x.get() + dx)
            y.set(y.get() + dy)
            //println("vertex: $v, force: $dx, $dy")
        }
    }
    val physics2 = object: Physics() {}
    abstract inner class Physics {
        fun simulate(iterations: Int = 1000){
            for(i in 0 until iterations){
                moveVerticesForces()
            }
        }

        private fun moveVerticesForces(){
            val adjustments = Array(vertices.size){vertices[it].calculatePositionAdjustment().add(vertices[it].calculateEdgeForces())}
            for((v,delta) in vertices.zip(adjustments)){
                val (dx,dy) = delta
                v.forceUpdate(dx, dy)
            }
        }

        abstract fun calculateAdjustmentFromPos(vertex: Vertex, scale: Double = 1.0): Position

        abstract fun calculatePosAdjustmentFromEdges(vertex: Vertex, scale: Double = 1.0): Position

    }


    //Gray out non-attached vertices and edges in the graph
    fun greyDetached(src: GraphicComponents<E>.Vertex) {
            for (vert in vertices) {
                vert.setColor(Color(0.0, 0.0, 1.0, 0.3))
            }
            for (edge in edges) {
                if (edge.v1 != src && edge.v2 != src) {
                    edge.setLineColor(Color.rgb(192, 192, 192, 0.8))
                    edge.setLabelColor(Color.GREY)
                } else {
                    edge.v1.let { if (it != src) it.setColor(Color.BLUE) }
                    edge.v2.let { if (it != src) it.setColor(Color.BLUE) }
                    edge.setLineColor(Color.GREEN, Color.RED, src)
                    edge.setLabelColor(Color.GREEN, Color.RED, src)
                }
            }
        }

    val clustering = object : Clustering() {}
    abstract inner class Clustering{
        fun moveClusters(clusters: Collection<Graph<E>>) {
            //convert clusters to List<List<Vertex>>
            val clustersGraphic = clusters.map { cluster ->
                ArrayList<GraphicComponents<E>.Vertex>().apply {
                    for (vert in cluster) {
                        add(vertices.find { it.v == vert } ?: continue)
                    }
                }
            }

            val numDimensionalSections = ceil(sqrt(clustersGraphic.size.toDouble())).toInt()

            for (xSections in 0 until numDimensionalSections) {
                for (ySections in 0 until numDimensionalSections) {
                    val cluster = try {
                        clustersGraphic[xSections * ySections]
                    } catch (_: Exception) {
                        break
                    }

                    for (v in cluster) {
                        v.x.set(v.x.get() * (1.0 / numDimensionalSections) + (xSections * (numDimensionalSections - 1)))
                        v.y.set(v.y.get() * (1.0 / numDimensionalSections) + (ySections * (numDimensionalSections - 1)))
                    }
                }
            }
        }
    }
}