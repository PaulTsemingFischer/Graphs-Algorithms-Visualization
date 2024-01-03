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

       // val verts = Array(10){i -> i}

//        val graph = AMGraph(*verts)
        val graph = AMGraph('a' ,'b','c','d', 'e', 'f', 'g')
        graph.randomize(0.5, 9)
        controller.graphInit(graph)
        for(edge in graph.edgeMatrix){
            for(weight in edge){
                print("[$weight]")
            }
            println()
        }

        for(vert in graph.getVertices()){
            println("Path $vert to 'b': " + graph.getDijkstraPath(vert, 'b'))
            println("Weight $vert to 'b': " + graph.getDijkstraWeight(vert, 'b'))
        }
//        println("------------Dijkstra------------")
//        println("From a:" + graph.getAllDijkstra('a').joinToString("\n"))
//        println("From b:" + graph.getAllDijkstra('b').joinToString("\n"))
//        println("From c:" + graph.getAllDijkstra('c').joinToString("\n"))
//        println("From d:" + graph.getAllDijkstra('d').joinToString("\n"))

//        val start1 = System.nanoTime()
//        for (i in verts.indices){
//            graph.getAllDijkstra(i)
//        }
//        println("Pau: Time Elapsed: ${System.nanoTime() - start1}")

//        val start2 = System.nanoTime()
//        for (i in 0..100){
//o            graph.getAllDijkstra2(i)
//        }
        //println("Sky: Time Elapsed: ${System.nanoTime() - start2}")


//        println("------------Dijkstra2------------")
//        println("From a:" + graph.getAllDijkstra2('a').joinToString("\n"))
//        println("From b:" + graph.getAllDijkstra2('b').joinToString("\n"))
//        println("From c:" + graph.getAllDijkstra2('c').joinToString("\n"))
//        println("From d:" + graph.getAllDijkstra2('d').joinToString("\n"))


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