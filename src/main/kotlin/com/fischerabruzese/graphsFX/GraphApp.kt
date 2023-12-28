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
        val controller : Controller<Int> = fxmlLoader.getController()!!

        stage.title = "Graph"
        stage.scene = scene

        val verts = Array(50){i -> i}
        val graph = AMGraph(*verts)
        graph.randomize(1.0, 9)
        controller.graphInit(graph)

//        println("------------Dijkstra------------")
//        println("From a:" + graph.getAllDijkstra('a').joinToString("\n"))
//        println("From b:" + graph.getAllDijkstra('b').joinToString("\n"))
//        println("From c:" + graph.getAllDijkstra('c').joinToString("\n"))
//        println("From d:" + graph.getAllDijkstra('d').joinToString("\n"))

        val start1 = System.nanoTime()
        for (i in verts.indices){
            graph.getAllDijkstra(i)
        }
        println("Pau: Time Elapsed: ${System.nanoTime() - start1}")

        val start2 = System.nanoTime()
        for (i in verts.indices){
            graph.getAllDijkstra2(i)
        }
        println("Sky: Time Elapsed: ${System.nanoTime() - start2}")


//        println("------------Dijkstra2------------")
//        println("From a:" + graph.getAllDijkstra2('a').joinToString("\n"))
//        println("From b:" + graph.getAllDijkstra2('b').joinToString("\n"))
//        println("From c:" + graph.getAllDijkstra2('c').joinToString("\n"))
//        println("From d:" + graph.getAllDijkstra2('d').joinToString("\n"))


//        controller.draw()
//        stage.show()
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