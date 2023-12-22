package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.*
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import java.lang.Exception

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 600.0, 400.0)
        val controller : Controller<Int> = fxmlLoader.getController()!!

        stage.title = "Graph"
        stage.scene = scene

        val graph = AMGraph(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        graph.randomize(0.3, 9)
        controller.graphInit(graph)

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