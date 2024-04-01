package com.fischerabruzese.graphsFX

import com.fischerabruzese.graph.AMGraph
import java.util.*

fun createGraph(text: String, numLetters: Int = 10): AMGraph<Char> {
    //Creating frequency matrix
    val frequencyMatrix = Array(numLetters) { IntArray(numLetters) { 0 } }
    var currChar: Int? = null

    for (char in text) {
        val nextChar = char.lowercaseChar().code - 'a'.code
        if (nextChar in 0 until numLetters) {
            currChar?.let {
                frequencyMatrix[it][nextChar]++
            }
            currChar = nextChar
        }
    }

    //Creating edge start counter
    val edgeStartCounter =
        IntArray(numLetters) { frequencyMatrix[it].sum() }//# edges starting at each vertex(= occurrences except for  first and last char)
    println(edgeStartCounter.joinToString(",", "[", "]"))

    //Creating graph
    val graph = AMGraph(List(numLetters) { Char('a'.code + it) })
    for (i in frequencyMatrix.indices) {
        for (j in frequencyMatrix[i].indices) {
            val edgeWeight = frequencyMatrix[i][j]
            val averageEdgeWeight = edgeStartCounter[i] / numLetters.toDouble()

            if (edgeWeight / averageEdgeWeight > 1.0) {
                graph[Char('a'.code + i), Char('a'.code + j)] = (10 * edgeWeight / averageEdgeWeight).toInt()
            }
        }
    }
    return graph
}

fun getText(): String {
    val text = StringBuilder()
    val scanner = Scanner(System.`in`)
    var nextLine = ""
    while (nextLine != "EXIT") {
        text.append(nextLine)
        nextLine = scanner.nextLine()
    }
    return text.toString()
}