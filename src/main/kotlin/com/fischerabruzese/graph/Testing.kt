package com.fischerabruzese.graph

fun main() {
    val graph = AMGraph('a' ,'b','c','d')
//
    for(from in graph.edgeMatrix){
        for(to in from){
            print("[$to]")
        }
        println()
    }
    println()
    println("Removing 'c'")
    println()
    graph.remove('c')
    for(from in graph.edgeMatrix){
        for(to in from){
            print("[$to]")
        }
        println()
    }
    //dijkstra testing
    for(vert in graph.getVertices()){
        println("Path: " + graph.getDijkstraPath(vert, 'b'))
        println("Weight: " + graph.getDijkstraWeight(vert, 'b'))
    }
//    graph.randomize(0.5, 10)
//    println("Randomize")
//    println(graph)
//
//    graph[4, 5] = 1
//    println("[4, 5] = 1")
//    println(graph)

}