package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GraphApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(GraphApp::class.java.getResource("graph.fxml"))
        val scene = Scene(fxmlLoader.load(), 1280.0, 820.0)
        val controller: Controller = fxmlLoader.getController()!!
        scene.stylesheets.addAll(GraphApp::class.java.getResource("style.css")!!.toExternalForm())

        stage.title = "Graph"
        stage.scene = scene

        /* Customize your graph */
        val verts = ('A'..'J').toList()

        val graph = AMGraph(verts)
        if(Random.nextBoolean()) graph['A','B'] = Random.nextInt(1,9) else graph['B','A'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['A','C'] = Random.nextInt(1,9) else graph['C','A'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['A','D'] = Random.nextInt(1,9) else graph['D','A'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['D','G'] = Random.nextInt(1,9) else graph['G','D'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['G','I'] = Random.nextInt(1,9) else graph['I','G'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['H','E'] = Random.nextInt(1,9) else graph['E','H'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['H','J'] = Random.nextInt(1,9) else graph['J','H'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['C','J'] = Random.nextInt(1,9) else graph['J','C'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['B','I'] = Random.nextInt(1,9) else graph['I','B'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['I','J'] = Random.nextInt(1,9) else graph['J','I'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['C','F'] = Random.nextInt(1,9) else graph['F','C'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['F','G'] = Random.nextInt(1,9) else graph['G','F'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['F','E'] = Random.nextInt(1,9) else graph['E','F'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['B','E'] = Random.nextInt(1,9) else graph['E','B'] = Random.nextInt(1,9)
        if(Random.nextBoolean()) graph['D','H'] = Random.nextInt(1,9) else graph['H','D'] = Random.nextInt(1,9)

        controller.initializeGraph(graph, stage)

        val r1 = 0.4
        val r2 = 0.2
        for (v in controller.graphicComponents.vertices) {
            when (v.v) {
                'A' -> v.pos = Position(cos(((0*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5, -1 * sin(((0*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5)
                'B' -> v.pos = Position(cos(((1*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5, -1 * sin(((1*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5)
                'I' -> v.pos = Position(cos(((2*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5, -1 * sin(((2*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5)
                'J' -> v.pos = Position(cos(((3*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5, -1 * sin(((3*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5)
                'C' -> v.pos = Position(cos(((4*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5, -1 * sin(((4*72.0) + 90.0) * (PI / 180.0)) * r1 + 0.5)

                'D' -> v.pos = Position(cos(((0*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5, -1 * sin(((0*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5)
                'E' -> v.pos = Position(cos(((1*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5, -1 * sin(((1*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5)
                'G' -> v.pos = Position(cos(((2*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5, -1 * sin(((2*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5)
                'H' -> v.pos = Position(cos(((3*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5, -1 * sin(((3*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5)
                'F' -> v.pos = Position(cos(((4*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5, -1 * sin(((4*72.0) + 90.0) * (PI / 180.0)) * r2 + 0.5)
            }
        }

        stage.show()
    }
}

fun main() {
    val graphApp = GraphApp()
    Application.launch(graphApp::class.java)
}