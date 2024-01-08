package com.fischerabruzese.graph

import java.util.LinkedList

/**
 * Represents a graph data structure.
 *
 * @param E The type of the vertices in the graph.
 */
class AMGraph<E:Any>(vararg outboundConnections : Pair<E,Iterable<E>>?, weights : List<Int>) : Graph<E>() {

    /**
     * Constructs a graph containing unconnected vertices.
     *
     * @param vertices The vertices to add to the graph.
     */
    constructor(vararg vertices: E) : this(
        *vertices.map { it to emptyList<E>()}.toTypedArray(), weights = emptyList<Int>()
    )

    /**
     * Constructs an empty graph.
     */
    internal constructor() : this(
        null, weights = emptyList<Int>()
    )


    private var vertices : ArrayList<E> = ArrayList()

    //TODO: make this private
    var edgeMatrix : Array<IntArray>

    private val indexLookup = HashMap<E, Int>()

    private var dijkstraTables : Array<Array<Pair<Int, Int>>?>

    /**
     * @return The number of vertices in the graph.
     */
    fun size() = vertices.size

    /**
     * Represents a graph data structure.
     *
     * @param outboundConnections A list of pairs of vertices and their outbound connections.
     * @param weights A list of weights corresponding to the outbound connections.
     */
    init {
        for(connections in outboundConnections){
            if(connections == null) continue
            if(indexLookup.putIfAbsent(connections.first, vertices.size) == null){
                vertices.add(connections.first)
            }
            for(outboundEdge in connections.second){
                if(indexLookup.putIfAbsent(outboundEdge, vertices.size) == null){
                    vertices.add(outboundEdge)
                }
            }
        }
        dijkstraTables = Array(size()){null}
        edgeMatrix = Array(size()) {IntArray(size()) {-1} }

        for(connections in outboundConnections) {
            if(connections == null) continue
            for((i, outboundEdge) in connections.second.withIndex()){
                try {
                    if(weights[i] <= 0) throw Exception()
                    else edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge]!!] = weights[i]
                } catch (e : Exception){
                    edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge]!!] = 1
                }
            }
        }
    }

    /**
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return The weight of the edge between the two vertices, or null if no edge exists.
     */
    override operator fun get(from : E, to : E ) : Int? {
        return indexLookup[from]?.let { f ->
            indexLookup[to]?.let { t ->
                get(f, t)
            }
        }
    }

    private fun get(from : Int, to : Int) : Int? {
        return if (edgeMatrix[from][to] == -1) null
        else edgeMatrix[from][to]
    }

    /**
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @param value The weight of the edge between the two vertices.
     * @return The previous weight of the edge between the two vertices, or null if no edge exists.
     */
    override operator fun set(from: E, to: E, value: Int): Int? {
        return indexLookup[from]?.let { f ->
            indexLookup[to]?.let { t ->
                set(f, t, value)
            }
        }
    }

    private fun set(from : Int, to : Int, value : Int) : Int? {
        dijkstraTables = Array(size()){null}
        return get(from, to).also { edgeMatrix[from][to] = value }
    }

    /**
     * @return An iterator over the vertices in the graph. The order is not guaranteed.
     */
    override fun iterator(): Iterator<E> = vertices.iterator()

    /**
     * @return A set of the vertices in the graph.
     */
    override fun getVertices(): Set<E> {
        return vertices.toSet()
    }

    /**
     * Sets random connections between vertices between 0 and maxWeight.
     * @param func A function that sets the probability of a connection being made.
     * @param maxWeight The maximum weight of a connection.
     */
    fun randomize(func : () -> Boolean, maxWeight: Int){
        for(i in edgeMatrix.indices) {
            for (j in edgeMatrix.indices) {
                if (func()) {
                    set(i, j, (1..maxWeight).random())
                } else {
                    set(i, j, -1)
                }
            }
        }
    }

    /**
     * Clears all edges in the graph.
     */
    fun clearEdges(){
        edgeMatrix = Array(size()) {IntArray(size()) {-1} }
        dijkstraTables = Array(size()){null}
    }

    /**
     * Adds a vertex to the graph.
     * @param verts The vertices to add to the graph.
     */
    fun add(vararg verts : E){
        for(vert in verts) indexLookup[vert] = size()
        vertices.addAll(verts)

        edgeMatrix = Array(size()) {i ->
            IntArray(size()) {j ->
                edgeMatrix.getOrNull(i)?.getOrNull(j) ?: -1
            }
        }
    }

    /**
     * Removes a vertex from the graph.
     * @param removals The vertices to remove from the graph.
     */
    fun remove(vararg removals : E){
        val vertexToRemove = Array(size()){false}
        for (vertex in removals){
            val id = indexLookup.remove(vertex) ?: continue
            vertexToRemove[id] = true
            vertices.removeAt(id)
        }


        val newEdgeMatrix = Array(size()) {IntArray(size()) {-1} }
        var fromOffset = 0
        for(from in edgeMatrix.indices) {
            if (vertexToRemove[from])
                fromOffset++
            else {
                var toOffset = 0
                for (to in edgeMatrix.indices) {
                    if (vertexToRemove[to])
                        toOffset++
                    else
                        newEdgeMatrix[from - fromOffset][to - toOffset] = edgeMatrix[from][to]
                }
            }
        }
        edgeMatrix = newEdgeMatrix
        dijkstraTables = Array(size()){null}
    }

    /**
     * Finds the shortest path between two vertices.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return A list of vertices representing the shortest path between the two vertices.
     */
    fun path(from: E, to: E): List<E>{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return tracePath(fromIndex, toIndex, getDijkstraTable(fromIndex)).map { vertices[it] }
    }

    /**
     * Finds the shortest distance between two vertices.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return The distance between the two vertices.
     * @precondition: If "to" is null, finds every path from "from", else only the path from "from" to "to" is accurate
     */
    fun distance(from: E, to: E) : Int{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return (getDijkstraTable(fromIndex)) [toIndex].second
    }

    private fun getDijkstraTable(fromIndex : Int) : Array<Pair<Int, Int>> {
        if (dijkstraTables[fromIndex] == null) dijkstraTables[fromIndex] = dijkstraFibHeap(fromIndex)
        return dijkstraTables[fromIndex]!!
    }

    private fun tracePath(from: Int, to: Int, dijkstraTable: Array<Pair<Int, Int>>): List<Int> {
        val path = LinkedList<Int>()
        path.add(to)
        var curr = to
        while(path.firstOrNull() != from){
            path.addFirst(dijkstraTable[curr].run {curr = first; first})
            if(path[0] == -1) {path.removeFirst(); break}
        }
        return path
    }

    /**
     * Implements a Fibonacci Heap in Dijkstra's algorithm to queue vertices.
     */
    fun dijkstraFibHeap(from : Int, to : Int? = null) : Array<Pair<Int, Int>> {
        //Initialize each vertex's info mapped to ids
        val prev = IntArray(size()) { -1 }
        val dist = IntArray(size()) { Int.MAX_VALUE }
        val visited = BooleanArray(size()) { false }

        dist[from] = 0

        //PriorityQueue storing Priority = dist, Value = id
        val heap = FibonacciHeap<Int, Int>()

        //store Queue's nodes for easy search/updates
        val nodeCollection = Array<Node<Int,Int>?>(size()) { null }
        nodeCollection[from] = heap.insert(dist[from],from)

        //loop forever, or until we mark destination (to) as visited
        while(to == null || !visited[to]){

            //store and remove next node, mark as visited, break if empty
            val cur = heap.extractMin()?.also{visited[it] = true} ?: break

            //iterate through potential outbound connections
            for((i,edge) in edgeMatrix[cur].withIndex()){

                //relax all existing connections
                if(edge != -1
                    //table update required if it's unvisited, and it's the shortest path (so far)
                    && !visited[i] && dist[cur] + edge < dist[i]){

                    //update
                        dist[i] = dist[cur] + edge
                        prev[i] = cur

                        //re-prioritize node or create and add it
                        if (nodeCollection[i] != null)
                            heap.decreaseKey(nodeCollection[i]!!, dist[i])
                        else nodeCollection[i] = heap.insert(dist[i],i)
                }
            }
        }
        return prev.zip(dist).toTypedArray()
    }

    /**
     * @precondition: If "to" is null, finds every path from "from", else only the path from "from" to "to" is accurate
     * @postcondition: Both Int.MAX_VALUE and -1 indicates no path
     * @return An array of (previous vertex index, distance)
     */
    fun dijkstra(from : Int, to : Int? = null) : Array<Pair<Int, Int>> {
        val distance = IntArray(size()) { Int.MAX_VALUE }
        val prev = IntArray(size()) { -1 }
        val visited = BooleanArray(size()) { false }

        distance[from] = 0
        while(to == null || !visited[to]){
            //Determine the next vertex to visit
            var currVert = visited.indexOfFirst{!it} //Finds first unvisited
            if(currVert == -1) break //All visited //TODO:  || distance[currVert] == Int.MAX_VALUE
            for(i in currVert + 1 until visited.size){//TODO: +1 might cause issues
                if(!visited[i] && distance[i] < distance[currVert]){
                    currVert = i
                }
            }
            //Update distances and previous
            val currDist = distance[currVert]
            for((i,edge) in edgeMatrix[currVert].withIndex()){
                if(!visited[i] && edge != -1 && currDist + edge < distance[i]){
                    distance[i] = (currDist + edgeMatrix[currVert][i])
                    prev[i] = currVert
                }
            }
            //Update visited
            visited[currVert] = true
        }
        return prev.zip(distance).toTypedArray() //funky function
    }

    /**
     * @return A string representation of the graph.
     */
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
}