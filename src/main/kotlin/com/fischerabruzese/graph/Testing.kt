package com.fischerabruzese.graph

fun main() {
    val one = ArrayList<Pair<Int, String>>()
    val two = ArrayList<Pair<Int, String>>()
    for(j in 1 until 100){
        val verts = Array(250){i -> i}
        val graph = AMGraph(*verts)
        graph.randomize({ Math.random() > 0.5 }, 9)
        val start1 = System.nanoTime()
        for(i in 0 until 1000) graph.dijkstraFibHeap(237)
        one.add((j to "${System.nanoTime() - start1}"))
        val start2 = System.nanoTime()
        for(i in 0 until 1000) graph.dijkstra(237)
        two.add((j to "${System.nanoTime() - start2}"))
    }
    println(one.joinToString(","))
    println(two.joinToString(","))

//    val graph = AMGraph('a' ,'b','c','d')
////
//    for(from in graph.edgeMatrix){
//        for(to in from){
//            print("[$to]")
//        }
//        println()
//    }
//    println()
//    println("Removing 'c'")
//    println()
//    graph.remove('c')
//    for(from in graph.edgeMatrix){
//        for(to in from){
//            print("[$to]")
//        }
//        println()
//    }
//    //dijkstra testing
//    for(vert in graph.getVertices()){
//        println("Path: " + graph.path(vert, 'b'))
//        println("Weight: " + graph.distance(vert, 'b'))
//    }
////    graph.randomize(0.5, 10)
////    println("Randomize")
////    println(graph)
////
////    graph[4, 5] = 1
////    println("[4, 5] = 1")
////    println(graph)

}