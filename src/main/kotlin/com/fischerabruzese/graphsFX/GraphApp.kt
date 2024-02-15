package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.*
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 1280.0, 820.0)
        val controller : Controller<Int> = fxmlLoader.getController()!!
        scene.stylesheets.addAll(GraphApp::class.java.getResource("style.css").toExternalForm())

        stage.title = "Graph"
        stage.scene = scene


        /* Customize your graph */

        val verts = Array(4){i -> i}

        val graph = AMGraph.graphOf(*verts)
        graph.randomizeSmart(3, 10)
        controller.setGraph(graph)
        println(graph.multipleMinCut(10))


        /* Runtime testing with slightly different algorithms */
/*
        println("--Time Trials--")

        val start1 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.depthFirstSearch2(from, to)
            }
        }
        println("PTF DFS: ${(System.nanoTime() - start1).div(10000.0).roundToInt().div(10.0)} ms")

        val start2 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.depthFirstSearch(from, to)
            }
        }
        println("Sky DFS: ${(System.nanoTime() - start2).div(10000.0).roundToInt().div(10.0)} ms")

        val start3 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.breadthFirstSearch2(from, to)
            }
        }
        println("PTF BFS: ${(System.nanoTime() - start3).div(10000.0).roundToInt().div(10.0)} ms")

        val start4 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.breadthFirstSearch(from, to)
            }
        }
        println("Sky BFS: ${(System.nanoTime() - start4).div(10000.0).roundToInt().div(10.0)} ms")

        for(from in verts){ //for some reason, the first run of dijkstra is always slower than the rest, so we run it before the timer
            for (to in verts){
                graph.dijkstra(from, to)
                graph.dijkstraFibHeap(from, to)
            }
        }

        val start5 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.dijkstra(from, to)
            }
        }
        println("PTF DSA: ${(System.nanoTime() - start5).div(10000.0).roundToInt().div(10.0)} ms")

        val start6 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.dijkstraFibHeap(from, to)
            }
        }
        println("Sky DSA: ${(System.nanoTime() - start6).div(10000.0).roundToInt().div(10.0)} ms")

        println("--Paths from 0 to 1--")

        println("PTF DFS: ${graph.depthFirstSearch2(0, 1)}")
        println("Sky DFS: ${graph.depthFirstSearch(0, 1)}")
        println("PTF BFS: ${graph.breadthFirstSearch2(0, 1)}")
        println("Sky BFS: ${graph.breadthFirstSearch(0, 1)}")
        println("PTF DSA: ${graph.path(0, 1, true)} | dist -> ${graph.distance(0, 1)}")
        graph.clearDijkstraCache()
        println("Sky DSA: ${graph.path(0, 1, false)} | dist -> ${graph.distance(0, 1)}")


        /* Launch the application */
*/
        controller.draw()
        stage.show()
    }
}

fun main() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
}