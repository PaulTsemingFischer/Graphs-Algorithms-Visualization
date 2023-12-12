package com.fischerabruzese.graph

import javafx.scene.transform.MatrixType

data class Edge<E:Any>(val from: E, val to: E?, val weight: Int)


class Graph<E:Any>(edges: Collection<Edge<E>>) {
    constructor(vararg edge: Edge<E>) : this(edge.toList())
    constructor() : this(emptyList())

    data class Vertex<E>(val item: E, val index: Int)

    private var vertices : ArrayList<Vertex<E>> = ArrayList()
    private var edgeMatrix : Array<IntArray>

    init {
        val addedVertices = HashMap<E, Int>()
        for(edge in edges){
            if(addedVertices.put(edge.from, addedVertices.size+1) == null){
                vertices.add(Vertex(edge.from, addedVertices.size))
            }
        }
        edgeMatrix = Array(vertices.size) {IntArray(vertices.size)}
        for(edge in edges){
            if(edge.to != null){
                edgeMatrix[addedVertices[edge.to]!!][addedVertices[edge.from]!!] = edge.weight
                //if symmetric
                edgeMatrix[addedVertices[edge.from]!!][addedVertices[edge.to]!!] = edge.weight
            }
        }

    }
}