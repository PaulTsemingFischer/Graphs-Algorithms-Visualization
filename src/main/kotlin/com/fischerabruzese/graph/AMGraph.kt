package com.fischerabruzese.graph

import java.math.BigInteger
import java.util.LinkedList
import kotlin.random.Random
import kotlin.math.pow

/**
 * Represents a directed graph with non-negative edge weights.
 *
 * @param E The type of the vertices in the graph.
 * @param outboundConnections A list of pairs of vertices and their outbound connections and weights.
 */
class AMGraph<E:Any>(vararg outboundConnections : Pair<E,Iterable<Pair<E,Int>>>?) : Graph<E>() {

    companion object AlternateConstructors{

        /**
         * Constructs a graph containing vertices and their outbound connections with edge weights of 1.
         *
         * @param connections A list of pairs of vertices and their outbound connections with no weights.
         */
        fun<E : Any> graphOf(vararg connections : Pair<E,Iterable<E>?>) : AMGraph<E> {
            return AMGraph(*connections.map {
                it.first to ( it.second?.map { it2 -> it2 to 1 } ?: emptyList() )
            }.toTypedArray())
        }

        /**
         * Constructs a graph containing unconnected vertices.
         *
         * @param vertices The vertices to add to the graph.
         */
        fun<E : Any> graphOf(vararg vertices : E) : AMGraph<E> {
            return AMGraph(*vertices.map {
                it to emptyList<Pair<E,Int>>()
            }.toTypedArray())
        }
    }

    /**
     * Constructs an empty graph.
     */
    constructor() : this(
        null
    )

    private var vertices : ArrayList<E> = ArrayList()// Vert index --> E
    private var edgeMatrix : Array<IntArray> // [Vert index][Vert index] --> edge weight
    private val indexLookup = HashMap<E, Int>() // E --> Vert index
    private var dijkstraTables : Array<Array<Pair<Int, Int>>?> // Vert index --> cached dijkstra table

    /**
     * @return The number of vertices in the graph.
     */
    override fun size() = vertices.size

