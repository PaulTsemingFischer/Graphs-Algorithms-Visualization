package com.fischerabruzese.graph

/**
 * Represents a node in the Fibonacci Heap data structure.
 *
 * @param P The type of the priority to compare the nodes against must implement Comparable.
 * @param V The type of value you want to store
 * @param priority the priority of the node
 * @param value the value you want to store at that priority
 */
class Node<P : Comparable<P>, V>(var priority : P, var value: V) {
    var parent: Node<P, V>? = null
    var child:  Node<P, V>? = null
    var prev:   Node<P, V>? = null
    var next:   Node<P, V>? = null
    var rank = 0
    var mark = false

    /**
     * Melds the current node with the given node, making the given node the previous sibling.
     *
     * @param node The node to meld with.
     */
    fun meld1(node: Node<P, V>) {
        this.prev?.next = node
        node.prev = this.prev
        node.next = this
        this.prev = node
    }

    /**
     * Melds the current node with the given node, considering the previous siblings of both nodes.
     *
     * @param node The node to meld with.
     */
    fun meld2(node: Node<P, V>) {
        this.prev?.next = node
        node.prev?.next = this
        val temp = this.prev
        this.prev = node.prev
        node.prev = temp
    }
}

 /**
 * Represents a Fibonacci Heap data structure.
 *
 * @param V The type of values stored in the heap, must implement Comparable.
 * @property minNode The root node of the heap.
 */
class FibonacciHeap<P : Comparable<P>, V>(var minNode: Node<P, V>? = null) {

    /**
     * Inserts a new value into the heap and returns the corresponding node.
     *
     * @param priority The priority of the value to insert.
     * @param value The value to store at that priority
     * @return The node containing the inserted value.
     */
    fun insert(priority : P, value: V): Node<P, V> {
        val x = Node(priority, value)
        if (this.minNode == null) {
            x.next = x
            x.prev = x
            this.minNode = x
        }
        else {
            this.minNode!!.meld1(x)
            if (x.priority < this.minNode!!.priority) this.minNode = x
        }
        return x
    }

    /**
     * Unions the current heap with another Fibonacci heap.
     *
     * @param other The heap to union with.
     */
    fun union(other: FibonacciHeap<P, V>) {
        if (this.minNode == null) {
            this.minNode = other.minNode
        }
        else if (other.minNode != null) {
            this.minNode!!.meld2(other.minNode!!)
            if (other.minNode!!.priority < this.minNode!!.priority) this.minNode = other.minNode
        }
        other.minNode = null
    }

    /**
     * Retrieves the minimum value in the heap without removing it.
     *
     * @return The minimum value in the heap.
     */
    fun minimum(): V? = this.minNode?.value



    /**
     * Extracts and returns the minimum value from the heap.
     *
     * @return The extracted minimum value.
     */
    fun extractMin(): V? {
        if (this.minNode == null) return null
        val min = minimum()
        val roots = mutableMapOf<Int, Node<P,V>>()

        fun add(r: Node<P,V>) {
            r.prev = r
            r.next = r
            var rr = r
            while (true) {
                var x = roots[rr.rank] ?: break
                roots.remove(rr.rank)
                if (x.priority < rr.priority) {
                    val t = rr
                    rr = x
                    x = t
                }
                x.parent = rr
                x.mark = false
                if (rr.child == null) {
                    x.next = x
                    x.prev = x
                    rr.child = x
                }
                else {
                    rr.child!!.meld1(x)
                }
                rr.rank++
            }
            roots[rr.rank] = rr
        }

        var r = this.minNode!!.next
        while (r != this.minNode) {
            val n = r!!.next
            add(r)
            r = n
        }
        val c = this.minNode!!.child
        if (c != null) {
            c.parent = null
            var rr = c.next!!
            add(c)
            while (rr != c) {
                val n = rr.next!!
                rr.parent = null
                add(rr)
                rr = n
            }
        }
        if (roots.isEmpty()) {
            this.minNode = null
            return min
        }
        val d = roots.keys.first()
        var mv = roots[d]!!
        roots.remove(d)
        mv.next = mv
        mv.prev = mv
        for ((_, rr) in roots) {
            rr.prev = mv
            rr.next = mv.next
            mv.next!!.prev = rr
            mv.next = rr
            if (rr.priority < mv.priority) mv = rr
        }
        this.minNode = mv
        return min
    }

    /**
     * Decreases the key of a node to a new value.
     *
     * @param n The node whose key should be decreased.
     * @param newPriority The new priority for the node.
     * @throws IllegalArgumentException if the new value is greater than the existing value.
     */
    fun decreaseKey(n: Node<P, V>, newPriority: P) {
        require (n.priority >= newPriority) {
            "In 'decreaseKey' new value greater than existing value"
        }
        n.priority = newPriority
        if (n == this.minNode) return
        val p = n.parent
        if (p == null) {
            if (newPriority < this.minNode!!.priority) this.minNode = n
            return
        }
        cutAndMeld(n)
    }

    private fun cut(x: Node<P,V>) {
        val p = x.parent ?: return
        p.rank--
        if (p.rank == 0) {
            p.child = null
        }
        else {
            p.child = x.next
            x.prev?.next = x.next
            x.next?.prev = x.prev
        }
        if (p.parent == null) return
        if (!p.mark) {
            p.mark = true
            return
        }
        cutAndMeld(p)
    }

    private fun cutAndMeld(x: Node<P,V>) {
        cut(x)
        x.parent = null
        this.minNode?.meld1(x)
    }

    /**
     * Deletes a node from the heap.
     *
     * @param n The node to delete.
     */
    fun delete(n: Node<P,V>) {
        val p = n.parent
        if (p == null) {
            if (n == this.minNode) {
                extractMin()
                return
            }
            n.prev?.next = n.next
            n.next?.prev = n.prev
        }
        else {
            cut(n)
        }
        var c: Node<P,V>? = n.child ?: return
        while (true) {
            c!!.parent = null
            c = c.next!!
            if (c == n.child) break
        }
        this.minNode?.meld2(c!!)
    }
}