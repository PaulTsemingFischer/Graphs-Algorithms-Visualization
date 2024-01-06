package com.fischerabruzese.graph

/**
 * Represents a node in the Fibonacci Heap data structure.
 *
 * @param V The type of the value stored in the node, must implement Comparable.
 */
class Node<V : Comparable<V>>(var value: V) {
    var parent: Node<V>? = null
    var child:  Node<V>? = null
    var prev:   Node<V>? = null
    var next:   Node<V>? = null
    var rank = 0
    var mark = false

    /**
     * Melds the current node with the given node, making the given node the previous sibling.
     *
     * @param node The node to meld with.
     */
    fun meld1(node: Node<V>) {
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
    fun meld2(node: Node<V>) {
        this.prev?.next = node
        node.prev?.next = this
        val temp = this.prev
        this.prev = node.prev
        node.prev = temp
    }
}

/**
 * Creates and returns a new Fibonacci Heap.
 *
 * @param V The type of values to be stored in the heap, must implement Comparable.
 * @return A new Fibonacci Heap instance.
 */fun <V: Comparable<V>> makeHeap() = FibonacciHeap<V>()

 /**
 * Represents a Fibonacci Heap data structure.
 *
 * @param V The type of values stored in the heap, must implement Comparable.
 * @property node The root node of the heap.
 */
class FibonacciHeap<V: Comparable<V>>(var node: Node<V>? = null) {

    /**
     * Inserts a new value into the heap and returns the corresponding node.
     *
     * @param v The value to insert.
     * @return The node containing the inserted value.
     */
    fun insert(v: V): Node<V> {
        val x = Node(v)
        if (this.node == null) {
            x.next = x
            x.prev = x
            this.node = x
        }
        else {
            this.node!!.meld1(x)
            if (x.value < this.node!!.value) this.node = x
        }
        return x
    }

    /**
     * Unions the current heap with another Fibonacci heap.
     *
     * @param other The heap to union with.
     */
    fun union(other: FibonacciHeap<V>) {
        if (this.node == null) {
            this.node = other.node
        }
        else if (other.node != null) {
            this.node!!.meld2(other.node!!)
            if (other.node!!.value < this.node!!.value) this.node = other.node
        }
        other.node = null
    }

    /**
     * Retrieves the minimum value in the heap without removing it.
     *
     * @return The minimum value in the heap.
     */
    fun minimum(): V? = this.node?.value

    /**
     * Extracts and returns the minimum value from the heap.
     *
     * @return The extracted minimum value.
     */
    fun extractMin(): V? {
        if (this.node == null) return null
        val min = minimum()
        val roots = mutableMapOf<Int, Node<V>>()

        fun add(r: Node<V>) {
            r.prev = r
            r.next = r
            var rr = r
            while (true) {
                var x = roots[rr.rank] ?: break
                roots.remove(rr.rank)
                if (x.value < rr.value) {
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

        var r = this.node!!.next
        while (r != this.node) {
            val n = r!!.next
            add(r)
            r = n
        }
        val c = this.node!!.child
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
            this.node = null
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
            if (rr.value < mv.value) mv = rr
        }
        this.node = mv
        return min
    }

    /**
     * Decreases the key of a node to a new value.
     *
     * @param n The node whose key should be decreased.
     * @param v The new value for the node.
     * @throws IllegalArgumentException if the new value is greater than the existing value.
     */
    fun decreaseKey(n: Node<V>, v: V) {
        require (n.value >= v) {
            "In 'decreaseKey' new value greater than existing value"
        }
        n.value = v
        if (n == this.node) return
        val p = n.parent
        if (p == null) {
            if (v < this.node!!.value) this.node = n
            return
        }
        cutAndMeld(n)
    }

    private fun cut(x: Node<V>) {
        val p = x.parent
        if (p == null) return
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

    private fun cutAndMeld(x: Node<V>) {
        cut(x)
        x.parent = null
        this.node?.meld1(x)
    }

    /**
     * Deletes a node from the heap.
     *
     * @param n The node to delete.
     */
    fun delete(n: Node<V>) {
        val p = n.parent
        if (p == null) {
            if (n == this.node) {
                extractMin()
                return
            }
            n.prev?.next = n.next
            n.next?.prev = n.prev
        }
        else {
            cut(n)
        }
        var c = n.child
        if (c == null) return
        while (true) {
            c!!.parent = null
            c = c.next
            if (c == n.child) break
        }
        this.node?.meld2(c!!)
    }

    /**
     * Visualizes the structure of the heap.
     */
    fun visualize() {
        if (this.node == null) {
            println("<empty>")
            return
        }

        fun f(n: Node<V>, pre: String) {
            var pc = "│ "
            var x = n
            while (true) {
                if (x.next != n) {
                    print("$pre├─")
                }
                else {
                    print("$pre└─")
                    pc = "  "
                }
                if (x.child == null) {
                    println("╴ ${x.value}")
                }
                else {
                    println("┐ ${x.value}")
                    f(x.child!!, pre + pc)
                }
                if (x.next == n) break
                x = x.next!!
            }
        }
        f(this.node!!, "")
    }
}

fun main(args: Array<String>) {
    println("MakeHeap:")
    val h = makeHeap<String>()
    h.visualize()

    println("\nInsert:")
    h.insert("cat")
    h.visualize()

    println("\nUnion:")
    val h2 = makeHeap<String>()
    h2.insert("rat")
    h.union(h2)
    h.visualize()

    println("\nMinimum:")
    var m = h.minimum()
    println(m)

    println("\nExtractMin:")
    // add a couple more items to demonstrate parent-child linking that
    // happens on delete min.
    h.insert("bat")
    val x = h.insert("meerkat")  // save x for decrease key and delete demos.
    m = h.extractMin()
    println("(extracted $m)")
    h.visualize()

    println("\nDecreaseKey:")
    h.decreaseKey(x, "gnat")
    h.visualize()

    println("\nDelete:")
    // add a couple more items.
    h.insert("bobcat")
    h.insert("bat")
    println("(deleting ${x.value})")
    h.delete(x)
    h.visualize()
}