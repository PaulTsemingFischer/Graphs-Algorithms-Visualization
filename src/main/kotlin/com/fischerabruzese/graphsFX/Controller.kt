package com.fischerabruzese.graphsFX

import javafx.beans.binding.DoubleBinding
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line

class Controller {
    @FXML
    private lateinit var pane: Pane

    private lateinit var paneWidth : ReadOnlyDoubleProperty
    private lateinit var paneHeight : ReadOnlyDoubleProperty
    private val CIRCLE_RADIUS = 20.0


    @FXML
    fun initialize(){
        paneWidth = pane.widthProperty()
        paneHeight = pane.heightProperty()
    }

    fun<E : Any> draw(graph : Graph<E>) {
        val vertices = graph.getVerticies().toList()
        val verticesElements = Array(vertices.size){ index -> Vertex(vertices[index].toString(), Math.random(), Math.random())}
        val edgeElements = ArrayList<Edge>()

        for(i in verticesElements.indices){
            for(j in i..verticesElements.indices.last){
                val weight = graph[vertices[i], vertices[j]] ?: continue
                edgeElements.add(Edge(verticesElements[i], verticesElements[j], weight))
            }
        }

        pane.children.addAll(verticesElements)
        pane.children.addAll(edgeElements)
    }

    @FXML
    private fun randomizePressed() {
        println("Randomize")
    }

    @FXML
    private fun editEdgePressed() {
        println("Edit edge")
    }



    inner class Vertex(name: String, xpos : Double, ypos : Double) : StackPane() {
        private val x = paneWidth.multiply(xpos).subtract(CIRCLE_RADIUS)
        private val y = paneHeight.multiply(ypos).subtract(CIRCLE_RADIUS)
        private val circle = Circle(CIRCLE_RADIUS, Color.BLUE)
        private val label = Label(name)

        init{
            //Circle
            circle.translateXProperty().bind(x)
            circle.translateYProperty().bind(y)

            //Label
            label.translateXProperty().bind(circle.translateXProperty())
            label.translateYProperty().bind(circle.translateYProperty())

            label.textFill = Color.WHITE

            children.addAll(circle, label)
        }

        fun move(x : Double, y : Double){
            circle.translateX = x
            circle.translateY = y
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