package com.fischerabruzese.graph

class Graph<E:Any>(vararg outboundConnections : Pair<E,Array<Pair<E,Int>>>?) {

//    constructor(vararg outboundConnectionsUnweighted : Pair<E,Array<E>>) : this(*(
//                Array<Pair<E,Array<Pair<E,Int>>>?>(outboundConnectionsUnweighted.size) {
//                    i -> Pair(
//                    outboundConnectionsUnweighted[i].first,
//                    Array<Pair<E,Int>>(
//                        outboundConnectionsUnweighted[i].second.size) {
//                            j -> Pair(outboundConnectionsUnweighted[i].second[j],1)
//                        }
//                    )
//                }
//            ))

    constructor(vararg vertices: E) : this(*(
                Array<Pair<E,Array<Pair<E,Int>>>?>(vertices.size) { i -> Pair(
                    vertices[i],
                    emptyArray()
                )}
            ))

    constructor() : this(null)

    data class Vertex<E>(val item: E, val index: Int)

    private var vertices : ArrayList<Vertex<E>> = ArrayList()
    private var edgeMatrix : Array<IntArray>

    init {
        val addedVertices = HashMap<E, Int>()
        for(connections in outboundConnections){
            if(connections == null) continue
            if(addedVertices.put(connections.first, vertices.size) == null){
                vertices.add(Vertex(connections.first, vertices.size))
            }
            for(outboundEdge in connections.second){
                if(addedVertices.put(outboundEdge.first, vertices.size) == null){
                    vertices.add(Vertex(outboundEdge.first, vertices.size))
                }
            }

        }
        edgeMatrix = Array(vertices.size) {IntArray(vertices.size) {-1} }

        for(connections in outboundConnections) {
            if(connections == null) continue
            for(outboundEdge in connections.second){
                edgeMatrix[addedVertices[connections.first]!!][addedVertices[outboundEdge.first]!!] = outboundEdge.second
            }
        }
    }

    override fun toString(): String {
        val string = StringBuilder()
        for(destinations in edgeMatrix){
            string.append("[")
            for (weight in destinations){
                string.append("$weight][")
            }
            string.deleteRange(string.length - 2, string.length)
            string.append("]\n")
        }
        return string.toString()
    }
    fun getVertices() : ArrayList<Vertex<E>> {
        return vertices
    }


}