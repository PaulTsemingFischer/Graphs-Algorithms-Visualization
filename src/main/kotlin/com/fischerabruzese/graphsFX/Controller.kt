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

    @FXML
    fun initialize() {
        val vertex0 = Vertex("0", 0.0, 0.0)
        val vertex1 = Vertex("1", 100.0, 100.0)
        val vertex2 = Vertex("2", 200.0, 300.0)
        val vertex3 = Vertex("3", 50.0, 200.0)
        val vertex4 = Vertex("4", 355.0, 177.0)

        val edge0 = Edge(vertex1, vertex2, 10)
        val edge1 = Edge(vertex2, vertex3, 20)
        val edge2 = Edge(vertex3, vertex4, 30)
        val edge3 = Edge(vertex4, vertex0, 40)
        val edge4 = Edge(vertex0, vertex1, 50)
        val edge5 = Edge(vertex1, vertex3, 60)
        val edge6 = Edge(vertex2, vertex4, 70)
        val edge7 = Edge(vertex3, vertex0, 80)
        val edge8 = Edge(vertex4, vertex1, 90)

        pane.children.addAll(edge0, edge1, edge2, edge3, edge4, edge5, edge6, edge7, edge8)
        pane.children.addAll(vertex0, vertex1, vertex2, vertex3, vertex4)

        pane.requestLayout()
        println("Pane size: ${pane.width}x${pane.height}")
    }

    @FXML
    private fun randomizePressed() {
        println("Randomize")
    }

    @FXML
    private fun editEdgePressed() {
        println("Edit edge")
    }



    class Vertex(name: String, x : Double, y : Double) : StackPane() {
        val circle = Circle(20.0, Color.BLUE)
        val label = Label(name)

        init{
            //Circle
            circle.translateX = x
            circle.translateY = y

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
    class Edge(from : Vertex, to : Vertex, weight : Int) : Pane() {
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