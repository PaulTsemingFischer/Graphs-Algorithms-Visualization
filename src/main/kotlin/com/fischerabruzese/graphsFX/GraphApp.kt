package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.*
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 600.0, 400.0)
        val controller : Controller<Char> = fxmlLoader.getController()!!

        stage.title = "Graph"
        stage.scene = scene

        val graph = AMGraph('a' to listOf('c' to 2, 'd' to 7), 'b' to listOf('c' to 7, 'd' to 1), 'c' to listOf('a' to 2, 'b' to 4, 'd' to 3), 'd' to listOf('a' to 7, 'b' to 1, 'c' to 3))
        controller.graphInit(graph)

        println("------------Dikstra------------")
        println("From a:" + graph.getAllDijkstra('a').joinToString("\n"))
        println("From b:" + graph.getAllDijkstra('b').joinToString("\n"))
        println("From c:" + graph.getAllDijkstra('c').joinToString("\n"))
        println("From d:" + graph.getAllDijkstra('d').joinToString("\n"))


        println("------------Dikstras------------")
        println("From a:" + graph.getAllDijkstras('a').joinToString("\n"))
        println("From b:" + graph.getAllDijkstras('b').joinToString("\n"))
        println("From c:" + graph.getAllDijkstras('c').joinToString("\n"))
        println("From d:" + graph.getAllDijkstras('d').joinToString("\n"))

        controller.draw()
        stage.show()
    }
//    fun<E : Any> draw(graph : Graph<E>){
//        if (::controller.isInitialized) {
//            controller.draw(graph)
//        } else {
//            throw Exception("Controller not initialized")
//        }
//    }
}

fun main() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
//    graphApp.draw(AMGraph<Int>(5 to ((0..4).map { it to 3} )))
}