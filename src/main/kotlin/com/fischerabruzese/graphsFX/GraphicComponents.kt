/* TODO:
    1. Create 2 lists of components for path ;;
    2. Create method that greys non-path objects
    3. Create method that ungreys all on non-path click
    4. Color path
*/

package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.Graph
import javafx.application.Platform
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
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.math.*
import kotlin.random.Random

class GraphicComponents<E: Any>(val graph: Graph<E>, val pane: Pane, val stringToVMap: HashMap<String, GraphicComponents<E>.Vertex>) {
    private val CIRCLE_RADIUS = 20.0

    private var selectedVertex: GraphicComponents<E>.Vertex? = null
    private val hitboxes = ArrayList<Circle>()
    internal var edges = ArrayList<Edge>()

    internal var currentPathVertices = LinkedList<Vertex>()
    internal var currentPathConnections = LinkedList<Edge.Connection>()

    @JvmName("dumpEdgePositions")
    private fun ArrayList<Edge>.dumpPositions() = this.map {
        Position(((it.v2.x.get()+it.v1.x.get())/2.0), ((it.v2.y.get()+it.v1.y.get())/2.0))
    }.toTypedArray()

    internal var vertices = ArrayList<Vertex>()
    @JvmName("dumpVertexPositions")
    private fun ArrayList<Vertex>.dumpPositions() = this.map { it.pos }.toTypedArray()



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
        //Position
            //x and y are between 0 and 1, everything should be modified in terms of these
        internal var x : DoubleProperty = SimpleDoubleProperty(xInit)
        internal var y : DoubleProperty = SimpleDoubleProperty(yInit)

