package com.fischerabruzese.graph

fun main() {
    val graph = Graph<Int>(5 to ((0..4).map { it to 3} ))
    graph.randomize(0.5, 10)
    println(graph)

}