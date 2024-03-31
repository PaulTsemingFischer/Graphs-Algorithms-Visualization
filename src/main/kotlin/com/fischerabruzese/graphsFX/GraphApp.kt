package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.random.Random

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 1280.0, 820.0)
        val controller : Controller = fxmlLoader.getController()!!
        scene.stylesheets.addAll(GraphApp::class.java.getResource("style.css")!!.toExternalForm())

        stage.title = "Graph"
        stage.scene = scene

        /* Customize your graph */
        val verts = ('A'..'J').toList()

        val graph = AMGraph(verts)
        graph['A','B'] = Random.nextInt(0,9)
        graph['A','C'] = Random.nextInt(0,9)
        graph['A','D'] = Random.nextInt(0,9)
        graph['D','E'] = Random.nextInt(0,9)
        graph['D','F'] = Random.nextInt(0,9)
        graph['B','H'] = Random.nextInt(0,9)
        graph['C','J'] = Random.nextInt(0,9)
        graph['G','I'] = Random.nextInt(0,9)
        graph['I','H'] = Random.nextInt(0,9)
        graph['H','E'] = Random.nextInt(0,9)
        graph['H','J'] = Random.nextInt(0,9)
        graph['F','D'] = Random.nextInt(0,9)
        graph['F','J'] = Random.nextInt(0,9)
        graph['H','E'] = Random.nextInt(0,9)
        graph['C','G'] = Random.nextInt(0,9)
        graph['B','I'] = Random.nextInt(0,9)

        controller.initializeGraph(graph, stage)
        stage.show()
    }
}

fun main() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
}