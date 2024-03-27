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
import javafx.scene.shape.StrokeType
import javafx.scene.text.Font
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.math.*
import kotlin.random.Random

/**
 * Represents all the components of a graph in the graphics pane
 * @param graph graph contained in [pane]
 * @param pane the pane to contain the graph
 */
class GraphicComponents<E: Any>(
    var graph: Graph<E>,
    val pane: Pane,
) {
    //Constants
    companion object {
        private val CIRCLE_RADIUS = 20.0
    }

    //Vertex + edge storage
    private var selectedVertex: GraphicComponents<E>.Vertex? = null
    private val hitboxes = ArrayList<Circle>()
    private var edges = ArrayList<Edge>()
    internal var vertices = ArrayList<Vertex>()
    @JvmName("dumpEdgePositions")
    private fun ArrayList<Edge>.dumpPositions() = this.map { Position(((it.v2.x.get()+it.v1.x.get())/2.0), ((it.v2.y.get()+it.v1.y.get())/2.0)) }.toTypedArray()
    @JvmName("dumpVertexPositions")
    private fun ArrayList<Vertex>.dumpPositions() = this.map { it.pos }.toTypedArray()

    //access vertices from a string
    val stringToVMap = HashMap<String, GraphicComponents<E>.Vertex>()

    //Path storage
    internal var currentPathVertices = LinkedList<Vertex>()
    internal var currentPathConnections = LinkedList<Edge.Connection>()

    /**
     * Clear the graphic components and remake them from the graph
     */
    fun draw() {
        //Clear the pane
        pane.children.clear()

        //Get list of vertices from graph
        val verticesElements = ArrayList(graph.getVertices())
        //Map the vertices to graphic representation using the old vertex positions if found or random positions if not
        val graphicVertices = verticesElements.mapIndexed { index, vertex ->
            Vertex(
                vertex,
                vertices.getOrNull(index)?.x?.get() ?: Math.random(),
                vertices.getOrNull(index)?.y?.get() ?: Math.random()
            )
        }

        //Iterate through the vertices to create edges
        val edgeElements = ArrayList<Edge>()
        for((v1pos, vertex1) in verticesElements.withIndex()){
            for(vertex2 in verticesElements.subList(v1pos, verticesElements.size)){
                val v1tov2Weight = graph[vertex1, vertex2] ?: -1
                val v2tov1Weight = graph[vertex2, vertex1] ?: -1
                if(v2tov1Weight > -1 || v1tov2Weight > -1)
                    edgeElements.add(Edge(graphicVertices[v1pos], graphicVertices[verticesElements.indexOf(vertex2)], v1tov2Weight, v2tov1Weight))
            }
        }

        //Reassign to new edges and vertices
        edges = edgeElements
        vertices = ArrayList(graphicVertices)

        //Add the elements to the pane
        pane.children.addAll(edgeElements)
        pane.children.addAll(graphicVertices)
        pane.children.addAll(hitboxes)
    }

    /**
     * Different properties you can use to color a vertex
     */
    enum class ColorType{
        PATH, SELECTED, HOVERED, GREYED, CLUSTERED, DEFAULT
    }

    /**
     * Represents a graphical vertex in the window.
     *
     * @property v The value of the vertex.
     * @property x The x position of the vertex, between 0 and 1.
     * @property y The y position of the vertex, between 0 and 1.
     */
    inner class Vertex(val v: E, xInit: Double, yInit: Double): StackPane() {
        /*Position*/
        //x and y are between 0 and 1, everything should be modified in terms of these
        internal var x : DoubleProperty = SimpleDoubleProperty(xInit)
        internal var y : DoubleProperty = SimpleDoubleProperty(yInit)

        //The current position of the vertex
        internal var pos
            get() = Position(x.get(), y.get())
            set(value){
                x.set(value.x)
                y.set(value.y)
            }

        //Graphical Display Components
        private val circle = Circle(Companion.CIRCLE_RADIUS, Color.BLUE)
        private val label = Label(v.toString())
        internal val hitbox = Circle(Companion.CIRCLE_RADIUS, Color.TRANSPARENT)

        /*Location Bindings*/
        private val usablePercentPaneWidth: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * Companion.CIRCLE_RADIUS / pane.widthProperty().get() },
            pane.widthProperty()
        )
        private val usablePercentPaneHeight: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * Companion.CIRCLE_RADIUS / pane.heightProperty().get() },
            pane.heightProperty()
        )
        //These are actually what get read by the components
        var vTranslateXBinding : DoubleBinding = pane.widthProperty().multiply(this.x).multiply(usablePercentPaneWidth).add(Companion.CIRCLE_RADIUS)
        var vTranslateYBinding : DoubleBinding = pane.heightProperty().multiply(this.y).multiply(usablePercentPaneHeight).add(Companion.CIRCLE_RADIUS)

        private fun bindAll(){
            //Circle
            val offsetBinding: DoubleBinding = circle.strokeWidthProperty().add(Companion.CIRCLE_RADIUS)
            circle.translateXProperty().bind(vTranslateXBinding.subtract(offsetBinding))
            circle.translateYProperty().bind(vTranslateYBinding.subtract(offsetBinding))
            circle.strokeType = StrokeType.OUTSIDE
            clearOutline()

            //Label
            label.translateXProperty().bind(vTranslateXBinding.subtract(offsetBinding))
            label.translateYProperty().bind(vTranslateYBinding.subtract(offsetBinding))

            label.textFill = Color.WHITE

            //Hitbox
            hitbox.translateXProperty().bind(vTranslateXBinding)
            hitbox.translateYProperty().bind(vTranslateYBinding)
        }
        
        /*Hitbox listeners*/
        private fun setHitboxListeners(){
            hitbox.setOnMouseEntered { if(currentPathVertices.isEmpty()) setHovered()}
            hitbox.setOnMouseExited { if(currentPathVertices.isEmpty()) clearColor(ColorType.HOVERED) }

            hitbox.setOnMousePressed {
                draggingFlag = true
                dragStart(it)
                selectedVertex = this
                if(!currentPathVertices.contains(this)){
                    ungreyEverything()
                    currentPathVertices.clear()
                    currentPathConnections.clear()
                    greyDetached(this) //normal selection greying
                    setSelected()
                }
            }
            hitbox.setOnMouseDragged { drag(it) }
            hitbox.setOnMouseReleased {
                clearColor(ColorType.SELECTED)
                if(!currentPathVertices.contains(this)) {
                    ungreyEverything()
                }
                selectedVertex = null
                draggingFlag = false
            }
            hitbox.pickOnBoundsProperty().set(true)

            hitboxes.add(hitbox)
        }
        
        /*Dragging*/
        private var xDelta : Double = 0.0
        private var yDelta : Double = 0.0
        internal var draggingFlag = false

        /**
         * Stores the mouse position on the vertex so that it doesn't matter where you click on the vertex
         */
        private fun dragStart(event : MouseEvent) {
            xDelta = event.sceneX / pane.width - x.get()
            yDelta = event.sceneY / pane.height - y.get()
        }

        /**
         * Updates the location of the vertex that's being dragged
         */
        private fun drag(event : MouseEvent) {
            x.set((event.sceneX / pane.width - xDelta).let{if(it > 1) 1.0 else if(it < 0) 0.0 else it})
            y.set((event.sceneY / pane.height - yDelta).let{if(it > 1) 1.0 else if(it < 0) 0.0 else it})
        }

        /*Coloring*/
        private val colorPriorityMap = hashMapOf(
            ColorType.PATH to 0,
            ColorType.SELECTED to 1,
            ColorType.HOVERED to 2,
            ColorType.GREYED to 3,
            ColorType.CLUSTERED to 4,
            ColorType.DEFAULT to 5
        )

        //The colors are stored via priority, where when one color is deactivated it will fall through until it's a colored field is met
        private val colorStorage = Array<Color?>(colorPriorityMap.size){ null}.apply{ this[colorPriorityMap[ColorType.DEFAULT]!!] = Color.BLUE }
        /**
         * The priority of the currently active color (lower number = higher priority)
         */
        private var currentColorPriority = colorPriorityMap[ColorType.DEFAULT]!!

        /**
         * Equivalent to the highest priority color that isn't null.
         * @return The priority of the current color.
         */
        private fun highestActivePriority(): Int {
            colorStorage.forEachIndexed() { index, color ->
                if (color != null) return index
            }
            return colorPriorityMap[ColorType.DEFAULT]!!
        }

        /**
         * Change the color of a certain [ColorType]
         * @param type The [ColorType] to change
         * @param color The new color. [ColorType.PATH] is the only type
         */
        fun setColor(type : ColorType, color : Color? = null){
            val priority = colorPriorityMap[type]!!

            //Update color storage to new color
            colorStorage[priority] = when (type) {
                ColorType.PATH -> color
                ColorType.SELECTED -> Color.RED
                ColorType.HOVERED -> Color.GREEN
                ColorType.GREYED -> grey(colorStorage[highestActivePriority()])
                ColorType.CLUSTERED -> color
                    .also{ if (colorStorage[colorPriorityMap[ColorType.GREYED]!!] != null) setColor(ColorType.GREYED) } //if grey is active, update it to a new grey based on this color
                ColorType.DEFAULT -> Color.BLUE
                    .also{ if(colorStorage[colorPriorityMap[ColorType.GREYED]!!] != null) setColor(ColorType.GREYED) } //if grey is active, update it to a new grey based on this color
            }

            //Decide if this color is the new active color
            if(priority <= currentColorPriority) {
                setColor(colorStorage[priority]!!)
                currentColorPriority = priority
            }
        }

        /**
         * Internal recoloring only everything should be managed through priority
         */
        private fun setColor(color: Color) {
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

        private fun grey(color : Color?) : Color?{
            val greyFactor = 0.5
            if(color == null) return null
            val newColor = Color.color(
                (color.red + ((Color.LIGHTGREY.red - color.red)* greyFactor)),
                (color.green + ((Color.LIGHTGREY.green - color.green)* greyFactor)),
                (color.blue + ((Color.LIGHTGREY.blue - color.blue)* greyFactor))
            )

            return Color(newColor.red, newColor.green, newColor.blue, 0.3)
        }

        /**
         * Update colors when the vertex is selected
         */
        private fun setSelected(){
            setColor(ColorType.SELECTED)
            clearColor(ColorType.GREYED)
        }

        /**
         * Update colors when the vertex is hovered
         */
        private fun setHovered(){
            setColor(ColorType.HOVERED)
            clearColor(ColorType.GREYED)
        }

        /**
         * Clears the color of a certain [ColorType]
         * @param type The [ColorType] to clear
         */
        fun clearColor(type : ColorType){
            val priority = colorPriorityMap[type]!!
            colorStorage[priority] = null
            if(priority == currentColorPriority) {
                val newGreatestPriority = highestActivePriority()
                setColor(colorStorage[newGreatestPriority]!!)
                currentColorPriority = newGreatestPriority
            }
        }

        init {
            //Add yourself to lookup table
            stringToVMap[v.toString()] = this
            //Bind all properties
            bindAll()
            //Become self-aware
            setHitboxListeners()
            //Start a family
            children.addAll(circle, label)
        }

        override fun toString(): String {
            return v.toString()
        }

        fun copy(): Vertex {
            return Vertex(v,x.get(), y.get())
        }
    }

    /**
     * Represents a graphical edge in the window. Each edge represents the possible connections between two vertices. If a connection only exists in one direction, the weight is -1 for the non-existent connection.
     *
     * @property v1 The first vertex of the edge.
     * @property v2 The second vertex of the edge.
     * @param v1tov2 The weight of the edge between v1 and v2.
     * @param v2tov1 The weight of the edge between v2 and v1.
     */
    inner class Edge(val v1 : Vertex, val v2 : Vertex, v1tov2 : Int, v2tov1: Int) : StackPane(){
        /* Connections */
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

        /**
         * Makes both the connections in this edge grey
         */
        fun grey(){
            setLineColor(Color.rgb(192, 192, 192, 0.8))
            setLabelColor(Color.GREY)
        }

        /**
         * Makes both the connections in this edge black (but slightly transparent) and remove any bolding.
         */
        fun ungrey(){
            setLineColor(Color.rgb(0, 0, 0, 0.6))
            setLabelColor(Color.BLACK)
            v1tov2Connection.unboldLine()
            v2tov1Connection.unboldLine()
        }

        fun hideLabels(){
            v1tov2Connection.hideLabel()
            v2tov1Connection.hideLabel()
        }

        fun showLabels(){
            v1tov2Connection.showLabel()
            v2tov1Connection.showLabel()
        }

        /**
         * Make both connections in this edge whatever color you want
         */
        fun setLineColor(color: Color) {
            v1tov2Connection.setLineColor(color)
            v2tov1Connection.setLineColor(color)
        }

        /**
         * @param outBoundColor the new color for the outbound connection
         * @param inboundColor the new color ro the inbound connection
         * @param from the vertex you want to calculate inbound and outbound from. Must be either [v1] or [v2].
         */
        fun setLineColor(outBoundColor: Color, inboundColor: Color, from: Vertex) {
            if(v1 == from){
                v1tov2Connection.setLineColor(outBoundColor)
                v2tov1Connection.setLineColor(inboundColor)
            } else if (v2 == from) {
                v1tov2Connection.setLineColor(inboundColor)
                v2tov1Connection.setLineColor(outBoundColor)
            }
        }

        /**
         * Make both connections' labels in this edge whatever color you want
         */
        fun setLabelColor(color: Color) {
            v1tov2Connection.setLabelColor(color)
            v2tov1Connection.setLabelColor(color)
        }

        /**
         * @param outBoundColor the new color for the outbound connection's label
         * @param inboundColor the new color ro the inbound connection's label
         * @param from the vertex you want to calculate inbound and outbound from. Must be either [v1] or [v2].
         */
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

        /**
         * Represents the connection of two vertices in direction, from -> to
         * @param from the origin of the connection
         * @param to the designation of the connection
         * @param weight the weight of the connection
         * @param mirror when creating 2 connections one of them should be mirrored so that they don't attempt to draw themselves in the same place.
         */
        inner class Connection(from : Vertex, to : Vertex, weight: Int, mirror : Boolean) : Pane() {
            /*Components*/
            private val line = Line()

            private var director1 : Director
            private var director2 : Director
            private var label = Label(weight.toString())

            init {
                //Create bindings of the displacement between and to on the screen
                val dyTotal = to.vTranslateYBinding.subtract(from.vTranslateYBinding)
                val dxTotal = to.vTranslateXBinding.subtract(from.vTranslateXBinding)

                //Create a binding of the distance between the 2 vertices
                val length = Bindings.createDoubleBinding(
                    { sqrt(dyTotal.get().pow(2) + dxTotal.get().pow(2)) },
                    dyTotal, dxTotal
                )

                //Calculate a displacement from the center of the vertex to map the start and end points to
                val dy = dxTotal.multiply(Companion.CIRCLE_RADIUS / 4).divide(length).multiply(-1)
                val dx = dyTotal.multiply(Companion.CIRCLE_RADIUS / 4).divide(length)

                //Bind the start and end properties
                line.startXProperty().bind(from.vTranslateXBinding.add(dx))
                line.startYProperty().bind(from.vTranslateYBinding.add(dy))
                line.endXProperty().bind(to.vTranslateXBinding.add(dx))
                line.endYProperty().bind(to.vTranslateYBinding.add(dy))

                //Create directions to place on the connection, one at 1/3 of the line and one at 2/3 of the line
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
                line.strokeWidth = 3.0
                director1.boldLine()
                director2.boldLine()
            }
            fun unboldLine() {
                line.strokeWidth = 1.0
                director1.unboldLine()
                director2.unboldLine()
            }

            fun setLabelColor(color : Color) {
                label.textFill = color
            }

            fun hideLabel() {
                label.isVisible = false
            }

            fun showLabel() {
                label.isVisible = true
            }

            fun setWeight(weight : String) {
                label.text = weight
            }

            /**
             * A chevron placed on the line of a connection indicating direction
             * @param posX the x location of the chevron's tip
             * @param posY the y location of the chevron's tip
             * @param mirror determines the direction of the chevron
             */
            inner class Director(posX : DoubleBinding, posY : DoubleBinding, mirror: Boolean) : Pane() {
                //The 2 lines of the chevron
                private val line1 = Line()
                private val line2 = Line()

                init {
                    //Bind start positions
                    line1.startXProperty().bind(posX)
                    line1.startYProperty().bind(posY)
                    line2.startXProperty().bind(posX)
                    line2.startYProperty().bind(posY)

                    //Calculate end positions
                    val dyTotal = v2.vTranslateYBinding.subtract(v1.vTranslateYBinding)
                    val dxTotal = v2.vTranslateXBinding.subtract(v1.vTranslateXBinding)

                    val theta = Bindings.createDoubleBinding(
                        { atan2(dyTotal.get(), dxTotal.get()) },
                        dyTotal, dxTotal
                    )

                    val dx1 = Bindings.createDoubleBinding(
                        { Companion.CIRCLE_RADIUS /4.8 * cos(theta.get() + (PI /4)) },
                        theta
                    )
                    val dy1 = Bindings.createDoubleBinding(
                        { Companion.CIRCLE_RADIUS /4.8 * sin(theta.get() + (PI /4)) },
                        theta
                    )
                    val dx2 = Bindings.createDoubleBinding(
                        { Companion.CIRCLE_RADIUS /4.8 * cos(theta.get() - (PI /4)) },
                        theta
                    )
                    val dy2 = Bindings.createDoubleBinding(
                        { Companion.CIRCLE_RADIUS /4.8 * sin(theta.get() - (PI /4)) },
                        theta
                    )

                    val endX1 = posX.add(dx1.multiply(if(mirror) -1 else 1))
                    val endY1 = posY.add(dy1.multiply(if(mirror) -1 else 1))
                    val endX2 = posX.add(dx2.multiply(if(mirror) -1 else 1))
                    val endY2 = posY.add(dy2.multiply(if(mirror) -1 else 1))

                    //Bind end positions
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

                fun boldLine() {
                    line1.strokeWidth = 3.0
                    line2.strokeWidth = 3.0
                }
                fun unboldLine() {
                    line1.strokeWidth = 1.0
                    line2.strokeWidth = 1.0
                }
            }
        }
    }

    /* Display */
    /**
     * Make all weights on the graph invisible (unweighted graph)
     */
    fun hideWeight() {
        for(edge in edges){
            edge.hideLabels()
        }
    }
    fun showWeight(){
        for(edge in edges){
            edge.showLabels()
        }
    }

    /* PHYSICS */
    /**
     * An object that represent a physics model for a some objects at some positions
     * @param speed determines the speed of the simulation. This could be implemented by modifying the fps or the effect of each frame.
     */
    abstract inner class Physics(var speed: Double = 0.0) {
        private var ghostVertices = ArrayList(vertices.map { it to it.pos })
        private val simulationThreadGroup = ThreadGroup("Simulation Threads")
        private var simulationThreads = LinkedList<Thread>()

        /**
         * Opens a thread that will generate and push frames to the gui at [speed] until [on] is false
         */
        private fun simulate() {
            ghostVertices = ArrayList(vertices.map { it to it.pos })
            Thread(simulationThreadGroup, {
                while(!Thread.interrupted()){
                    try {
                        pushGhostFrame(generateFrame(speed, unaffected = listOfNotNull(selectedVertex), verticesPos = ghostVertices.toList()))
                    } catch (ex: Exception) {
                        when(ex) {
                            is NoSuchElementException, is IndexOutOfBoundsException -> {
                                Platform.runLater {
                                    stopSimulation()
                                    startSimulation()
                                }
                                return@Thread
                            }
                            else -> throw ex
                        }
                    }
                    //Thread.sleep(1)
                }
            }, "Ghost Frame Pusher").also{Platform.runLater{simulationThreads.add(it)}}.start()


            Thread(simulationThreadGroup, {
                val thisThread = Thread.currentThread()
                while (!Thread.interrupted()) {
                    val latch = CountDownLatch(1) // Initialize with a count of 1
                    Platform.runLater {
                        try {
                            pushRealFrame()
                        } catch (ex: Exception) {
                            when(ex) {
                                is NoSuchElementException, is IndexOutOfBoundsException -> {
                                    stopSimulation()
                                    startSimulation()
                                    return@runLater //Don't count down latch and cause a InterruptedException in thread
                                }
                                else -> throw ex
                            }
                        }
                        latch.countDown() //signal that Platform has executed our frame
                    }
                    try { latch.await() }
                    catch (e: InterruptedException) { return@Thread } //wait for platform to execute our frame
                }
            }, "Real Frame Pusher").also{Platform.runLater{simulationThreads.add(it)}}.start()
        }

        fun stopSimulation(){
            for(t in simulationThreads){
                t.interrupt()
                t.join() //wait for each thread to die
            }
            ghostVertices = ArrayList()
            simulationThreads = LinkedList<Thread>()
        }

        fun isActive(): Boolean{
            return simulationThreads.isNotEmpty()
        }

        /**
         * Starts the simulation if it is inactive
         * @return true if the simulation was inactive and has been started. False if the simulation was already active
         */
        fun startSimulation(): Boolean{
            if(isActive()) return false

            simulate()
            return true
        }

        /**
         * Sums all the displacements from all the effectors of [at]
         *
         * @param at The position at which to calculate the adjustment
         * @param froms A list of positions and lambda that calculates the displacement that position should cause on an object given the distance-squared between the 2 objects
         * @param forceCapPerPos The maximum force cap per position, default is 0.1
         * @return The displacement calculated based on the provided parameters
         */
        abstract fun calculateAdjustmentAtPos(at: Position, froms: List<Pair<Position, (Double) -> Double>>, forceCapPerPos: Double = 0.1): Displacement

        /**
         * @param speed the speed (ie magnitude) of the calculations
         * @param unaffected all the vertices that aren't moved
         * @param uneffectors all the vertices that do not cause movements
         * @returns An array of displacements such that the displacement at each index correspondents with [vertices]
         */
        abstract fun generateFrame(speed: Double, unaffected: List<GraphicComponents<E>.Vertex> = emptyList(), uneffectors: List<GraphicComponents<E>.Vertex> = emptyList(), verticesPos: List<Pair<GraphicComponents<E>.Vertex,Position>> = vertices.map{it to it.pos}): Array<Displacement>

        /** Updates every vertex with the calculated displacements */
        private fun pushRealFrame(){
            for(vertexIndex in vertices.indices){
                if(!vertices[vertexIndex].draggingFlag){
                    vertices[vertexIndex].pos = ghostVertices[vertexIndex].second
                }
            }
            ghostVertices = ArrayList(vertices.map { it to it.pos }) //reset ghost vertices
        }

        private fun pushGhostFrame(displacementArr: Array<Displacement>){
            for((vertexIndex, displacement) in displacementArr.withIndex()) {
                if(!ghostVertices[vertexIndex].first.draggingFlag){
                    ghostVertices[vertexIndex] = ghostVertices[vertexIndex].let { it.first to it.second.plus(displacement) }
                    ghostVertices[vertexIndex] = ghostVertices[vertexIndex].let {
                        it.first to Position( //recreating position will constrain position data, but tbh position class should be rewritten anyway its kinda trash
                            it.second.x,
                            it.second.y
                        )
                    }
                }
            }
        }
    }

    /**
     * The custom physics object used for this graphic
     */
    val physicsC = object: Physics(0.0) {
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

        /**
         * Helper function to calculate the adjustment of [at] as a result of [from] via [magnitudeFormula] with a given [scaleFactor]
         */
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

            return Displacement(fdx, fdy, 0.9, -0.9)
        }

        override fun generateFrame(speed: Double, unaffected: List<GraphicComponents<E>.Vertex>, uneffectors: List<GraphicComponents<E>.Vertex>, verticesPos: List<Pair<GraphicComponents<E>.Vertex,Position>>): Array<Displacement>{
            val max = 2000
            val scaleFactor = speed.pow(4) * max

            val displacements = Array(vertices.size) { Displacement(0.0, 0.0) }
            for((affectedVertex, affectedPos) in verticesPos){
                if(unaffected.contains(affectedVertex)) continue
                val effectors = LinkedList<Pair<Position, (Double) -> Double>>()

                val vertexRepulsionField: (Double) -> Double = { rSqr ->  (scaleFactor / rSqr)}
                val vertexAttractionField: (Double) -> Double = { rSqr ->  (-scaleFactor * rSqr.pow(2))}

                val unconnectedVertexField: (Double) -> Double = { rSqr -> 1 * vertexRepulsionField(rSqr)}
                val singleConnectedVertexField: (Double) -> Double = { rSqr -> 1000 * vertexAttractionField(rSqr) + 0.5 * vertexRepulsionField(rSqr)}
                val doubleConnectedVertexField: (Double) -> Double = { rSqr -> 2000 * vertexAttractionField(rSqr) + 0.5 * vertexRepulsionField(rSqr)}
                val edgeFieldEquation: (Double) -> Double = { rSqr ->  0.5 * vertexRepulsionField(rSqr) }
                val wallFieldEquation: (Double) -> Double = { rSqr ->  0.5 * vertexRepulsionField(rSqr) }

                //vertices
                val (effectorVerts, effectorPos) = verticesPos.filterNot{ (uneffectors.contains(it.first) || affectedVertex === it.first) }.unzip()
                effectorVerts
                    .mapIndexedTo(effectors) { i, vertexEffector ->
                        when(graph.countEdgesBetween(vertexEffector.v, affectedVertex.v)){
                                1 -> Pair(effectorPos[i], singleConnectedVertexField)
                                2 -> Pair(effectorPos[i], doubleConnectedVertexField)
                                else -> Pair(effectorPos[i], unconnectedVertexField)
                            }
                    }

//                edges.zip(edges.dumpPositions())
//                    .filterNot { (e, _) -> e.v1 == affectedVertex || e.v2 == affectedVertex } //should I add another filter for effector stuff
//                    .mapTo(effectors) { (_, effectorPos) -> Pair(effectorPos, edgeFieldEquation) }

                //walls
                listOf(Position(1.0, affectedPos.y), Position(0.0, affectedPos.y), Position(affectedPos.x, 1.0), Position(affectedPos.x, 0.0))
                    .mapTo(effectors){ wallEffectorPos -> Pair(wallEffectorPos, wallFieldEquation)}

                displacements[vertices.indexOf(affectedVertex)] += calculateAdjustmentAtPos(affectedPos, effectors)
            }

            return displacements
        }
    }


    /* GRAPHICS GRAPH COLORING */

    fun greyEverything(){
        for(edge in edges){
            edge.grey()
        }
        for (vert in vertices){
            vert.setColor(ColorType.GREYED)
        }
    }

    /**
     * Grey's out non-attached vertices and edges in the graph
     */
    fun greyDetached(src: GraphicComponents<E>.Vertex) {
        for (vert in vertices.filterNot{ it == src }) {
            vert.setColor(ColorType.GREYED)
        }
        for (edge in edges) {
            if (edge.v1 != src && edge.v2 != src) {
                edge.grey()
            } else {
                edge.v1.let { if (it != src) it.clearColor(ColorType.GREYED)}
                edge.v2.let { if (it != src) it.clearColor(ColorType.GREYED) }
                edge.setLineColor(Color.GREEN, Color.RED, src)
                edge.setLabelColor(Color.GREEN, Color.RED, src)
            }
        }
    }

    /**
     * Ungrey everything by setting line and label colors for edges and color for vertices.
     */
    fun ungreyEverything(){
        for(edge in edges){
            edge.ungrey()
        }
        for (vert in vertices){
            vert.clearColor(ColorType.GREYED)
            vert.clearColor(ColorType.PATH)
        }
    }

    /**
     * Given [path] grey stuff no in path and make the path fancy. And add path to [currentPathVertices] and [currentPathConnections]
     */
    fun colorPath(path: List<Any>){
        currentPathVertices.clear()
        for(v in path){
            for(vertex in vertices){
                if(vertex.v == v)
                    currentPathVertices.add(vertex)
            }
        }

        currentPathConnections.clear()
        for((v1,v2) in currentPathVertices.dropLast(1).zip(currentPathVertices.drop(1))){
            for(edge in edges){
                if(edge.v1 == v1 && edge.v2 == v2){
                    currentPathConnections.addLast(edge.v1tov2Connection)
                    break
                }
                else if (edge.v1 == v2 && edge.v2 == v1){
                    currentPathConnections.addLast(edge.v2tov1Connection)
                    break
                }
            }
        }

        greyEverything()
        makePathFancyColors()
    }

    /**
     * Creates a gradient for the current [currentPathVertices] and [currentPathConnections]
     */
    private fun makePathFancyColors() {
        val startColor = Controller.PATH_START
        val endColor = Controller.PATH_END
        val segments: Double = currentPathVertices.size + currentPathConnections.size.toDouble()
        var currColor = startColor
        val connections = LinkedList(currentPathConnections)
        val verts = LinkedList(currentPathVertices)

        while(!verts.isEmpty()){
            val vert = verts.removeFirst()
            if(verts.isEmpty()) vert.setColor(ColorType.PATH, endColor) else vert.setColor(ColorType.PATH, currColor)
            currColor = Color.color(
                (currColor.red + ((endColor.red - startColor.red)/segments)),
                (currColor.green + ((endColor.green - startColor.green)/segments)),
                (currColor.blue + ((endColor.blue - startColor.blue)/segments))
            )
            
            if(!connections.isEmpty()) {
                val connection = connections.removeFirst()
                connection.setLineColor(currColor)
                connection.boldLine()
                currColor = Color.color(
                    (currColor.red + ((endColor.red - startColor.red)/segments)),
                    (currColor.green + ((endColor.green - startColor.green)/segments)),
                    (currColor.blue + ((endColor.blue - startColor.blue)/segments))
                )
            }
        }
    }

    /**
     * Given [clusters] colors the graph so that each cluster is a distinct color
     */
    fun colorClusters(clusters: Collection<Graph<Any>>){
        fun randomColor(): Color = Color.color(Math.random(), Math.random(), Math.random())
        val colors = LinkedList(listOf(
            Color.rgb(148, 0, 211), // Deep Purple
            Color.rgb(102, 205, 170), // Aquamarine
            Color.rgb(218, 165, 32), // Goldenrod
            Color.rgb(0, 191, 255), // Deep Sky Blue
            Color.rgb(218, 112, 214), // Orchid
            Color.rgb(154, 205, 50), // Yellow Green
            Color.rgb(255, 69, 0), // Orange Red
            Color.rgb(139, 69, 19), // Saddle Brown
            Color.rgb(176, 224, 230), // Powder Blue
            Color.rgb(255, 105, 180), // Hot Pink
            Color.rgb(70, 130, 180), // Steel Blue
            Color.rgb(0, 128, 128), // Teal
            Color.rgb(255, 99, 71), // Tomato
            Color.rgb(255, 215, 0), // Gold
            Color.rgb(255, 160, 122) // Light Salmon
        ))
        val sortedClusters = clusters.sortedWith(compareBy({ it.size() }, { cluster -> (cluster.mapVertices { e -> e.toString() }).minByOrNull { it.length}  }))
        for(cluster in sortedClusters){
            val color = if(colors.isNotEmpty()) colors.removeFirst() else randomColor()
            for(vertex in cluster){
                stringToVMap[vertex.toString()]?.setColor(ColorType.CLUSTERED, color)
            }
        }
    }

    fun clearClusterColoring(){
        for(vertex in vertices){
            vertex.clearColor(ColorType.CLUSTERED)
        }
    }
}