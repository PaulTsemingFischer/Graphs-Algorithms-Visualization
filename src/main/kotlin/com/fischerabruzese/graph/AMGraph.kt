//TODO: Separate randomize avg connections and probability so that you can do decimal avg connections
//TODO: Make randomize depend on the toggle
//TODO: Graph presets
//TODO: Figure out the issue with OG dijkstra

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

    companion object AlternateConstructors {

        /**
         * Constructs a graph containing vertices and their outbound connections with edge weights of 1.
         *
         * @param connections A list of pairs of vertices and their outbound connections with no weights.
         */
        fun <E : Any> graphOf(vararg connections: Pair<E, Iterable<E>?>): AMGraph<E> {
            return AMGraph(*connections.map {
                it.first to (it.second?.map { it2 -> it2 to 1 } ?: emptyList())
            }.toTypedArray())
        }

        /**
         * Constructs a graph containing unconnected vertices.
         *
         * @param vertices The vertices to add to the graph.
         */
        fun <E : Any> graphOf(vararg vertices: E): AMGraph<E> {
            return AMGraph(*vertices.map {
                it to emptyList<Pair<E, Int>>()
            }.toTypedArray())
        }
    }

    /**
     * Constructs an empty graph.
     */
    constructor() : this(
        null
    )

    private var vertices: ArrayList<E> = ArrayList()// Vert index --> E
    private var edgeMatrix: Array<IntArray> // [Vert index][Vert index] --> edge weight
    private val indexLookup = HashMap<E, Int>() // E --> Vert index
    private var dijkstraTables: Array<Array<Pair<Int, Int>>?> // Vert index --> cached dijkstra table(pair of previous, distance to every vertex)

    /*------------------ FUNCTIONALITY ------------------*/
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
        for (connections in outboundConnections) {
            if (connections == null) continue
            if (indexLookup.putIfAbsent(connections.first, vertices.size) == null) {
                vertices.add(connections.first)
            }
            for (outboundEdge in connections.second) {
                if (indexLookup.putIfAbsent(outboundEdge.first, vertices.size) == null) {
                    vertices.add(outboundEdge.first)
                }
            }
        }
        dijkstraTables = Array(size()) { null }
        edgeMatrix = Array(size()) { IntArray(size()) { -1 } }

        for (connections in outboundConnections) {
            if (connections == null) continue
            for (outboundEdge in connections.second) {
                try {
                    if (outboundEdge.second <= 0) throw IllegalArgumentException()
                    else edgeMatrix[indexLookup[connections.first]!!][indexLookup[outboundEdge.first]!!] =
                        outboundEdge.second
                } catch (e: IllegalArgumentException) {
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
    override operator fun get(from: E, to: E): Int? {
        return indexLookup[from]?.let { f ->
            indexLookup[to]?.let { t ->
                get(f, t)
            }
        }
    }

    private fun get(from: Int, to: Int): Int? {
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

    private fun set(from: Int, to: Int, value: Int): Int? {
        dijkstraTables = Array(size()) { null }
        return get(from, to).also { edgeMatrix[from][to] = value }
    }

    private fun contains(vertex: E): Boolean {
        return indexLookup.containsKey(vertex)
    }

    private fun neighbors(vertex: E): List<E>? {
        return indexLookup[vertex]?.let {
            ArrayList<E>().apply {
                for ((i, ob) in edgeMatrix[it].withIndex())
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
     * Clears all edges in the graph.
     */
    fun clearEdges() {
        edgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        dijkstraTables = Array(size()) { null }
    }

    /**
     * Adds a vertex to the graph.
     * @param verts The vertices to add to the graph.
     */
    override fun add(vararg verts: E) {
        for (vert in verts) indexLookup[vert] = size()
        vertices.addAll(verts)

        edgeMatrix = Array(size()) { i ->
            IntArray(size()) { j ->
                edgeMatrix.getOrNull(i)?.getOrNull(j) ?: -1
            }
        }
    }

    /**
     * Removes a vertex from the graph.
     * @param verts The vertices to remove from the graph.
     */
    override fun remove(vararg verts: E) {
        val vertexToRemove = Array(size()) { false }
        for (vertex in verts) {
            val id = indexLookup.remove(vertex) ?: continue
            vertexToRemove[id] = true
            vertices.removeAt(id)
        }

        val newEdgeMatrix = Array(size()) { IntArray(size()) { -1 } }
        var fromOffset = 0
        for (from in edgeMatrix.indices) {
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
        dijkstraTables = Array(size()) { null }
    }

    /**
     * @return A string representation of the graph.
     */
    override fun toString(): String {
        val string = StringBuilder()
        for (destinations in edgeMatrix) {
            string.append("[")
            for (weight in destinations) {
                string.append("${weight.let { if (it == -1) " " else it }}][")
            }
            string.deleteRange(string.length - 2, string.length)
            string.append("]\n")
        }
        return string.toString()
    }

    /*------------------ RANDOMIZATION ------------------*/
    /**
     * Sets random connections between vertices between 0 and maxWeight.
     * @param func A function that sets the probability of a connection being made.
     * @param maxWeight The maximum weight of a connection.
     */

    /**
     * Sets random connections with a probability that will average a certain amount of connections per vertex
     * @param avgConnectionsPerVertex The average amount of a connections per vertex. If fully connected is set to true, it must be greater than or equal to 1.
     * @param maxWeight The maximum weight of a connection.
     * @param fullyConnected Makes sure that there's always at least one connection per vertex
     * @param random A random object that determines what graph is constructed
     */
    override fun randomize(avgConnectionsPerVertex: Int, maxWeight: Int, fullyConnected: Boolean, random: Random) { //when inheritance removed add default values
        val probability = ( avgConnectionsPerVertex.toDouble() + (if(fullyConnected) -1 else 0) ) / size()
        randomize(probability, maxWeight, fullyConnected, random)
    }
    /**
     * Sets random connections between vertices between 0 and maxWeight.
     * @param probability The probability of a connection being made. Must be between 0 and 1. If fully connected is set to true, it will be the chance of additional connections per vertex
     * @param maxWeight The maximum weight of a connection.
     * @param fullyConnected Makes sure that there's always at least one connection per vertex
     * @param random A random object that determines what graph is constructed
     */
    override fun randomize(probability: Double, maxWeight: Int, fullyConnected: Boolean, random: Random) { //when removed add default values
        randomize({ random.nextDouble() < probability }, maxWeight)
        if(fullyConnected) randomFullyConnect(maxWeight, random)
    }
    private fun randomize(func: () -> Boolean, maxWeight: Int, random: Random = Random) {
        for (i in edgeMatrix.indices) {
            for (j in edgeMatrix.indices) {
                if (func()) {
                    set(i, j, random.nextInt(1,maxWeight))
                } else {
                    set(i, j, -1)
                }
            }
        }
    }

    /**
     *  Adds edges so that the graph is fully-connected
     */
    private fun randomFullyConnect(maxWeight: Int, random: Random){
        val bidirectional = getBidirectionalUnweighted()
        var vertex = random.nextInt(size())
        var unreachables : List<Int>

        //Runs while there are any unreachable vertices from `vertex` (store all the unreachable ones in `unreachables`)
        //Vertices --> Unreachable non-self vertices --> Unreachable non-self id's
        while(vertices.filter { indexLookup[it] != vertex && bidirectional.path(vertices[vertex], it, false).isEmpty() }.map{indexLookup[it]!!}.also{ unreachables = it }.isNotEmpty()){
            val from : Int
            val to : Int
            val weight = random.nextInt(maxWeight)
            if(random.nextBoolean()) {
                from = vertex
                to = unreachables.random(random)
            }
            else {
                from = unreachables.random(random)
                to = vertex
            }
            edgeMatrix[from][to] = weight
            bidirectional.set(from, to, 1)
            bidirectional.set(to, from, 1)

            vertex = random.nextInt(size())
            unreachables = emptyList()
        }
    }

    /*------------------ PATHING ------------------*/
    /*BFS and DFS */
    /**
     * Finds a path between two vertices using either depth or breadth.
     * @param depth true will use depth first search false will use breadth first search
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    private fun search(depth: Boolean, start: E, dest: E): List<E> {
        val dest = indexLookup[dest]!!
        val q = LinkedList<Int>()
        val prev = IntArray(size()) { -1 }

        q.addFirst(indexLookup[start]!!)
        prev[indexLookup[start]!!] = -2

        while (!q.isEmpty()) {
            val curPath = q.pop()

            for ((ob, dist) in edgeMatrix[curPath].withIndex()) {

                if (prev[ob] < 0 && dist != -1 && ob != curPath) {
                    if (depth)
                        q.addFirst(ob)
                    else //breadth
                        q.addLast(ob)
                    prev[ob] = curPath
                    if (ob == dest) break
                }
            }
        }
        return LinkedList<E>().apply {
            var next: Int = dest
            if (prev[dest] != -1) {
                while (next != indexLookup[start]!!) {
                    addFirst(vertices[next])
                    next = prev[next]
                }
                addFirst(start)
            }
        }
    }
    /**
     * Finds a path between two vertices using a depth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun depthFirstSearch(start: E, dest: E): List<E> = search(true, start, dest)
    /**
     * Finds a path between two vertices using a breadth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun breadthFirstSearch(start: E, dest: E): List<E> = search(false, start, dest)

    /**
     * Finds a path between two vertices using a depth first search.
     * @param start The vertex to start from.
     * @param dest The vertex to end at.
     * @return A list of vertices representing the path between the two vertices.
     */
    fun depthFirstSearch2(start: E, dest: E): List<E> {
        return depthFirstSearch2(indexLookup[start]!!, indexLookup[dest]!!, BooleanArray(size())).map { vertices[it] }
    }
    private fun depthFirstSearch2(vertex: Int, dest: Int, visited: BooleanArray): LinkedList<Int> {
        visited[vertex] = true
        for ((ob, dist) in edgeMatrix[vertex].withIndex()) {
            if (!visited[ob] && dist != -1) {
                if (ob == dest) return LinkedList<Int>().apply { addFirst(ob); addFirst(vertex) }
                depthFirstSearch2(ob, dest, visited).let { if (it.isNotEmpty()) return it.apply { addFirst(vertex) } }
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
    fun breadthFirstSearch2(start: E, dest: E): List<E> {
        return breadthFirstSearch2(indexLookup[start]!!, indexLookup[dest]!!).map { vertices[it] }
    }
    private fun breadthFirstSearch2(vertex: Int, dest: Int): List<Int> {
        val q = LinkedList<Int>()
        val prev = IntArray(size()) { -1 }
        prev[vertex] = -2 //-2 represents start
        q.add(vertex)
        while (!q.isEmpty()) {
            val currVer = q.pop()
            for ((ob, dist) in edgeMatrix[currVer].withIndex()) {
                if (prev[ob] == -1 && dist != -1) {
                    q.addLast(ob)
                    prev[ob] = currVer
                }
            }
        }
        val path = LinkedList<Int>()
        var curr = dest
        path.addFirst(dest)
        while (prev[curr] != -2 && prev[curr] != -1) {
            path.addFirst(prev[curr])
            curr = prev[curr]
        }
        if (path.first() == vertex) return path
        return emptyList()
    }

    /* DIJKSTRAS */
    /**
     * Finds the shortest path between two vertices using dijkstra's algorithm. If dijkstra's algorithm has already been run, the cached table is used.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return A list of vertices representing the shortest path between the two vertices.
     */
    override fun path(from: E, to: E): List<E> {
        return path(from, to, false)
    }

    fun path(from: E, to: E, useSimpleAlgorithm: Boolean = false): List<E> {
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        //println("SD Table: ${clearDijkstraCache()} ${getDijkstraTableSimple(fromIndex).contentDeepToString()}\n FD Table:${clearDijkstraCache()} ${getDijkstraTable(fromIndex).contentDeepToString()} \n")
        return try {
            tracePath(
                fromIndex,
                toIndex,
                if (useSimpleAlgorithm) getDijkstraTableSimple(fromIndex) else getDijkstraTable(fromIndex)
            ).map { vertices[it] }
        } catch (e: IndexOutOfBoundsException) { //More nodes were added that are disjoint and not in cached tables (we know there's no path)
            emptyList()
        }
    }

    /**
     * Finds the shortest distance between two vertices using dijkstra's algorithm. If dijkstra's algorithm has already been run, the cached table is used.
     * @param from The vertex to start from.
     * @param to The vertex to end at.
     * @return The distance between the two vertices.
     */
    override fun distance(from: E, to: E): Int {
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return (getDijkstraTable(fromIndex))[toIndex].second
    }

    private fun getDijkstraTable(fromIndex: Int): Array<Pair<Int, Int>> {
        if (dijkstraTables[fromIndex] == null) dijkstraTables[fromIndex] = dijkstraFibHeap(fromIndex)
        return dijkstraTables[fromIndex]!!
    }

    private fun getDijkstraTableSimple(fromIndex: Int): Array<Pair<Int, Int>> {
        if (dijkstraTables[fromIndex] == null) dijkstraTables[fromIndex] = dijkstra(fromIndex)
        return dijkstraTables[fromIndex]!!
    }

    /**
     *  Goes through the Dijkstra's table and returns a list of the path between from and to if it exists, and returns an empty list otherwise.
     */
    private fun tracePath(from: Int, to: Int, dijkstraTable: Array<Pair<Int, Int>>): List<Int> {
        val path = LinkedList<Int>()
        path.add(to)
        var curr = to
        while (path.firstOrNull() != from) {
            path.addFirst(dijkstraTable[curr].run { curr = first; first })
            if (path[0] == -1) {
                path.removeFirst(); break
            }
        }
        return if (path.first() == from) path else emptyList()
    }

    internal fun clearDijkstraCache() {
        dijkstraTables = Array(size()) { null }
    }

    /*------------------ COLORING ------------------*/
    /**
     * Colors the graph
     */
    private fun color(maxColors: Int): Array<List<E>>? {
        require(maxColors > 0)
        val max = BigInteger(maxColors.toString()).pow(size() + 1)
        var colors = BigInteger.ZERO //storing this array as an int for FANCY iteration

        fun getColor(index: Int): Int {
            return colors.divide(maxColors.toDouble().pow(index.toDouble()).toInt().toBigInteger())
                .mod(maxColors.toBigInteger()).toInt()
        }

        fun check(): Boolean {
            for (vert in vertices) {
                for ((ob, w) in edgeMatrix[indexLookup[vert]!!].withIndex()) {
                    if (w != -1 && getColor(ob) == getColor(indexLookup[vert]!!)) {
                        return false
                    }
                }
            }
            return true
        }

        while (!check()) {
            if (colors == max) return null
            colors = colors.plus(BigInteger.ONE)
        }

        return ArrayList<ArrayList<E>>().apply {
            for (vert in vertices.indices) {
                while (getColor(vert) >= size()) {
                    add(ArrayList())
                }
                this[getColor(vert)].addLast(vertices[vert])
            }
        }.toTypedArray()
    }

    /*------------------ CLUSTERING ------------------*/
    fun clusters() : List<AMGraph<E>>{
        return TODO()
    }

    fun karger(numAttempts: Int) : List<Pair<Int,Int>>{
        var bestCut = mincut()
        repeat(numAttempts - 1){
            val minCut = mincut()
            if(minCut.size < bestCut.size) bestCut = minCut
        }
        return mincut()
    }

    private fun mincut() : List<Pair<Int,Int>> {
        //'from' > 'to' in edges
        var edges: MutableList<Pair<Int, Int>> = ArrayList()

        //everything that is not equal to it's index is a reference on where to find your value
        val nodeRedirection = IntArray(size()) { it }

        //navigates through references until it finds the redirected value
        fun getLink(node: Int): Int {
            if (nodeRedirection[node] == node) return node
            return getLink(nodeRedirection[node])
        }

        var numNodes = size()

        //Initializing edges from edge-matrix, triangular to ensure from > to
        for (from in 1 until size()) {
            for (to in 0 until from) {
                if (edgeMatrix[from][to] > -1) edges.add(from to to)
                if (edgeMatrix[to][from] > -1) edges.add(from to to)
            }
        }

        //Randomize edge list
        randomizeList(edges)
        edges = LinkedList(edges) //turn into a queue so we can pop

        //Finding and cutting the (probably) min-cut
        fun collapse() {
            val edge = edges.pop()
            //from and to are the merged values (if they've previously been merged)
            val from = getLink(edge.first)
            val to = getLink(edge.second)

            if (from == to) return //If they've been merged together the edge doesn't exist anymore

            //Redirect the 2nd node so it becomes the first
            nodeRedirection[edge.second] = from
            //finished collapsing 2 nodes into 1
            numNodes--
        }

        fun cut(): List<Pair<Int, Int>> {
            //Make a concrete version of node redirection where there are no references for efficiency
            val concreteRedirection = IntArray(nodeRedirection.size)
            for (node in nodeRedirection.indices) {
                concreteRedirection[node] = getLink(node)
            }

            //'from' and 'to' are the 2 remaining nodes on our graph (no distinction between them)
            var from: Int = -1 //-1 is uninitialized
            var to: Int = -1
            //stores all the original nodes that collapsed to from/to
            val fromList = ArrayList<Int>() //-1 indicates
            val toList = ArrayList<Int>()

            //find the actual edges that exist between the 2 remaining nodes on our collapsed graph
            for ((original, new) in concreteRedirection.withIndex()) {
                //Initialize from/to with first appearance of
                if (from == -1) from = new
                else if (to == -1 && new != from) to = new

                //Add to the correct list
                when (new) {
                    from -> fromList
                    to -> toList
                    else -> continue
                }.add(original)
            }
            //Checking all possible original connections between our collapsed graph to return the necessary cut
            return ArrayList<Pair<Int, Int>>().apply {
                for (f in fromList) {
                    for (t in toList) {
                        if (edgeMatrix[f][t] > -1) add(f to t)
                        if (edgeMatrix[t][f] > -1) add(t to f)
                    }
                }
            }
        }

        //we cut the connections when we've collapsed everything into 2 nodes
        while (numNodes > 2){
//            if (edges.isEmpty())
//                throw IllegalStateException("")
            collapse()
        }
        return cut()//.also{println(it)}
    }

    private fun<T> randomizeList(list: MutableList<T>) {
        fun swap(index1: Int, index2: Int) {
            list[index1] = list.set(index2,list[index1])
        }

        for(i in list.indices){
            swap(i,Random.nextInt(i,list.size))
        }
    }
}

fun main() {

}