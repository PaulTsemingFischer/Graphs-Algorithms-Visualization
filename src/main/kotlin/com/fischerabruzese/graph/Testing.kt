package com.fischerabruzese.graph

import kotlin.random.Random

fun main() {
    for(j in 10 until 300){
        val verts2 = Array(j){i -> i}
        val graph2 = AMGraph(*verts2)
        graph2.randomize(Random(249), 9)
        val start2 = System.nanoTime()
        for (i in verts2.indices){
            graph2.path2(i,0)
        }
        print(j to "${System.nanoTime() - start2}")
        print(",")
    }
    println()

    for(j in 10 until 300){
        val verts = Array(j){i -> i}
        val graph = AMGraph(*verts)
        graph.randomize(Random(249), 9)
        val start1 = System.nanoTime()
        for (i in verts.indices){
            graph.path(i,0)
        }
        print(j to "${System.nanoTime() - start1}")
        print(",")
    }




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