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

        val verts = Array(100){i -> i}

        val graph = AMGraph.graphOf(*verts)
        graph.randomize(0.03, 9)
        controller.graphInit(graph)

        val start1 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.depthFirstSearch2(from, to)
            }
        }
        println("PTF DFS: ${System.nanoTime() - start1}")

        val start2 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.depthFirstSearch(from, to)
            }
        }
        println("Sky DFS: ${System.nanoTime() - start2}")

        val start3 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.breadthFirstSearch2(from, to)
            }
        }
        println("PTF BFS: ${System.nanoTime() - start3}")

        val start4 = System.nanoTime()
        for(from in verts){
            for (to in verts){
                graph.breadthFirstSearch(from, to)
            }
        }
        println("Sky BFS: ${System.nanoTime() - start4}")

        println("PTF DFS: ${graph.depthFirstSearch2(0, 1)}")
        println("Sky DFS: ${graph.depthFirstSearch(0, 1)}")
        println("PTF BFS: ${graph.breadthFirstSearch2(0, 1)}")
        println("Sky BFS: ${graph.breadthFirstSearch(0, 1)}")

        controller.draw()
        stage.show()
    }
}

fun main() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
}