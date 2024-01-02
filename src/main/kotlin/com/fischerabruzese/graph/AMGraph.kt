package com.fischerabruzese.graph

import java.util.LinkedList
import java.util.PriorityQueue

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
    private val dijkstraTables = Array<Array<Pair<Int, Int>>?>(vertices.size){null}

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
        edgeMatrix = Array(vertices.size) {IntArray(vertices.size) {-1} }

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
        return get(from, to).also { edgeMatrix[from][to] = value }
    }

    override fun iterator(): Iterator<E> {
        TODO("Not yet implemented")
    }

    override fun getVerticies(): Set<E> {
        return vertices.toSet()
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

    fun clearEdges(){
        for(from in edgeMatrix.indices)
            for(to in edgeMatrix.indices)
                edgeMatrix[from][to] = 0
    }

    fun add(vararg additions : E){
        vertices.addAll(additions)
        val newEdgeMatrix = Array(additions.size) {IntArray(additions.size) {-1} }
        for(from in edgeMatrix.indices)
            for(to in edgeMatrix.indices)
                newEdgeMatrix[from][to] = edgeMatrix[from][to]
        edgeMatrix = newEdgeMatrix
    }

    fun remove(vararg removals : E){
        val vertexToRemove = Array(vertices.size){false}
        for (vertex in removals.map { indexLookup[it]!! }){
            vertexToRemove[vertex] = true
        }

        val newEdgeMatrix = Array(vertices.size - removals.size) {IntArray(vertices.size - removals.size) {-1} }
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
    }
    //Dijkstra
    fun getDijkstraPath(from: E, to: E): List<E>{
        if()
    }
    fun getDijkstra(from: E, to: E) : Pair<List<E>, Int>{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return dikstra(fromIndex, toIndex).let{
            getPath(fromIndex, toIndex, it).map{vertex -> vertices[vertex]} to it[toIndex].second
        }
    }

    fun getDijkstraWeight(from: E, to: E) : Int{
        val fromIndex = indexLookup[from]!!
        val toIndex = indexLookup[to]!!
        return dikstra(fromIndex, toIndex)[toIndex].second
    }


    //Pre-condition: If "to" is null, finds every path from "from", else only the path from "from" to "to" is accurate
    //Post-condition: A Int.MAX_VALUE in distance indicates unreachable, a -1 in Prev indicates no path
    //Post-condition: Returns an array of (previous vertex index, distance)
    private fun dikstra(from : Int, to : Int?) : Array<Pair<Int, Int>> {

        val distance = IntArray(vertices.size) { Int.MAX_VALUE }
        val prev = IntArray(vertices.size) { -1 }
        val visited = BooleanArray(vertices.size) { false }

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

    private fun dikstra2(from : Int, to : Int?) : Array<Pair<Int, Int>> {
        data class Vertex(val id : Int, var prev : Int, var distFromSrc : Int, var visited : Boolean) : Comparable<Vertex>{
            override fun compareTo(other: Vertex): Int {
                return this.distFromSrc.compareTo(other.distFromSrc)
            }
        }
        val vlist = List(vertices.size) { i -> Vertex(i, -1, Int.MAX_VALUE, false)}
        vlist[from].distFromSrc = 0
        val que = PriorityQueue<Vertex>()
        que.add(vlist[from])
        var deadNodeCounter = 0
        while(to == null || !vlist[to].visited){
            val currVert = que.peek() ?: break
            if(currVert.visited) {
                deadNodeCounter++
                que.remove()
                continue
            }
            for((i,edge) in edgeMatrix[currVert.id].withIndex()){
                if(edge != -1 && !vlist[i].visited && currVert.distFromSrc + edge < vlist[i].distFromSrc){
                    vlist[i].distFromSrc = currVert.distFromSrc + edge
                    vlist[i].prev = currVert.id
                    que.add(vlist[i])
                }
            }
            que.poll().visited = true
        }
        println(deadNodeCounter)
        return Array(vlist.size) { i -> vlist[i].prev to vlist[i].distFromSrc}
    }

    private fun getPath(from: Int, to: Int, dikstraTable: Array<Pair<Int, Int>>): List<Int> {
        val path = LinkedList<Int>()
        path.add(to)
        var curr = to
        while(path.firstOrNull() != from){
            path.addFirst(dikstraTable[curr].first.also{curr = it})
        }
        return path
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