package com.fischerabruzese.graph

fun main() {
    val graph = Graph<Char>(
        Pair('A', arrayOf(Pair('B', 1)))
    )
    println(graph)
    println("Verts: ${graph.getVertices()}")
}