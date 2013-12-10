/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (C) 2013, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
/*
 * Based on j.u.c.ConcurrentSkipListMap
 */
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package co.paralleluniverse.concurrent.util;

import co.paralleluniverse.common.util.UtilUnsafe;
import java.util.*;

public class ConcurrentSkipListPriorityQueue<E> extends AbstractQueue<E>
        implements Cloneable,
        java.io.Serializable {
    /*
     * This class implements a tree-like two-dimensionally linked skip
     * list in which the index levels are represented in separate
     * nodes from the base nodes holding data.  There are two reasons
     * for taking this approach instead of the usual array-based
     * structure: 1) Array based implementations seem to encounter
     * more complexity and overhead 2) We can use cheaper algorithms
     * for the heavily-traversed index lists than can be used for the
     * base lists.  Here's a picture of some of the basics for a
     * possible list with 2 levels of index:
     *
     * Head nodes          Index nodes
     * +-+    right        +-+                      +-+
     * |2|---------------->| |--------------------->| |->null
     * +-+                 +-+                      +-+
     *  | down              |                        |
     *  v                   v                        v
     * +-+            +-+  +-+       +-+            +-+       +-+
     * |1|----------->| |->| |------>| |----------->| |------>| |->null
     * +-+            +-+  +-+       +-+            +-+       +-+
     *  v              |    |         |              |         |
     * Nodes  next     v    v         v              v         v
     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
     * | |->|A|->|B|->|C|->|D|->|E|->|F|->|G|->|H|->|I|->|J|->|K|->null
     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
     *
     * The base lists use a variant of the HM linked ordered set
     * algorithm. See Tim Harris, "A pragmatic implementation of
     * non-blocking linked lists"
     * http://www.cl.cam.ac.uk/~tlh20/publications.html and Maged
     * Michael "High Performance Dynamic Lock-Free Hash Tables and
     * List-Based Sets"
     * http://www.research.ibm.com/people/m/michael/pubs.htm.  The
     * basic idea in these lists is to mark the "next" pointers of
     * deleted nodes when deleting to avoid conflicts with concurrent
     * insertions, and when traversing to keep track of triples
     * (predecessor, node, successor) in order to detect when and how
     * to unlink these deleted nodes.
     *
     * Rather than using mark-bits to mark list deletions (which can
     * be slow and space-intensive using AtomicMarkedReference), nodes
     * use direct CAS'able next pointers.  On deletion, instead of
     * marking a pointer, they splice in another node that can be
     * thought of as standing for a marked pointer (indicating this by
     * using otherwise impossible field values).  Using plain nodes
     * acts roughly like "boxed" implementations of marked pointers,
     * but uses new nodes only when nodes are deleted, not for every
     * link.  This requires less space and supports faster
     * traversal. Even if marked references were better supported by
     * JVMs, traversal using this technique might still be faster
     * because any search need only read ahead one more node than
     * otherwise required (to check for trailing marker) rather than
     * unmasking mark bits or whatever on each read.
     *
     * This approach maintains the essential property needed in the HM
     * algorithm of changing the next-pointer of a deleted node so
     * that any other CAS of it will fail, but implements the idea by
     * changing the pointer to point to a different node, not by
     * marking it.  While it would be possible to further squeeze
     * space by defining marker nodes not to have key/value fields, it
     * isn't worth the extra type-testing overhead.  The deletion
     * markers are rarely encountered during traversal and are
     * normally quickly garbage collected. (Note that this technique
     * would not work well in systems without garbage collection.)
     *
     * In addition to using deletion markers, the lists also use
     * nullness of value fields to indicate deletion, in a style
     * similar to typical lazy-deletion schemes.  If a node's value is
     * null, then it is considered logically deleted and ignored even
     * though it is still reachable. This maintains proper control of
     * concurrent replace vs delete operations -- an attempted replace
     * must fail if a delete beat it by nulling field, and a delete
     * must return the last non-null value held in the field. (Note:
     * Null, rather than some special marker, is used for value fields
     * here because it just so happens to mesh with the Map API
     * requirement that method get returns null if there is no
     * mapping, which allows nodes to remain concurrently readable
     * even when deleted. Using any other marker value here would be
     * messy at best.)
     *
     * Here's the sequence of events for a deletion of node n with
     * predecessor b and successor f, initially:
     *
     *        +------+       +------+      +------+
     *   ...  |   b  |------>|   n  |----->|   f  | ...
     *        +------+       +------+      +------+
     *
     * 1. CAS n's value field from non-null to null.
     *    From this point on, no public operations encountering
     *    the node consider this mapping to exist. However, other
     *    ongoing insertions and deletions might still modify
     *    n's next pointer.
     *
     * 2. CAS n's next pointer to point to a new marker node.
     *    From this point on, no other nodes can be appended to n.
     *    which avoids deletion errors in CAS-based linked lists.
     *
     *        +------+       +------+      +------+       +------+
     *   ...  |   b  |------>|   n  |----->|marker|------>|   f  | ...
     *        +------+       +------+      +------+       +------+
     *
     * 3. CAS b's next pointer over both n and its marker.
     *    From this point on, no new traversals will encounter n,
     *    and it can eventually be GCed.
     *        +------+                                    +------+
     *   ...  |   b  |----------------------------------->|   f  | ...
     *        +------+                                    +------+
     *
     * A failure at step 1 leads to simple retry due to a lost race
     * with another operation. Steps 2-3 can fail because some other
     * thread noticed during a traversal a node with null value and
     * helped out by marking and/or unlinking.  This helping-out
     * ensures that no thread can become stuck waiting for progress of
     * the deleting thread.  The use of marker nodes slightly
     * complicates helping-out code because traversals must track
     * consistent reads of up to four nodes (b, n, marker, f), not
     * just (b, n, f), although the next field of a marker is
     * immutable, and once a next field is CAS'ed to point to a
     * marker, it never again changes, so this requires less care.
     *
     * Skip lists add indexing to this scheme, so that the base-level
     * traversals start close to the locations being found, inserted
     * or deleted -- usually base level traversals only traverse a few
     * nodes. This doesn't change the basic algorithm except for the
     * need to make sure base traversals start at predecessors (here,
     * b) that are not (structurally) deleted, otherwise retrying
     * after processing the deletion.
     *
     * Index levels are maintained as lists with volatile next fields,
     * using CAS to link and unlink.  Races are allowed in index-list
     * operations that can (rarely) fail to link in a new index node
     * or delete one. (We can't do this of course for data nodes.)
     * However, even when this happens, the index lists remain sorted,
     * so correctly serve as indices.  This can impact performance,
     * but since skip lists are probabilistic anyway, the net result
     * is that under contention, the effective "p" value may be lower
     * than its nominal value. And race windows are kept small enough
     * that in practice these failures are rare, even under a lot of
     * contention.
     *
     * The fact that retries (for both base and index lists) are
     * relatively cheap due to indexing allows some minor
     * simplifications of retry logic. Traversal restarts are
     * performed after most "helping-out" CASes. This isn't always
     * strictly necessary, but the implicit backoffs tend to help
     * reduce other downstream failed CAS's enough to outweigh restart
     * cost.  This worsens the worst case, but seems to improve even
     * highly contended cases.
     *
     * Unlike most skip-list implementations, index insertion and
     * deletion here require a separate traversal pass occuring after
     * the base-level action, to add or remove index nodes.  This adds
     * to single-threaded overhead, but improves contended
     * multithreaded performance by narrowing interference windows,
     * and allows deletion to ensure that all index nodes will be made
     * unreachable upon return from a public remove operation, thus
     * avoiding unwanted garbage retention. This is more important
     * here than in some other data structures because we cannot null
     * out node fields referencing user keys since they might still be
     * read by other ongoing traversals.
     *
     * Indexing uses skip list parameters that maintain good search
     * performance while using sparser-than-usual indices: The
     * hardwired parameters k=1, p=0.5 (see method randomLevel) mean
     * that about one-quarter of the nodes have indices. Of those that
     * do, half have one level, a quarter have two, and so on (see
     * Pugh's Skip List Cookbook, sec 3.4).  The expected total space
     * requirement for a map is slightly less than for the current
     * implementation of java.util.TreeMap.
     *
     * Changing the level of the index (i.e, the height of the
     * tree-like structure) also uses CAS. The head index has initial
     * level/height of one. Creation of an index with height greater
     * than the current level adds a level to the head index by
     * CAS'ing on a new top-most head. To maintain good performance
     * after a lot of removals, deletion methods heuristically try to
     * reduce the height if the topmost levels appear to be empty.
     * This may encounter races in which it possible (but rare) to
     * reduce and "lose" a level just as it is about to contain an
     * index (that will then never be encountered). This does no
     * structural harm, and in practice appears to be a better option
     * than allowing unrestrained growth of levels.
     *
     * The code for all this is more verbose than you'd like. Most
     * operations entail locating an element (or position to insert an
     * element). The code to do this can't be nicely factored out
     * because subsequent uses require a snapshot of predecessor
     * and/or successor and/or value fields which can't be returned
     * all at once, at least not without creating yet another object
     * to hold them -- creating such little objects is an especially
     * bad idea for basic internal search operations because it adds
     * to GC overhead.  (This is one of the few times I've wished Java
     * had macros.) Instead, some traversal code is interleaved within
     * insertion and removal operations.  The control logic to handle
     * all the retry conditions is sometimes twisty. Most search is
     * broken into 2 parts. findPredecessor() searches index nodes
     * only, returning a base-level predecessor of the key. findNode()
     * finishes out the base-level search. Even with this factoring,
     * there is a fair amount of near-duplication of code to handle
     * variants.
     *
     * For explanation of algorithms sharing at least a couple of
     * features with this one, see Mikhail Fomitchev's thesis
     * (http://www.cs.yorku.ca/~mikhail/), Keir Fraser's thesis
     * (http://www.cl.cam.ac.uk/users/kaf24/), and Hakan Sundell's
     * thesis (http://www.cs.chalmers.se/~phs/).
     *
     * Given the use of tree-like index nodes, you might wonder why
     * this doesn't use some kind of search tree instead, which would
     * support somewhat faster search operations. The reason is that
     * there are no known efficient lock-free insertion and deletion
     * algorithms for search trees. The immutability of the "down"
     * links of index nodes (as opposed to mutable "left" fields in
     * true trees) makes this tractable using only CAS operations.
     *
     * Notation guide for local variables
     * Node:         b, n, f    for  predecessor, node, successor
     * Index:        q, r, d    for index node, right, down.
     *               t          for another index node
     * Head:         h
     * Levels:       j
     * Keys:         k, key
     * Values:       v, value
     * Comparisons:  c
     */
    private static final long serialVersionUID = -8627078645895051609L;
    /**
     * Generates the initial random seed for the cheaper per-instance
     * random number generators used in randomLevel.
     */
    private static final Random seedGenerator = new Random();
    /**
     * Special value used to identify base-level header
     */
    private static final Object BASE_HEADER = new Object();
    /**
     * The topmost head index of the skiplist.
     */
    private transient volatile HeadIndex<E> head;
    /**
     * The comparator used to maintain order in this map, or null
     * if using natural ordering.
     *
     * @serial
     */
    private final Comparator<? super E> comparator;
    /**
     * Seed for simple random number generator. Not volatile since it
     * doesn't matter too much if different threads don't see updates.
     */
    private transient int randomSeed;

    /**
     * Initializes or resets state. Needed by constructors, clone,
     * clear, readObject. and ConcurrentSkipListSet.clone.
     * (Note that comparator must be separately initialized.)
     */
    final void initialize() {
        randomSeed = seedGenerator.nextInt() | 0x0100; // ensure nonzero
        head = new HeadIndex<E>(new Node<E>(null, BASE_HEADER, null),
                null, null, 1);
    }

    /**
     * compareAndSet head node
     */
    private boolean casHead(HeadIndex<E> cmp, HeadIndex<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    /* ---------------- Nodes -------------- */
    /**
     * Nodes hold keys and values, and are singly linked in sorted
     * order, possibly with some intervening marker nodes. The list is
     * headed by a dummy node accessible as head.node. The value field
     * is declared only as Object because it takes special non-V
     * values for marker and header nodes.
     */
    static final class Node<V> {
        final V key;
        volatile Object value;
        volatile Node<V> next;

        Node(V key, Node<V> next) {
            this(key, key, next);
        }

        /**
         * Creates a new regular node.
         */
        Node(V key, Object value, Node<V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        /**
         * Creates a new marker node. A marker is distinguished by
         * having its value field point to itself. Marker nodes also
         * have null keys, a fact that is exploited in a few places,
         * but this doesn't distinguish markers from the base-level
         * header node (head.node), which also has a null key.
         */
        Node(Node<V> next) {
            this.key = null;
            this.value = this;
            this.next = next;
        }

        /**
         * compareAndSet value field
         */
        boolean casValue(Object cmp, Object val) {
            return UNSAFE.compareAndSwapObject(this, valueOffset, cmp, val);
        }

        /**
         * compareAndSet next field
         */
        boolean casNext(Node<V> cmp, Node<V> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        /**
         * Returns true if this node is a marker. This method isn't
         * actually called in any current code checking for markers
         * because callers will have already read value field and need
         * to use that read (not another done here) and so directly
         * test if value points to node.
         *
         * @param n a possibly null reference to a node
         * @return true if this node is a marker node
         */
        boolean isMarker() {
            return value == this;
        }

        /**
         * Returns true if this node is the header of base-level list.
         *
         * @return true if this node is header node
         */
        boolean isBaseHeader() {
            return value == BASE_HEADER;
        }

        /**
         * Tries to append a deletion marker to this node.
         *
         * @param f the assumed current successor of this node
         * @return true if successful
         */
        boolean appendMarker(Node<V> f) {
            return casNext(f, new Node<V>(f));
        }

        /**
         * Helps out a deletion by appending marker or unlinking from
         * predecessor. This is called during traversals when value
         * field seen to be null.
         *
         * @param b predecessor
         * @param f successor
         */
        void helpDelete(Node<V> b, Node<V> f) {
            /*
             * Rechecking links and then doing only one of the
             * help-out stages per call tends to minimize CAS
             * interference among helping threads.
             */
            if (f == next && this == b.next) {
                if (f == null || f.value != f) // not already marked
                    appendMarker(f);
                else
                    b.casNext(this, f.next);
            }
        }

        /**
         * Returns value if this node contains a valid key-value pair,
         * else null.
         *
         * @return this node's value if it isn't a marker or header or
         *         is deleted, else null.
         */
        V getValidValue() {
            Object v = value;
            if (v == this || v == BASE_HEADER)
                return null;
            return (V) v;
        }
        // UNSAFE mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = UtilUnsafe.getUnsafe();
                Class k = Node.class;
                valueOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("value"));
                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ---------------- Indexing -------------- */
    /**
     * Index nodes represent the levels of the skip list. Note that
     * even though both Nodes and Indexes have forward-pointing
     * fields, they have different types and are handled in different
     * ways, that can't nicely be captured by placing field in a
     * shared abstract class.
     */
    static class Index<V> {
        final Node<V> node;
        final Index<V> down;
        volatile Index<V> right;

        /**
         * Creates index node with given values.
         */
        Index(Node<V> node, Index<V> down, Index<V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }

        /**
         * compareAndSet right field
         */
        final boolean casRight(Index<V> cmp, Index<V> val) {
            return UNSAFE.compareAndSwapObject(this, rightOffset, cmp, val);
        }

        /**
         * Returns true if the node this indexes has been deleted.
         *
         * @return true if indexed node is known to be deleted
         */
        final boolean indexesDeletedNode() {
            return node.value == null;
        }

        /**
         * Tries to CAS newSucc as successor. To minimize races with
         * unlink that may lose this index node, if the node being
         * indexed is known to be deleted, it doesn't try to link in.
         *
         * @param succ    the expected current successor
         * @param newSucc the new successor
         * @return true if successful
         */
        final boolean link(Index<V> succ, Index<V> newSucc) {
            Node<V> n = node;
            newSucc.right = succ;
            return n.value != null && casRight(succ, newSucc);
        }

        /**
         * Tries to CAS right field to skip over apparent successor
         * succ. Fails (forcing a retraversal by caller) if this node
         * is known to be deleted.
         *
         * @param succ the expected current successor
         * @return true if successful
         */
        final boolean unlink(Index<V> succ) {
            return !indexesDeletedNode() && casRight(succ, succ.right);
        }
        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long rightOffset;

        static {
            try {
                UNSAFE = UtilUnsafe.getUnsafe();
                Class k = Index.class;
                rightOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("right"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ---------------- Head nodes -------------- */
    /**
     * Nodes heading each level keep track of their level.
     */
    static final class HeadIndex<V> extends Index<V> {
        final int level;

        HeadIndex(Node<V> node, Index<V> down, Index<V> right, int level) {
            super(node, down, right);
            this.level = level;
        }
    }

    /* ---------------- Comparison utilities -------------- */
    /**
     * Represents a key with a comparator as a Comparable.
     *
     * Because most sorted collections seem to use natural ordering on
     * Comparables (Strings, Integers, etc), most internal methods are
     * geared to use them. This is generally faster than checking
     * per-comparison whether to use comparator or comparable because
     * it doesn't require a (Comparable) cast for each comparison.
     * (Optimizers can only sometimes remove such redundant checks
     * themselves.) When Comparators are used,
     * ComparableUsingComparators are created so that they act in the
     * same way as natural orderings. This penalizes use of
     * Comparators vs Comparables, which seems like the right
     * tradeoff.
     */
    static final class ComparableUsingComparator<K> implements Comparable<K> {
        final K actualKey;
        final Comparator<? super K> cmp;

        ComparableUsingComparator(K key, Comparator<? super K> cmp) {
            this.actualKey = key;
            this.cmp = cmp;
        }

        public int compareTo(K k2) {
            return cmp.compare(actualKey, k2);
        }
    }

    /**
     * If using comparator, return a ComparableUsingComparator, else
     * cast key as Comparable, which may cause ClassCastException,
     * which is propagated back to caller.
     */
    private Comparable<? super E> comparable(Object key)
            throws ClassCastException {
        if (key == null)
            throw new NullPointerException();
        if (comparator != null)
            return new ComparableUsingComparator<E>((E) key, comparator);
        else
            return (Comparable<? super E>) key;
    }

    /**
     * Compares using comparator or natural ordering. Used when the
     * ComparableUsingComparator approach doesn't apply.
     */
    int compare(E k1, E k2) throws ClassCastException {
        Comparator<? super E> cmp = comparator;
        if (cmp != null)
            return cmp.compare(k1, k2);
        else
            return ((Comparable<? super E>) k1).compareTo(k2);
    }

    /* ---------------- Traversal -------------- */
    /**
     * Returns a base-level node with key strictly less than given key,
     * or the base-level header if there is no such node. Also
     * unlinks indexes to deleted nodes found along the way. Callers
     * rely on this side-effect of clearing indices to deleted nodes.
     *
     * @param key the key
     * @return a predecessor of key
     */
    private Node<E> findPredecessor(Comparable<? super E> key) {
        if (key == null)
            throw new NullPointerException(); // don't postpone errors
        for (;;) {
            Index<E> q = head;
            Index<E> r = q.right;
            for (;;) {
                if (r != null) {
                    Node<E> n = r.node;
                    E k = n.key;
                    if (n.value == null) {
                        if (!q.unlink(r))
                            break;           // restart
                        r = q.right;         // reread r
                        continue;
                    }
                    if (key.compareTo(k) >= 0) { // skip "equal" keys
                        q = r;
                        r = r.right;
                        continue;
                    }
                }
                Index<E> d = q.down;
                if (d != null) {
                    q = d;
                    r = d.right;
                } else
                    return q.node;
            }
        }
    }

    /**
     * Returns node holding key or null if no such, clearing out any
     * deleted nodes seen along the way. Repeatedly traverses at
     * base-level looking for key starting at predecessor returned
     * from findPredecessor, processing base-level deletions as
     * encountered. Some callers rely on this side-effect of clearing
     * deleted nodes.
     *
     * Restarts occur, at traversal step centered on node n, if:
     *
     * (1) After reading n's next field, n is no longer assumed
     * predecessor b's current successor, which means that
     * we don't have a consistent 3-node snapshot and so cannot
     * unlink any subsequent deleted nodes encountered.
     *
     * (2) n's value field is null, indicating n is deleted, in
     * which case we help out an ongoing structural deletion
     * before retrying. Even though there are cases where such
     * unlinking doesn't require restart, they aren't sorted out
     * here because doing so would not usually outweigh cost of
     * restarting.
     *
     * (3) n is a marker or n's predecessor's value field is null,
     * indicating (among other possibilities) that
     * findPredecessor returned a deleted node. We can't unlink
     * the node because we don't know its predecessor, so rely
     * on another call to findPredecessor to notice and return
     * some earlier predecessor, which it will do. This check is
     * only strictly needed at beginning of loop, (and the
     * b.value check isn't strictly needed at all) but is done
     * each iteration to help avoid contention with other
     * threads by callers that will fail to be able to change
     * links, and so will retry anyway.
     *
     * The traversal loops in doPut, doRemove, and findNear all
     * include the same three kinds of checks. And specialized
     * versions appear in findFirst, and findLast and their
     * variants. They can't easily share code because each uses the
     * reads of fields held in locals occurring in the orders they
     * were performed.
     *
     * @param key the key
     * @return node holding key, or null if no such
     */
    Node<E> findNode(Comparable<? super E> key) {
        for (;;) {
            Node<E> b = findPredecessor(key);
            Node<E> n = b.next;
            for (;;) {
                if (n == null)
                    return null;
                Node<E> f = n.next;
                if (n != b.next)                // inconsistent read
                    break;
                Object v = n.value;
                if (v == null) {                // n is deleted
                    n.helpDelete(b, f);
                    break;
                }
                if (v == n || b.value == null)  // b is deleted
                    break;
                int c = key.compareTo(n.key);
                if (c == 0) {
                    if (key.equals(n.key)) // allow comapreTo equality
                        return n;
                } else if (c < 0)
                    return null;
                b = n;
                n = f;
            }
        }
    }

    /**
     * Gets value for key using findNode.
     *
     * @param okey the key
     * @return the value, or null if absent
     */
    private E doGet(Object okey) {
        Comparable<? super E> key = comparable(okey);
        /*
         * Loop needed here and elsewhere in case value field goes
         * null just as it is about to be returned, in which case we
         * lost a race with a deletion, so must retry.
         */
        for (;;) {
            Node<E> n = findNode(key);
            if (n == null)
                return null;
            Object v = n.value;
            if (v != null)
                return (E) v;
        }
    }
    /* ---------------- Insertion -------------- */

    /**
     * Main insertion method. Adds element if not present, or
     * replaces value if present and onlyIfAbsent is false.
     *
     * @param kkey         the key
     * @param value        the value that must be associated with key
     * @param onlyIfAbsent if should not insert if already present
     * @return the old value, or null if newly inserted
     */
    private void doPut(E kkey) {
        Comparable<? super E> key = comparable(kkey);
        for (;;) {
            Node<E> b = findPredecessor(key);
            Node<E> n = b.next;
            for (;;) {
                if (n != null) {
                    Node<E> f = n.next;
                    if (n != b.next)               // inconsistent read
                        break;
                    Object v = n.value;
                    if (v == null) {               // n is deleted
                        n.helpDelete(b, f);
                        break;
                    }
                    if (v == n || b.value == null) // b is deleted
                        break;
                    int c = key.compareTo(n.key);
                    if (c >= 0) {
                        b = n;
                        n = f;
                        continue;
                    }
                    // else c < 0; fall through
                }

                Node<E> z = new Node<E>(kkey, n);
                if (!b.casNext(n, z))
                    break;         // restart if lost race to append to b
                int level = randomLevel();
                if (level > 0)
                    insertIndex(z, level);
                return;
            }
        }
    }

    /**
     * Returns a random level for inserting a new node.
     * Hardwired to k=1, p=0.5, max 31 (see above and
     * Pugh's "Skip List Cookbook", sec 3.4).
     *
     * This uses the simplest of the generators described in George
     * Marsaglia's "Xorshift RNGs" paper. This is not a high-quality
     * generator but is acceptable here.
     */
    private int randomLevel() {
        int x = randomSeed;
        x ^= x << 13;
        x ^= x >>> 17;
        randomSeed = x ^= x << 5;
        if ((x & 0x80000001) != 0) // test highest and lowest bits
            return 0;
        int level = 1;
        while (((x >>>= 1) & 1) != 0)
            ++level;
        return level;
    }

    /**
     * Creates and adds index nodes for the given node.
     *
     * @param z     the node
     * @param level the level of the index
     */
    private void insertIndex(Node<E> z, int level) {
        HeadIndex<E> h = head;
        int max = h.level;

        if (level <= max) {
            Index<E> idx = null;
            for (int i = 1; i <= level; ++i)
                idx = new Index<E>(z, idx, null);
            addIndex(idx, h, level);

        } else { // Add a new level
            /*
             * To reduce interference by other threads checking for
             * empty levels in tryReduceLevel, new levels are added
             * with initialized right pointers. Which in turn requires
             * keeping levels in an array to access them while
             * creating new head index nodes from the opposite
             * direction.
             */
            level = max + 1;
            Index<E>[] idxs = (Index<E>[]) new Index[level + 1];
            Index<E> idx = null;
            for (int i = 1; i <= level; ++i)
                idxs[i] = idx = new Index<E>(z, idx, null);

            HeadIndex<E> oldh;
            int k;
            for (;;) {
                oldh = head;
                int oldLevel = oldh.level;
                if (level <= oldLevel) { // lost race to add level
                    k = level;
                    break;
                }
                HeadIndex<E> newh = oldh;
                Node<E> oldbase = oldh.node;
                for (int j = oldLevel + 1; j <= level; ++j)
                    newh = new HeadIndex<E>(oldbase, newh, idxs[j], j);
                if (casHead(oldh, newh)) {
                    k = oldLevel;
                    break;
                }
            }
            addIndex(idxs[k], oldh, k);
        }
    }

    /**
     * Adds given index nodes from given level down to 1.
     *
     * @param idx        the topmost index node being inserted
     * @param h          the value of head to use to insert. This must be
     *                   snapshotted by callers to provide correct insertion level
     * @param indexLevel the level of the index
     */
    private void addIndex(Index<E> idx, HeadIndex<E> h, int indexLevel) {
        // Track next level to insert in case of retries
        int insertionLevel = indexLevel;
        Comparable<? super E> key = comparable(idx.node.key);
        if (key == null)
            throw new NullPointerException();

        // Similar to findPredecessor, but adding index nodes along
        // path to key.
        for (;;) {
            int j = h.level;
            Index<E> q = h;
            Index<E> r = q.right;
            Index<E> t = idx;
            for (;;) {
                if (r != null) {
                    Node<E> n = r.node;
                    // compare before deletion check avoids needing recheck
                    int c = key.compareTo(n.key);
                    if (n.value == null) {
                        if (!q.unlink(r))
                            break;
                        r = q.right;
                        continue;
                    }
                    if (c > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }

                if (j == insertionLevel) {
                    // Don't insert index if node already deleted
                    if (t.indexesDeletedNode()) {
                        findNode(key); // cleans up
                        return;
                    }
                    if (!q.link(r, t))
                        break; // restart
                    if (--insertionLevel == 0) {
                        // need final deletion check before return
                        if (t.indexesDeletedNode())
                            findNode(key);
                        return;
                    }
                }

                if (--j >= insertionLevel && j < indexLevel)
                    t = t.down;
                q = q.down;
                r = q.right;
            }
        }
    }

    /* ---------------- Deletion -------------- */
    /**
     * Main deletion method. Locates node, nulls value, appends a
     * deletion marker, unlinks predecessor, removes associated index
     * nodes, and possibly reduces head index level.
     *
     * Index nodes are cleared out simply by calling findPredecessor.
     * which unlinks indexes to deleted nodes found along path to key,
     * which will include the indexes to this node. This is done
     * unconditionally. We can't check beforehand whether there are
     * index nodes because it might be the case that some or all
     * indexes hadn't been inserted yet for this node during initial
     * search for it, and we'd like to ensure lack of garbage
     * retention, so must call to be sure.
     *
     * @param okey  the key
     * @param value if non-null, the value that must be
     *              associated with key
     * @return the node, or null if not found
     */
    private E doRemove(Object okey, Object value) {
        Comparable<? super E> key = comparable(okey);
        for (;;) {
            Node<E> b = findPredecessor(key);
            Node<E> n = b.next;
            for (;;) {
                if (n == null)
                    return null;
                Node<E> f = n.next;
                if (n != b.next)                    // inconsistent read
                    break;
                Object v = n.value;
                if (v == null) {                    // n is deleted
                    n.helpDelete(b, f);
                    break;
                }
                if (v == n || b.value == null)      // b is deleted
                    break;
                int c = key.compareTo(n.key);
                if (c < 0)
                    return null;
                if (c > 0) {
                    b = n;
                    n = f;
                    continue;
                }
                if (value != null && !value.equals(v))
                    return null;
                if (!n.casValue(v, null))
                    break;
                if (!n.appendMarker(f) || !b.casNext(n, f))
                    findNode(key);                  // Retry via findNode
                else {
                    findPredecessor(key);           // Clean index
                    if (head.right == null)
                        tryReduceLevel();
                }
                return (E) v;
            }
        }
    }

    /**
     * Possibly reduce head level if it has no nodes. This method can
     * (rarely) make mistakes, in which case levels can disappear even
     * though they are about to contain index nodes. This impacts
     * performance, not correctness. To minimize mistakes as well as
     * to reduce hysteresis, the level is reduced by one only if the
     * topmost three levels look empty. Also, if the removed level
     * looks non-empty after CAS, we try to change it back quick
     * before anyone notices our mistake! (This trick works pretty
     * well because this method will practically never make mistakes
     * unless current thread stalls immediately before first CAS, in
     * which case it is very unlikely to stall again immediately
     * afterwards, so will recover.)
     *
     * We put up with all this rather than just let levels grow
     * because otherwise, even a small map that has undergone a large
     * number of insertions and removals will have a lot of levels,
     * slowing down access more than would an occasional unwanted
     * reduction.
     */
    private void tryReduceLevel() {
        HeadIndex<E> h = head;
        HeadIndex<E> d;
        HeadIndex<E> e;
        if (h.level > 3
                && (d = (HeadIndex<E>) h.down) != null
                && (e = (HeadIndex<E>) d.down) != null
                && e.right == null
                && d.right == null
                && h.right == null
                && casHead(h, d) && // try to set
                h.right != null) // recheck
            casHead(d, h);   // try to backout
    }

    /* ---------------- Finding and removing first element -------------- */
    /**
     * Specialized variant of findNode to get first valid node.
     *
     * @return first node or null if empty
     */
    Node<E> findFirst() {
        for (;;) {
            Node<E> b = head.node;
            Node<E> n = b.next;
            if (n == null)
                return null;
            if (n.value != null)
                return n;
            n.helpDelete(b, n.next);
        }
    }

    /**
     * Removes first entry; returns its snapshot.
     *
     * @return null if empty, else snapshot of first entry
     */
    E doRemoveFirst() {
        for (;;) {
            Node<E> b = head.node;
            Node<E> n = b.next;
            if (n == null)
                return null;
            Node<E> f = n.next;
            if (n != b.next)
                continue;
            Object v = n.value;
            if (v == null) {
                n.helpDelete(b, f);
                continue;
            }
            if (!n.casValue(v, null))
                continue;
            if (!n.appendMarker(f) || !b.casNext(n, f))
                findFirst(); // retry
            clearIndexToFirst();
            return n.key;
        }
    }

    /**
     * Clears out index nodes associated with deleted first entry.
     */
    private void clearIndexToFirst() {
        for (;;) {
            Index<E> q = head;
            for (;;) {
                Index<E> r = q.right;
                if (r != null && r.indexesDeletedNode() && !q.unlink(r))
                    break;
                if ((q = q.down) == null) {
                    if (head.right == null)
                        tryReduceLevel();
                    return;
                }
            }
        }
    }


    /* ---------------- Finding and removing last element -------------- */
    /**
     * Specialized version of find to get last valid node.
     *
     * @return last node or null if empty
     */
    Node<E> findLast() {
        /*
         * findPredecessor can't be used to traverse index level
         * because this doesn't use comparisons.  So traversals of
         * both levels are folded together.
         */
        Index<E> q = head;
        for (;;) {
            Index<E> d, r;
            if ((r = q.right) != null) {
                if (r.indexesDeletedNode()) {
                    q.unlink(r);
                    q = head; // restart
                } else
                    q = r;
            } else if ((d = q.down) != null) {
                q = d;
            } else {
                Node<E> b = q.node;
                Node<E> n = b.next;
                for (;;) {
                    if (n == null)
                        return b.isBaseHeader() ? null : b;
                    Node<E> f = n.next;            // inconsistent read
                    if (n != b.next)
                        break;
                    Object v = n.value;
                    if (v == null) {                 // n is deleted
                        n.helpDelete(b, f);
                        break;
                    }
                    if (v == n || b.value == null)   // b is deleted
                        break;
                    b = n;
                    n = f;
                }
                q = head; // restart
            }
        }
    }

    /**
     * Specialized variant of findPredecessor to get predecessor of last
     * valid node. Needed when removing the last entry. It is possible
     * that all successors of returned node will have been deleted upon
     * return, in which case this method can be retried.
     *
     * @return likely predecessor of last node
     */
    private Node<E> findPredecessorOfLast() {
        for (;;) {
            Index<E> q = head;
            for (;;) {
                Index<E> d, r;
                if ((r = q.right) != null) {
                    if (r.indexesDeletedNode()) {
                        q.unlink(r);
                        break;    // must restart
                    }
                    // proceed as far across as possible without overshooting
                    if (r.node.next != null) {
                        q = r;
                        continue;
                    }
                }
                if ((d = q.down) != null)
                    q = d;
                else
                    return q.node;
            }
        }
    }

    /**
     * Removes last entry; returns its snapshot.
     * Specialized variant of doRemove.
     *
     * @return null if empty, else snapshot of last entry
     */
    E doRemoveLastEntry() {
        for (;;) {
            Node<E> b = findPredecessorOfLast();
            Node<E> n = b.next;
            if (n == null) {
                if (b.isBaseHeader())               // empty
                    return null;
                else
                    continue; // all b's successors are deleted; retry
            }
            for (;;) {
                Node<E> f = n.next;
                if (n != b.next)                    // inconsistent read
                    break;
                Object v = n.value;
                if (v == null) {                    // n is deleted
                    n.helpDelete(b, f);
                    break;
                }
                if (v == n || b.value == null)      // b is deleted
                    break;
                if (f != null) {
                    b = n;
                    n = f;
                    continue;
                }
                if (!n.casValue(v, null))
                    break;
                E key = n.key;
                Comparable<? super E> ck = comparable(key);
                if (!n.appendMarker(f) || !b.casNext(n, f))
                    findNode(ck);                  // Retry via findNode
                else {
                    findPredecessor(ck);           // Clean index
                    if (head.right == null)
                        tryReduceLevel();
                }
                return key;
            }
        }
    }

    /* ---------------- Constructors -------------- */
    /**
     * Constructs a new, empty map, sorted according to the
     * {@linkplain Comparable natural ordering} of the keys.
     */
    public ConcurrentSkipListPriorityQueue() {
        this.comparator = null;
        initialize();
    }

    /**
     * Constructs a new, empty map, sorted according to the specified
     * comparator.
     *
     * @param comparator the comparator that will be used to order this map.
     *                   If <tt>null</tt>, the {@linkplain Comparable natural
     *        ordering} of the keys will be used.
     */
    public ConcurrentSkipListPriorityQueue(Comparator<? super E> comparator) {
        this.comparator = comparator;
        initialize();
    }

    public ConcurrentSkipListPriorityQueue(Collection<? extends E> m) {
        this.comparator = null;
        initialize();
        addAll(m);
    }

    /**
     * Returns a shallow copy of this <tt>ConcurrentSkipListMap</tt>
     * instance. (The keys and values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
    @Override
    public ConcurrentSkipListPriorityQueue<E> clone() {
        ConcurrentSkipListPriorityQueue<E> clone = null;
        try {
            clone = (ConcurrentSkipListPriorityQueue<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }

        clone.initialize();
        clone.buildFromSorted(this);
        return clone;
    }

    /**
     * Streamlined bulk insertion to initialize from elements of
     * given sorted map. Call only from constructor or clone
     * method.
     */
    private void buildFromSorted(ConcurrentSkipListPriorityQueue<E> pq) {
        if (pq == null)
            throw new NullPointerException();

        HeadIndex<E> h = head;
        Node<E> basepred = h.node;

        // Track the current rightmost node at each level. Uses an
        // ArrayList to avoid committing to initial or maximum level.
        ArrayList<Index<E>> preds = new ArrayList<Index<E>>();

        // initialize
        for (int i = 0; i <= h.level; ++i)
            preds.add(null);
        Index<E> q = h;
        for (int i = h.level; i > 0; --i) {
            preds.set(i, q);
            q = q.down;
        }

        Iterator<E> it = pq.iterator();
        while (it.hasNext()) {
            E k = it.next();
            int j = randomLevel();
            if (j > h.level)
                j = h.level + 1;
            if (k == null)
                throw new NullPointerException();
            Node<E> z = new Node<E>(k, null);
            basepred.next = z;
            basepred = z;
            if (j > 0) {
                Index<E> idx = null;
                for (int i = 1; i <= j; ++i) {
                    idx = new Index<E>(z, idx, null);
                    if (i > h.level)
                        h = new HeadIndex<E>(h.node, h, idx, i);

                    if (i < preds.size()) {
                        preds.get(i).right = idx;
                        preds.set(i, idx);
                    } else
                        preds.add(idx);
                }
            }
        }
        head = h;
    }

    /* ---------------- Serialization -------------- */
    /**
     * Save the state of this map to a stream.
     *
     * @serialData The key (Object) and value (Object) for each
     * key-value mapping represented by the map, followed by
     * <tt>null</tt>. The key-value mappings are emitted in key-order
     * (as determined by the Comparator, or by the keys' natural
     * ordering if no Comparator).
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        // Write out the Comparator and any hidden stuff
        s.defaultWriteObject();

        // Write out keys and values (alternating)
        for (Node<E> n = findFirst(); n != null; n = n.next) {
            E v = n.getValidValue();
            if (v != null)
                s.writeObject(n.key);
        }
        s.writeObject(null);
    }

    /**
     * Reconstitute the map from a stream.
     */
    private void readObject(final java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        // Read in the Comparator and any hidden stuff
        s.defaultReadObject();
        // Reset transients
        initialize();

        /*
         * This is nearly identical to buildFromSorted, but is
         * distinct because readObject calls can't be nicely adapted
         * as the kind of iterator needed by buildFromSorted. (They
         * can be, but doing so requires type cheats and/or creation
         * of adaptor classes.) It is simpler to just adapt the code.
         */

        HeadIndex<E> h = head;
        Node<E> basepred = h.node;
        ArrayList<Index<E>> preds = new ArrayList<Index<E>>();
        for (int i = 0; i <= h.level; ++i)
            preds.add(null);
        Index<E> q = h;
        for (int i = h.level; i > 0; --i) {
            preds.set(i, q);
            q = q.down;
        }

        for (;;) {
            Object k = s.readObject();
            if (k == null)
                break;
            E key = (E) k;
            int j = randomLevel();
            if (j > h.level)
                j = h.level + 1;
            Node<E> z = new Node<E>(key, null);
            basepred.next = z;
            basepred = z;
            if (j > 0) {
                Index<E> idx = null;
                for (int i = 1; i <= j; ++i) {
                    idx = new Index<E>(z, idx, null);
                    if (i > h.level)
                        h = new HeadIndex<E>(h.node, h, idx, i);

                    if (i < preds.size()) {
                        preds.get(i).right = idx;
                        preds.set(i, idx);
                    } else
                        preds.add(idx);
                }
            }
        }
        head = h;
    }

    /* ------ Map API methods ------ */
    @Override
    public boolean contains(Object key) {
        return doGet(key) != null;
    }

    @Override
    public boolean offer(E key) {
        if (key == null)
            throw new NullPointerException();
        doPut(key);
        return true;
    }

    @Override
    public E poll() {
        return doRemoveFirst();
    }

    @Override
    public E peek() {
        Node<E> n = findFirst();
        if (n == null)
            return null;
        return n.key;
    }

    public E peekLast() {
        Node<E> n = findLast();
        if (n == null)
            return null;
        return n.key;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iter();
    }

    @Override
    public boolean remove(Object key) {
        return doRemove(key, null) != null;
    }

    /**
     * Returns the number of key-value mappings in this map. If this map
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, it
     * returns <tt>Integer.MAX_VALUE</tt>.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these maps, determining the current
     * number of elements requires traversing them all to count them.
     * Additionally, it is possible for the size to change during
     * execution of this method, in which case the returned result
     * will be inaccurate. Thus, this method is typically not very
     * useful in concurrent applications.
     *
     * @return the number of elements in this map
     */
    @Override
    public int size() {
        long count = 0;
        for (Node<E> n = findFirst(); n != null; n = n.next) {
            if (n.getValidValue() != null)
                ++count;
        }
        return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return findFirst() == null;
    }

    /**
     * Removes all of the mappings from this map.
     */
    @Override
    public void clear() {
        initialize();
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    class Iter implements Iterator<E> {
        /**
         * the last node returned by next()
         */
        Node<E> lastReturned;
        /**
         * the next node to return from next();
         */
        Node<E> next;

        /**
         * Initializes ascending iterator for entire range.
         */
        Iter() {
            for (;;) {
                next = findFirst();
                if (next == null)
                    break;
                Object x = next.value;
                if (x != null && x != next)
                    break;
            }
        }

        @Override
        public final boolean hasNext() {
            return next != null;
        }

        @Override
        public E next() {
            Node<E> n = next;
            advance();
            return n.key;
        }

        /**
         * Advances next to higher entry.
         */
        final void advance() {
            if (next == null)
                throw new NoSuchElementException();
            lastReturned = next;
            for (;;) {
                next = next.next;
                if (next == null)
                    break;
                Object x = next.value;
                if (x != null && x != next)
                    break;
            }
        }

        @Override
        public void remove() {
            Node<E> l = lastReturned;
            if (l == null)
                throw new IllegalStateException();
            // It would not be worth all of the overhead to directly
            // unlink from here. Using remove is fast enough.
            ConcurrentSkipListPriorityQueue.this.remove(l.key);
            lastReturned = null;
        }
    }

    /*
     * View classes are static, delegating to a ConcurrentNavigableMap
     * to allow use by SubMaps, which outweighs the ugliness of
     * needing type-tests for Iterator methods.
     */
    static <E> List<E> toList(Collection<E> c) {
        // Using size() here would be a pessimization.
        List<E> list = new ArrayList<E>(c.size());
        for (E e : c)
            list.add(e);
        return list;
    }

    @Override
    public Object[] toArray() {
        return toList(this).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return toList(this).toArray(a);
    }
    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;

    static {
        try {
            UNSAFE = UtilUnsafe.getUnsafe();
            Class k = ConcurrentSkipListPriorityQueue.class;
            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