    /**
     * Represents a graph data structure.
     *
     * Example: val graph = AMGraph('a' to listOf('b' to 1), ...)
     *
     * @param outboundConnections A list of pairs of vertices and their outbound connections and weights.
     */
    init {
        for(connections in outboundConnections){
            if(connections == null) continue
            if(indexLookup.putIfAbsent(connections.first, vertices.size) == null){
                vertices.add(connections.first)
            }
            for(outboundEdge in connections.second){
                if(indexLookup.putIfAbsent(outboundEdge.first, vertices.size) == null){
                    vertices.add(outboundEdge.first)
                }
            }
        }
        dijkstraTables = Array(size()){null}
        edgeMatrix = Array(size()) {IntArray(size()) {-1} }

        for(connections in outboundConnections) {
            if(connections == null) continue
            for(outboundEdge in connections.second){
                try {
                    if(outboundEdge.second <= 0) throw IllegalArgumentException()
                    else edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] = outboundEdge.second
                } catch (e : IllegalArgumentException){
                    edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] = 1
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
     * @param value The weight to set on the edge between the two vertices.
     * @return The previous weight of the edge between the two vertices, or null if no edge existed.
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

    private fun contains(vertex: E) : Boolean {
        return indexLookup.containsKey(vertex)
    }

    private fun neighbors(vertex: E) : List<E>? {
        return indexLookup[vertex]?.let {
            ArrayList<E>().apply {
                for((i,ob) in edgeMatrix[it].withIndex())
                    if (ob != -1) add(vertices[i])
            }
        }
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
     * Sets random connections between vertices between 0 and maxWeight.
     * @param probability The probability of a connection being made. Must be between 0 and 1.
     * @param maxWeight The maximum weight of a connection.
     */
    fun randomize(probability : Double, maxWeight: Int){
        randomize({Random.nextDouble() < probability}, maxWeight)
    }

    fun randomizeSmart(avgConnectionsPerVertex: Int, maxWeight: Int){
        val probability = avgConnectionsPerVertex.toDouble() / size()
        randomize(probability, maxWeight)
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
    override fun add(vararg verts : E){
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
     * @param verts The vertices to remove from the graph.
     */
    override fun remove(vararg verts : E){
        val vertexToRemove = Array(size()){false}
        for (vertex in verts){
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
     * Finds the shortest path between two vertices using dijkstra's algorithm. If dijkstra's algorithm has already been run, the cached table is used.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return A list of vertices representing the shortest path between the two vertices.
     */
    override fun path(from: E, to: E): List<E>{
        return path(from, to, false)
    }

    fun path(from: E, to: E, useSimpleAlgorithm : Boolean = false): List<E>{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return try {
            tracePath(fromIndex, toIndex, if(useSimpleAlgorithm) getDijkstraTableSimple(fromIndex) else getDijkstraTable(fromIndex)).map { vertices[it] }
        } catch (e : IndexOutOfBoundsException) {
            emptyList()
        }
    }

    /**
     * Finds the shortest distance between two vertices using dijkstra's algorithm. If dijkstra's algorithm has already been run, the cached table is used.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return The distance between the two vertices.
     */
    override fun distance(from: E, to: E) : Int{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return (getDijkstraTable(fromIndex)) [toIndex].second
    }

    private fun getDijkstraTable(fromIndex : Int) : Array<Pair<Int, Int>> {
        if (dijkstraTables[fromIndex] == null) dijkstraTables[fromIndex] = dijkstraFibHeap(fromIndex)
        return dijkstraTables[fromIndex]!!
    }

    private fun getDijkstraTableSimple(fromIndex : Int) : Array<Pair<Int, Int>> {
        if (dijkstraTables[fromIndex] == null) dijkstraTables[fromIndex] = dijkstra(fromIndex)
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
        return if(path.first() == from) path else emptyList()
    }

    /**
     * Finds a path between two vertices using a depth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun depthFirstSearch2(start: E, dest : E) : List<E> {
        return depthFirstSearch2(indexLookup[start]!!, indexLookup[dest]!!, BooleanArray(size())).map { vertices[it] }
    }

    private fun depthFirstSearch2(vertex : Int, dest : Int, visited: BooleanArray) : LinkedList<Int> {
        visited[vertex] = true
        for((ob,dist) in edgeMatrix[vertex].withIndex()){
            if(!visited[ob] && dist != -1){
                if(ob == dest) return LinkedList<Int>().apply{addFirst(ob); addFirst(vertex)}
                depthFirstSearch2(ob, dest, visited).let{if(it.isNotEmpty()) return it.apply{addFirst(vertex)}}
            }
        }
        return LinkedList()
    }

    /**
     * Finds a path between two vertices using a breadth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun breadthFirstSearch2(start: E, dest : E) : List<E> {
        return breadthFirstSearch2(indexLookup[start]!!, indexLookup[dest]!!).map { vertices[it] }
    }

    private fun breadthFirstSearch2(vertex : Int, dest : Int) : List<Int> {
        val q = LinkedList<Int>()
        val prev = IntArray(size()){ -1 }
        prev[vertex] = -2 //-2 represents start
        q.add(vertex)
        while(!q.isEmpty()){
            val currVer = q.pop()
            for((ob,dist) in edgeMatrix[currVer].withIndex()){
                if(prev[ob] == -1 && dist != -1){
                    q.addLast(ob)
                    prev[ob] = currVer
                }
            }
        }
        val path = LinkedList<Int>()
        var curr = dest
        path.addFirst(dest)
        while(prev[curr] != -2 && prev[curr] != -1){
            path.addFirst(prev[curr])
            curr = prev[curr]
        }
        if(path.first() == vertex) return path
        return emptyList()
    }

    /**
     * Finds a path between two vertices using a depth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun depthFirstSearch(start : E, dest : E) : List<E> = search(true, start, dest)

    /**
     * Finds a path between two vertices using a breadth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun breadthFirstSearch(start : E, dest : E) : List<E> = search(false, start, dest)

    private fun search(depth : Boolean, start : E, dest : E) : List<E>{
        val dest = indexLookup[dest]!!
        val q = LinkedList<Int>()
        val prev = IntArray(size()) { -1 }

        q.addFirst(indexLookup[start]!!)
        prev[indexLookup[start]!!] = -2

        while(!q.isEmpty()){
            val curPath = q.pop()

            for((ob,dist) in edgeMatrix[curPath].withIndex()){

                if(prev[ob] < 0 && dist != -1 && ob != curPath){
                    if (depth)
                        q.addFirst(ob)
                    else //breadth
                        q.addLast(ob)
                    prev[ob] = curPath
                    if(ob == dest) break
                }
            }
        }
        return LinkedList<E>().apply {
            var next : Int = dest
            if(prev[dest] != -1) {
                while(next != indexLookup[start]!!){
                    addFirst(vertices[next])
                    next = prev[next]
                }
                addFirst(start)
            }
        }
    }

    /**
     * Implements a Fibonacci Heap in Dijkstra's algorithm to queue vertices.
     */
    internal fun dijkstraFibHeap(from : Int, to : Int? = null) : Array<Pair<Int, Int>> {
        //Initialize each vertex's info mapped to ids
        val prev = IntArray(size()) { -1 }
        val dist = IntArray(size()) { Int.MAX_VALUE }
        dist[from] = 0

        //PriorityQueue storing Priority = dist, Value = id
        val heap = FibonacciHeap<Int, Int>()

        //Store Queue's nodes for easy search/updates
        val nodeCollection = Array<Node<Int,Int>?>(size()) { null }
        nodeCollection[from] = heap.insert(dist[from],from)

        //loop forever, or until we have visited to
        while(to == null || heap.minimum() == to){

            //store and remove next node, mark as visited, break if empty
            val cur = heap.extractMin() ?: break

            //iterate through potential outbound connections
            for((i,edge) in edgeMatrix[cur].withIndex()){

                //relax all existing connections
                if(edge != -1
                    //table update required if it's the shortest path (so far)
                    && dist[cur] + edge < dist[i]){

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
    internal fun dijkstra(from : Int, to : Int? = null) : Array<Pair<Int, Int>> {
        val distance = IntArray(size()) { Int.MAX_VALUE }
        val prev = IntArray(size()) { -1 }
        val visited = BooleanArray(size()) { false }

        distance[from] = 0
        while(to == null || !visited[to]){
            //Determine the next vertex to visit
            var currVert = visited.indexOfFirst{!it} //Finds first unvisited
            if(currVert == -1) break //All visited
            for(i in currVert + 1 until visited.size){
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

    internal fun clearDijkstraCache(){
        dijkstraTables = Array(size()){null}
    }

    /**
     * Colors the graph
     */
    private fun color(maxColors : Int) : Array<List<E>>?{
        require(maxColors > 0)
        val max = BigInteger(maxColors.toString()).pow(size()+1)
        var colors = BigInteger.ZERO //storing this array as an int for FANCY iteration

        fun getColor(index : Int): Int {
            return colors.divide(maxColors.toDouble().pow(index.toDouble()).toInt().toBigInteger()).mod(maxColors.toBigInteger()).toInt()
        }

        fun check() : Boolean {
            for(vert in vertices){
                for ((ob,w) in edgeMatrix[indexLookup[vert]!!].withIndex()){
                    if (w != -1 && getColor(ob) == getColor(indexLookup[vert]!!)){
                        return false
                    }
                }
            }
            return true
        }

        while(!check()){
            if(colors == max) return null
            colors = colors.plus(BigInteger.ONE)
        }

        return ArrayList<List<E>>().apply {
            for (vert in vertices.indices) {
                while (getColor(vert) >= size()) {
                    add(emptyList())
                }
                this[getColor(vert)].addLast(vertices[vert])
            }
        }.toTypedArray()
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