        internal var pos
            get() = Position(x.get(), y.get())
            set(value){
                x.set(value.x)
                y.set(value.y)
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

        //Bindings
            //These are actually what get read by the components
        var vTranslateXBinding : DoubleBinding = pane.widthProperty().multiply(this.x).multiply(usablePercentPaneWidth).add(CIRCLE_RADIUS)
        var vtranslateYBinding : DoubleBinding = pane.heightProperty().multiply(this.y).multiply(usablePercentPaneHeight).add(CIRCLE_RADIUS)

        fun bindAll(){
            //Circle
            circle.translateXProperty().bind(vTranslateXBinding.subtract(CIRCLE_RADIUS))
            circle.translateYProperty().bind(vtranslateYBinding.subtract(CIRCLE_RADIUS))

            //Label
            label.translateXProperty().bind(vTranslateXBinding.subtract(CIRCLE_RADIUS))
            label.translateYProperty().bind(vtranslateYBinding.subtract(CIRCLE_RADIUS))

            label.textFill = Color.WHITE

            //Hitbox
            hitbox.translateXProperty().bind(vTranslateXBinding)
            hitbox.translateYProperty().bind(vtranslateYBinding)
        }
        
        //Dragging
        private var xDelta : Double = 0.0
        private var yDelta : Double = 0.0

        init{
            stringToVMap[v.toString()] = this

            bindAll()

            //Listeners
            hitbox.setOnMouseEntered { circle.fill = Color.GREEN }
            hitbox.setOnMouseExited { circle.fill = Color.BLUE}

            hitbox.setOnMousePressed {
                dragStart(it)
                greyDetached(this)
                selectedVertex = this
                if(!currentPathVertices.contains(this)){
                    ungreyEverything()
                    currentPathVertices.clear()
                    currentPathConnections.clear()
                }
                circle.fill = Color.RED
            }
            hitbox.setOnMouseDragged { drag(it) }
            hitbox.setOnMouseReleased { ungreyEverything(); circle.fill = Color.GREEN; selectedVertex = null }
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

        fun grey(){
            setColor(Color(0.0, 0.0, 1.0, 0.3))
        }
        fun ungrey(){
            setColor(Color.BLUE)
        }

        fun setColor(color: Color) {
            circle.fill = color
        }

        fun setOutline(color: Color){
            circle.stroke = color
            circle.strokeWidth = 5.0
        }

        fun clearOutline(){
            circle.stroke = Color.TRANSPARENT
            circle.strokeWidth = 0.0
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
                val dyTotal = to.vtranslateYBinding.subtract(from.vtranslateYBinding)
                val dxTotal = to.vTranslateXBinding.subtract(from.vTranslateXBinding)

                val length = Bindings.createDoubleBinding(
                    { sqrt(dyTotal.get().pow(2) + dxTotal.get().pow(2)) },
                    dyTotal, dxTotal
                )

                val dy = dxTotal.multiply(CIRCLE_RADIUS / 4).divide(length).multiply(-1)
                val dx = dyTotal.multiply(CIRCLE_RADIUS / 4).divide(length)

                line.startXProperty().bind(from.vTranslateXBinding.add(dx))
                line.startYProperty().bind(from.vtranslateYBinding.add(dy))
                line.endXProperty().bind(to.vTranslateXBinding.add(dx))
                line.endYProperty().bind(to.vtranslateYBinding.add(dy))

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

            fun boldLine(){
                line.strokeWidth = 5.0
            }
            fun unboldLine() {
                line.strokeWidth = 1.0
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

                    val dyTotal = v2.vtranslateYBinding.subtract(v1.vtranslateYBinding)
                    val dxTotal = v2.vTranslateXBinding.subtract(v1.vTranslateXBinding)

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

        fun grey(){
            setLineColor(Color.rgb(192, 192, 192, 0.8))
            setLabelColor(Color.GREY)
        }
        fun ungrey(){
            setLineColor(Color.rgb(0, 0, 0, 0.6))
            setLabelColor(Color.BLACK)
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

    abstract inner class Physics(var on: Boolean, var speed: Double) {
        fun simulate() {
            Thread {
                while(on){
                    val latch = CountDownLatch(1) // Initialize with a count of 1
                    val nextFrame = generateFrame(speed, unaffected = listOfNotNull(selectedVertex))
                    Platform.runLater {
                        pushFrame(nextFrame)
                        latch.countDown() //signal that Platform has executed our frame
                    }
                    latch.await() //wait for platform to execute our frame
                }
            }.start()
        }
        /**
         * @param at The position at which to calculate the adjustment
         * @param froms The list of triples containing the source position, weight, and force magnitude function(defaults to 1/radiusSquared)
         * @param forceCapPerPos The maximum force cap per position, default is 0.1
         * @return The displacement calculated based on the provided parameters
         */
        abstract fun calculateAdjustmentAtPos(at: Position, froms: List<Pair<Position, (Double) -> Double>>, forceCapPerPos: Double = 0.01): Displacement

        abstract fun generateFrame(speed: Double, unaffected: List<GraphicComponents<E>.Vertex> = emptyList(), uneffectors: List<GraphicComponents<E>.Vertex> = emptyList()): Array<Displacement>

        abstract fun pushFrame(displacementArr: Array<Displacement>)
    }
    val physics = object: Physics(false, 0.0) {

        /** Calculates the change in position of [at] as a result of its distance from each [froms] */
        override fun calculateAdjustmentAtPos(at: Position, froms: List<Pair<Position, (Double) -> Double>>, forceCapPerPos: Double): Displacement{
            val displacement = Displacement(0.0, 0.0)

            //Adding adjustments
            for((pos, fieldEq) in froms){
                val scaleFactor = 0.00006 / (vertices.size + edges.size)
                 if(at == pos) return Displacement(Random.nextDouble(-0.000001, 0.000001), Random.nextDouble(-0.000001, 0.000001)) //Nudge slightly if at the same position
                displacement += calculateAdjustmentAtPos(at, pos, scaleFactor, fieldEq)
            }

            //Capping the total force, add some variation
            displacement.constrainBetween(
                forceCapPerPos, //+ Random.nextDouble(-forceCapPerPos/10, forceCapPerPos/10),
                -forceCapPerPos //+ Random.nextDouble(-forceCapPerPos/10, forceCapPerPos/10)
            )
            return displacement
        }

        /** Calculates the change in position of [at] based on [from] */
        private fun calculateAdjustmentAtPos(at: Position, from: Position, scaleFactor: Double, magnitudeFormula: (radiusSquared: Double) -> Double = { 1 / it }): Displacement {
            //Window scalers
            val xScaler = 2 * pane.width / (pane.width + pane.height)
            val yScaler = 2 * pane.height / (pane.width + pane.height)

            val dx = (at.x - from.x) * xScaler
            val dy = (at.y - from.y) * yScaler
            val radiusSquared = dx.pow(2) + dy.pow(2)

            val magnitude = scaleFactor * magnitudeFormula(radiusSquared)
            val angle = atan2(dy, dx)

            val fdx = magnitude * cos(angle)
            val fdy = magnitude * sin(angle)

            return Displacement(fdx, fdy)
        }

        /** Hands you an array of all the displacements in the current frame */
        override fun generateFrame(speed: Double, unaffected: List<GraphicComponents<E>.Vertex>, uneffectors: List<GraphicComponents<E>.Vertex>): Array<Displacement>{
            val max = 200
            val scaleFactor = speed.pow(4) * max

            val displacements = Array(vertices.size) { Displacement(0.0, 0.0) }
            for((id, vertex) in vertices.withIndex()){
                if(unaffected.contains(vertex)) continue
                val effectors = LinkedList<Pair<Position, (Double) -> Double>>()

                val vertexRepulsionField: (Double) -> Double = { rSqr ->  (scaleFactor / rSqr)}
                val vertexAttractionField: (Double) -> Double = { rSqr ->  (-scaleFactor * rSqr.pow(2))}

                val unconnectedVertexField: (Double) -> Double = { rSqr -> 1 * vertexRepulsionField(rSqr)}
                val singleConnectedVertexField: (Double) -> Double = { rSqr -> 1000 * vertexAttractionField(rSqr) + 0.5 * vertexRepulsionField(rSqr)}
                val doubleConnectedVertexField: (Double) -> Double = { rSqr -> 2000 * vertexAttractionField(rSqr) + 0.5 * vertexRepulsionField(rSqr)}
                val edgeFieldEquation: (Double) -> Double = { rSqr ->  0.5 * vertexRepulsionField(rSqr) }
                val wallFieldEquation: (Double) -> Double = { rSqr ->  0.5 * vertexRepulsionField(rSqr) }

                //vertices
                vertices
                    .filterNot{ uneffectors.contains(it) || vertex === it }
                    .mapTo(effectors) { vertexEffector ->
                        when(graph.bidirectionalConnections(vertexEffector.v, vertex.v)){
                                1 -> Pair(vertexEffector.pos, singleConnectedVertexField)
                                2 -> Pair(vertexEffector.pos, doubleConnectedVertexField)
                                else -> Pair(vertexEffector.pos, unconnectedVertexField)
                            }
                    }

                edges.zip(edges.dumpPositions())
                    .filterNot { (e, _) -> e.v1 == vertex || e.v2 == vertex } //should I add another filter for effector stuff
                    .mapTo(effectors) { (_, pos) -> Pair(pos, edgeFieldEquation) }

                //walls
                listOf(Position(1.0, vertex.pos.y), Position(0.0, vertex.pos.y), Position(vertex.pos.x, 1.0), Position(vertex.pos.x, 0.0))
                    .mapTo(effectors){ wallEffectorPos -> Pair(wallEffectorPos, wallFieldEquation)}

                displacements[id] += calculateAdjustmentAtPos(vertex.pos, effectors)
            }

            return displacements
        }

        /** Updates every vertex with the frames displacements */
        override fun pushFrame(displacementArr: Array<Displacement>){
            for((vertexIndex, displacement) in displacementArr.withIndex()){
                vertices[vertexIndex].pos += displacement
            }
        }
    }
/*
    fun greyNonPath() {
        for (vert in vertices) {
            if(!currentPathVertices.contains(vert))
                vert.grey()
        }
        for (edge in edges){
            if(!currentPathEdges.contains(edge)){
                edge.grey()
            }
        }
    }

 */


    //Grey out non-attached vertices and edges in the graph
    fun greyDetached(src: GraphicComponents<E>.Vertex) {
        for (vert in vertices.filterNot{ it == src }) {
            vert.grey()
        }
        for (edge in edges) {
            if (edge.v1 != src && edge.v2 != src) {
                edge.grey()
            } else {
                edge.v1.let { if (it != src) it.ungrey()}
                edge.v2.let { if (it != src) it.ungrey() }
                edge.setLineColor(Color.GREEN, Color.RED, src)
                edge.setLabelColor(Color.GREEN, Color.RED, src)
            }
        }
    }

    //Ungrey everything by setting line and label colors for edges and color for vertices.
    fun ungreyEverything(){
        for(edge in edges){
            edge.ungrey()
        }
        for (vert in vertices){
            vert.ungrey()
        }
    }

    fun greyEverything(){
        for(edge in edges){
            edge.grey()
        }
        for (vert in vertices){
            vert.grey()
        }
    }

    fun makePathFancyColors() {
        val startColor = Color.ORANGE
        val endColor = Color.PURPLE
        val segments = currentPathVertices.size + currentPathConnections.size
        val currColor = startColor
        val connections = LinkedList(currentPathConnections)
        val verts = LinkedList(currentPathVertices)

        while(!verts.isEmpty()){
            val vert = verts.removeFirst()
            vert.setColor(currColor)
            //update currColor
            
            if(!connections.isEmpty()) {
                val connection = connections.removeFirst()
                connection.setLineColor(currColor)
                connection.boldLine()
                //update currColor
            }
        }
    }


//    val clustering = object : Clustering() {}
//    abstract inner class Clustering{
//        fun moveClusters(clusters: Collection<Graph<E>>) {
//            //convert clusters to List<List<Vertex>>
//            val clustersGraphic = clusters.map { cluster ->
//                ArrayList<GraphicComponents<E>.Vertex>().apply {
//                    for (vert in cluster) {
//                        add(vertices.find { it.v == vert } ?: continue)
//                    }
//                }
//            }
//
//            val numDimensionalSections = ceil(sqrt(clustersGraphic.size.toDouble())).toInt()
//
//            for (xSections in 0 until numDimensionalSections) {
//                for (ySections in 0 until numDimensionalSections) {
//                    val cluster = try {
//                        clustersGraphic[xSections * ySections]
//                    } catch (_: Exception) {
//                        break
//                    }
//
//                    for (v in cluster) {
//                        v.x.set(v.x.get() * (1.0 / numDimensionalSections) + (xSections * (numDimensionalSections - 1)))
//                        v.y.set(v.y.get() * (1.0 / numDimensionalSections) + (ySections * (numDimensionalSections - 1)))
//                    }
//                }
//            }
//        }
//    }
}