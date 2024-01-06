package com.fischerabruzese.graph

import java.util.LinkedList
import java.util.PriorityQueue
import kotlin.random.Random

/* Construct a graph with outboundConnections containing:
        outboundConnection(Source, ArrayOf(Destination, Weight))
*/
class AMGraph<E:Any>(vararg outboundConnections : Pair<E,Iterable<E>>?, weights : List<Int>) : Graph<E>() {

    constructor(vararg vertices: E) : this(
        *vertices.map { it to emptyList<E>()}.toTypedArray(), weights = emptyList<Int>()
    )

    internal constructor() : this(
        null, weights = emptyList<Int>()
    )

    private var vertices : ArrayList<E> = ArrayList()
    var edgeMatrix : Array<IntArray> //TODO:make this private
    private val indexLookup = HashMap<E, Int>()
    private var dijkstraTables : Array<Array<Pair<Int, Int>>?>
    fun size() = vertices.size

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

    override fun iterator(): Iterator<E> = vertices.iterator()

    override fun getVertices(): Set<E> {
        return vertices.toSet()
    }

    fun randomize(random: Random, maxWeight: Int){
        for(i in edgeMatrix.indices) {
            for (j in edgeMatrix.indices) {
                if (0.01 > random.nextDouble()) {
                    set(i, j, (1..maxWeight).random())
                } else {
                    set(i, j, -1)
                }
            }
        }
    }

    fun clearEdges(){
        edgeMatrix = Array(size()) {IntArray(size()) {-1} }
        dijkstraTables = Array(size()){null}
    }

    fun add(vararg verts : E){
        for(vert in verts) indexLookup[vert] = size()
        vertices.addAll(verts)

        edgeMatrix = Array(size()) {i ->
            IntArray(size()) {j ->
                edgeMatrix.getOrNull(i)?.getOrNull(j) ?: -1
            }
        }
    }

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

    fun path(from: E, to: E): List<E>{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return tracePath(fromIndex, toIndex, generateDijkstraTable(fromIndex)).map { vertices[it] }
    }

    fun distance(from: E, to: E) : Int{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return (generateDijkstraTable(fromIndex)) [toIndex].second
    }

    fun path2(from: E, to: E): List<E>{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return tracePath(fromIndex, toIndex, generateDijkstraTable2(fromIndex)).map { vertices[it] }
    }

    private fun generateDijkstraTable(fromIndex : Int) : Array<Pair<Int, Int>> {
        if (dijkstraTables[fromIndex] == null) dijkstraTables[fromIndex] = dijkstra(fromIndex)
        return dijkstraTables[fromIndex]!!
    }

    private fun generateDijkstraTable2(fromIndex : Int) : Array<Pair<Int, Int>> {
        if (dijkstraTables[fromIndex] == null) dijkstraTables[fromIndex] = dijkstra2(fromIndex)
        return dijkstraTables[fromIndex]!!
    }

    private fun tracePath(from: Int, to: Int, pathingTable: Array<Pair<Int, Int>>): List<Int> {
        val path = LinkedList<Int>()
        path.add(to)
        var curr = to
        while(path.firstOrNull() != from){
            path.addFirst(pathingTable[curr].run {curr = first; first})
            if(path[0] == -1) {path.removeFirst(); break}
        }
        return path
    }

    private fun dijkstra3(from : Int, to : Int? = null) : Array<Pair<Int, Int>> {
        //initialize default values
        data class Vertex(val id : Int, var prevId : Int, var dist : Int, var visited : Boolean) : Comparable<Vertex>{
            override fun compareTo(other: Vertex) = this.dist.compareTo(other.dist) //visitation will be based on distances from src
        }

        val vlist = List(size()) { i -> Vertex(i, -1, Int.MAX_VALUE, false)}
        vlist[from].dist = 0
        //create a que of vertexes to visit
        val que = PriorityQueue<Vertex>()
        que.add(vlist[from])

        //var deadNodes = 0 // for analysis purposes
        while(to == null || !vlist[to].visited){ //exit if we mark our destination as visited
            val currVert = que.peek() ?: break //exit if we've emptied the que

            //if a shorter path was already found (it's a dead node in que)
            if(currVert.visited) {
                //deadNodes++
                que.remove()
                continue //continue to next iteration
            }

            //iterate through potential outbound connections
            for((i,edge) in edgeMatrix[currVert.id].withIndex()){
                /* relax edge.
                update required if: edge exists, it's unvisited, it's the shortest path (so far) */
                if(edge != -1 && !vlist[i].visited && currVert.dist + edge < vlist[i].dist){
                    //update
                    vlist[i].dist = currVert.dist + edge
                    vlist[i].prevId = currVert.id
                    //que unvisited node
                    que.add(vlist[i])
                }
            }

            //remove and mark as visited
            que.poll().visited = true
        }
        //println(deadNodes)
        //create array storing id (in index), previd, and distance from src
        return Array(vlist.size) { i -> vlist[i].prevId to vlist[i].dist}
    }

    private fun dijkstra(from : Int, to : Int? = null) : Array<Pair<Int, Int>> {
        //initialize default values
        data class Vertex(val id : Int, var prevId : Int, var dist : Int, var visited : Boolean) : Comparable<Vertex>{
            override fun compareTo(other: Vertex) = this.dist.compareTo(other.dist) //visitation will be based on distances from src
        }

        //sorted by id
        val djkTable = List(size()) { i -> Vertex(i, -1, Int.MAX_VALUE, false)}
        djkTable[from].dist = 0

        //create an order of vertexes to visit
        val heap = FibonacciHeap<Vertex>()
        //store vertices for easy search/updates
        val nodeCollection = Array<Node<Vertex>?>(size()) { null }
        nodeCollection[from] = heap.insert(djkTable[from])

        while(to == null || !djkTable[to].visited){ //exit if we mark our destination as visited

            //store and remove next node, mark as visited, break if empty
            val currVert = heap.extractMin()?.apply{visited = true} ?: break

            //iterate through potential outbound connections
            for((i,edge) in edgeMatrix[currVert.id].withIndex()){

                //relax all existing connections
                if(edge != -1
                    //table update required if it's unvisited, and it's the shortest path (so far)
                    && !djkTable[i].visited && currVert.dist + edge < djkTable[i].dist){

                        //update
                        djkTable[i].dist = currVert.dist + edge
                        djkTable[i].prevId = currVert.id

                        //re-prioritize node or create and add it
                        if (nodeCollection[i] != null)
                            heap.decreaseKey(nodeCollection[i]!!, djkTable[i])
                        else nodeCollection[i] = heap.insert(djkTable[i])
                }
            }

        }
        //create array storing id (in index), previd, and distance from src
        return Array(djkTable.size) { i -> djkTable[i].prevId to djkTable[i].dist}
    }

    //Pre-condition: If "to" is null, finds every path from "from", else only the path from "from" to "to" is accurate
    //Post-condition: A Int.MAX_VALUE in distance indicates unreachable, a -1 in Prev indicates no path
    //Post-condition: Returns an array of (previous vertex index, distance)
    private fun dijkstra2(from : Int, to : Int? = null) : Array<Pair<Int, Int>> {
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