package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.*
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.random.Random

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 600.0, 400.0)
        val controller : Controller<Int> = fxmlLoader.getController()!!

        stage.title = "Graph"
        stage.scene = scene

        val verts = Array(100){i -> i}
//
        val graph = AMGraph.graphOf(*verts)
//        val test = AMGraph<Char>('a' to listOf('b' to 1,'c' to 5), 'd' to listOf('d' to 3), 'f' to listOf('c' to 3), 'd' to listOf('f' to 3))
        graph.randomize({Random.nextBoolean()}, 9)
        controller.graphInit(graph)

        val start1 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.depthFirstSearchv2(from, to)
            }
        }
        println("PTF: Time Elapsed: ${System.nanoTime() - start1}")

        val start2 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.depthFirstSearch(from, to)
            }
        }
        println("Sky: Time Elapsed: ${System.nanoTime() - start2}")


//        for(edge in graph.edgeMatrix){
//            for(weight in edge){
//                print("[$weight]")
//            }
//            println()
//        }
//
//        for(vert in graph.getVertices()){
//            println("Path $vert to 'b': " + graph.path(vert, 'b'))
//            println("Weight $vert to 'b': " + graph.distance(vert, 'b'))
//        }
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