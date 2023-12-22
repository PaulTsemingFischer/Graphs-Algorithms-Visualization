package com.fischerabruzese.graphsFX

import javafx.beans.binding.DoubleBinding
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import com.fischerabruzese.graph.*
import javafx.beans.binding.Bindings
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.input.MouseEvent

class Controller<E: Any> {
    @FXML
    private lateinit var pane: Pane

    private lateinit var paneWidth : ReadOnlyDoubleProperty
    private lateinit var paneHeight : ReadOnlyDoubleProperty
    private val CIRCLE_RADIUS = 20.0

    private lateinit var graph : Graph<E>

    @FXML
    fun initialize() {
        println("Initialize")
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    fun graphInit(graph : Graph<E>){
        this.graph = graph
    }

    fun draw() {
        println("Draw")
        val vertices = graph.getVerticies().toList()
        val verticesElements = Array(vertices.size){ index -> Vertex(vertices[index].toString(), Math.random(), Math.random())}
        val edgeElements = ArrayList<Edge>()

        for(i in verticesElements.indices){
            for(j in i..verticesElements.indices.last){
                val weight = graph[vertices[i], vertices[j]] ?: continue
                edgeElements.add(Edge(verticesElements[i], verticesElements[j], weight))
            }
        }

        pane.children.addAll(edgeElements)
        pane.children.addAll(verticesElements)

    }

    @FXML
    private fun randomizePressed() {
        pane.children.clear()
        draw()
    }

    @FXML
    private fun editEdgePressed() {
        println("Edit edge")
    }


    //Precondition: x and y are between 0 and 1
    inner class Vertex(name: String, x : Double, y : Double) : StackPane() {
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

        private var xpos : DoubleProperty = SimpleDoubleProperty(x)
        private var ypos : DoubleProperty = SimpleDoubleProperty(y)

        private var xBinding : DoubleBinding = paneWidth.multiply(xpos).multiply(usablePercentPaneWidth)
        private var yBinding : DoubleBinding = paneHeight.multiply(ypos).multiply(usablePercentPaneHeight)

        //Dragging
        private var xDelta : Double = 0.0
        private var yDelta : Double = 0.0

        //Location Bindings
        private val usablePercentPaneWidth: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * CIRCLE_RADIUS / paneWidth.get() },
            paneWidth
        )
        private val usablePercentPaneHeight: DoubleBinding = Bindings.createDoubleBinding(
            { 1.0 - 2 * CIRCLE_RADIUS / paneHeight.get() },
            paneHeight
        )

        private var xpos : DoubleProperty = SimpleDoubleProperty(x)
        private var ypos : DoubleProperty = SimpleDoubleProperty(y)

        private var xBinding : DoubleBinding = paneWidth.multiply(xpos).multiply(usablePercentPaneWidth)
        private var yBinding : DoubleBinding = paneHeight.multiply(ypos).multiply(usablePercentPaneHeight)

        //Dragging
        private var xDelta : Double = 0.0
        private var yDelta : Double = 0.0

        init{
            //Circle
            circle.translateXProperty().bind(xBinding)
            circle.translateYProperty().bind(yBinding)

            //Label
            label.translateXProperty().bind(circle.translateXProperty())
            label.translateYProperty().bind(circle.translateYProperty())

            label.textFill = Color.WHITE
            label.pickOnBoundsProperty().set(false)

            //Dragging
            setOnMousePressed { dragStart(it) }
            setOnMouseDragged { drag(it) }

            //Hitbox
            hitbox.translateXProperty().bind(circle.translateXProperty())
            hitbox.translateYProperty().bind(circle.translateYProperty())
            hitbox.setOnMousePressed { dragStart(it) }
            hitbox.setOnMouseDragged { drag(it) }

            children.addAll(circle, label, hitbox)
        }

        private fun dragStart(event : MouseEvent) {
            xDelta = event.sceneX / pane.width - xpos.get()
            yDelta = event.sceneY / pane.height - ypos.get()

            println("Drag start - $xDelta, $yDelta")
        }
        private fun drag(event : MouseEvent) {
            xpos.set(event.sceneX / pane.width - xDelta)
            ypos.set(event.sceneY / pane.height - yDelta)
            circle.fill = Color.RED
        }

        fun getCenterX() : DoubleBinding {
            return circle.translateXProperty().add(circle.radiusProperty())
        }
        fun getCenterY() : DoubleBinding {
            return circle.translateYProperty().add(circle.radiusProperty())
        }
    }
    inner class Edge(from : Vertex, to : Vertex, weight : Int) : Pane() {
        private val line = Line()
        private val label = Label(weight.toString())

        init{
            //Binding endpoints
            line.startXProperty().bind(from.getCenterX())
            line.startYProperty().bind(from.getCenterY())
            line.endXProperty().bind(to.getCenterX())
            line.endYProperty().bind(to.getCenterY())

            //Binding label position to the midpoint of the line
            label.translateXProperty().bind((line.startXProperty().add(line.endXProperty())).divide(2))
            label.translateYProperty().bind((line.startYProperty().add(line.endYProperty())).divide(2))
            label.textFill = Color.BLACK

            children.addAll(line, label)
        }
    }
}