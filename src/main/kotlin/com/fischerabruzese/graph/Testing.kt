package com.fischerabruzese.graph

fun main() {
    val graph = AMGraph<Int>(5 to ((0..4).map { it to 3} ))

    graph.randomize(0.5, 10)
    println("Randomize")
    println(graph)

    graph[4, 5] = 1
    println("[4, 5] = 1")
    println(graph)

}