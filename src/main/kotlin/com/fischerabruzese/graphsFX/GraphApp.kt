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
        val scene = Scene(fxmlLoader.load(), 1280.0, 820.0)
        val controller : Controller<Int> = fxmlLoader.getController()!!
        scene.stylesheets.addAll(GraphApp::class.java.getResource("style.css").toExternalForm())

        stage.title = "Graph"
        stage.scene = scene

        /* Customize your graph */
        val verts = (0 until 40).toList()

        //val graph = createGraph(getText())
        val graph = AMGraph.fromCollection(verts)
        graph.randomizeWithCluster(3, 9, .39, 0.004)


//        val graph = AMGraph.graphOf<Int>(
//            listOf(
//                1 to listOf(2 to 1, 3 to 1, 5 to 1),
//                6 to listOf(7 to 1, 9 to 1, 10 to 1),
//                1 to listOf(6 to 1, 7 to 1),
//                2 to listOf(8 to 1, 10 to 1, 4 to 1),
//                3 to listOf(9 to 1),
//                4 to listOf(10 to 1, 2 to 1),
//                5 to listOf(6 to 1, 7 to 1),
//                7 to listOf(5 to 1),
//                8 to listOf(10 to 1)
//            )
//        )
//        val graph = AMGraph.graphOf<Int>(
//            listOf(
//                1 to listOf(2 to 1, 3 to 1, 5 to 1),
//                6 to listOf(7 to 1, 9 to 1, 10 to 1),
//                11 to listOf(12 to 1, 13 to 1, 15 to 1),
//                16 to listOf(17 to 1, 18 to 1, 20 to 1),
//                2 to listOf(8 to 1, 10 to 1, 4 to 1),
//                3 to listOf(9 to 1),
//                4 to listOf(10 to 1, 2 to 1),
//                5 to listOf(6 to 1, 7 to 1),
//                7 to listOf(5 to 1),
//                8 to listOf(10 to 1),
//                12 to listOf(14 to 1, 16 to 1, 18 to 1),
//                13 to listOf(17 to 1, 19 to 1, 20 to 1),
//                14 to listOf(16 to 1, 18 to 1),
//                15 to listOf(17 to 1, 19 to 1, 20 to 1),
//                16 to listOf(18 to 1, 20 to 1),
//                17 to listOf(19 to 1, 20 to 1),
//                18 to listOf(20 to 1),
//                19 to listOf(20 to 1)
//            )
//        )


//        graph.randomize(2, 9, true, Random(69))
        controller.initializeGraph(graph)
        //controller.moveClusters(graph.getClusters(0.501))



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
        //controller.unfixSimulationLag()
    }
}

fun main() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
}