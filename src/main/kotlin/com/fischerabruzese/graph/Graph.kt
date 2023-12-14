package com.fischerabruzese.graph

/* Construct a graph with outboundConnections containing:
        outboundConnection(Source, ArrayOf(Destination, Weight))
 */
class Graph<E:Any>(vararg outboundConnections : Pair<E,Iterable<Pair<E,Int>>>?) {

//    constructor(vararg outboundConnectionsUnweighted: Pair<E, Iterable<E>>) : this(
//        *outboundConnectionsUnweighted.map { (source, destinations) ->
//            source to destinations.map { it to 1 }
//        }.toTypedArray()
//    )

    constructor(vararg vertices: E) : this(
        *vertices.map { it to emptyList<Pair<E, Int>>()}.toTypedArray()
    )

    internal constructor() : this(
        null
    )

    //do we need to store the index? is the index the location in the array?
    data class Vertex<E>(val item: E,
    )

    private var vertices : ArrayList<Vertex<E>> = ArrayList()
    private var edgeMatrix : Array<IntArray>

    init {
        val addedVertices = HashMap<E, Int>()
        for(connections in outboundConnections){
            if(connections == null) continue
            if(indexLookup.put(connections.first, vertices.size) == null){
                vertices.add(Vertex(connections.first))
            }
            for(outboundEdge in connections.second){
                if(indexLookup.put(outboundEdge.first, vertices.size) == null){
                    vertices.add(Vertex(outboundEdge.first))
                }
            }
        }
        edgeMatrix = Array(vertices.size) {IntArray(vertices.size) {-1} }

        for(connections in outboundConnections) {
            if(connections == null) continue
            for(outboundEdge in connections.second){
                edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] = outboundEdge.second
            }
        }
    }

    //syntax -> this[from,to]
    operator fun get(from : E, to : E ) : Int? {
        return indexLookup[from]?.let { fromi -> indexLookup[to]?.let { toi -> get(fromi, toi) } }
    }

    private fun get(from : Int, to : Int) : Int? {
        return if (edgeMatrix[from][to] == -1) null
        else edgeMatrix[from][to]
    }

    //returns previous node connection
    //syntax -> this[from,to] = value
    operator fun set(from : Int, to : Int, value : Int) : Int? {
        return get(from, to).also { edgeMatrix[from][to] = value }
        //TODO: DETERMINE IF WE NEED TO RERUN PATHING ALGORITHMS
    }

    fun connect(from : Int, to : Int) : Boolean {
        return set(from, to, 1) == null
    }

    fun randomize(chanceFilled : Double, maxWeight: Int){
        for(i in edgeMatrix.indices) {
            for (j in edgeMatrix.indices) {
                if (chanceFilled > Math.random()) {
                    set(i, j, (1..maxWeight).random())
                } else {
                    set(i, j, -1)
                }
            }
        }
    }

    override fun toString(): String {
        val string = StringBuilder()
        for(destinations in edgeMatrix){
            string.append("[")
            for (weight in destinations){
                string.append("${weight.let{if(it == -1) " " else it}}][")
            }
            string.deleteRange(string.length - 2, string.length)
            string.append("]\n")
        }
        return string.toString()
    }

    fun printVertices() = println("Vertices: $vertices")
}