package com.fischerabruzese.graphsFX

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 600.0, 400.0)
        stage.title = "Graph"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(GraphApp::class.java)
}