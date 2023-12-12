package com.fischerabruzese.graph
data class Edge<E:Any>(val from: E, val to: E, val weight: Int)


class Graph<E:Any>(edges: Collection<Edge<E>>) {
    //constructor(vararg items: E) : this(items.toList())
    constructor() : this(emptyList())

    data class Vertex<E>(val item: E, val index: Int)

    private var vertices : ArrayList<Vertex<E>>
    init {
        vertices = ArrayList<Vertex<E>>().apply {
            for ((i, edge) in edges.withIndex())
                add(Vertex(edge, i))
        }
    }
}