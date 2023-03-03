package cpen221.mp3.fsftbuffer;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.concurrent.ConcurrentHashMap;


public class FSFTBuffer<T extends Bufferable> {

    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    //fields for capacity and timeout thresholds of the object
    private final int timeout;
    private final int capacity;

    //field to keep track of time
    private volatile int time;

    //map to store objects in the buffer
    private ConcurrentHashMap<String, T> storedObjects = new ConcurrentHashMap<>();

    //map to keep track of timeout time for each object
    private ConcurrentHashMap<String, Integer> timings = new ConcurrentHashMap<>();

    //map to keep track of access time for each object
    private ConcurrentHashMap<String, Integer> accessed = new ConcurrentHashMap<>();


    /**
     * RI:
     * - CANNOT HAVE MORE THAN CAPACITY NUMBER OF OBJECTS IN THE BUFFER AT ANY GIVEN TIME
     * - OBJECTS THAT ARE IN THE FSFTBUFFER CANNOT HAVE A TIMEOUT TIME OLDER THAN THE CURRENT TIME
     * - ALL OBJECTS IN THE FSFTBUFFER MUST BE OF TYPE T
     * - The keys in the storedObjects map strictly matches with the keys in the timings map and the keys in the accessed map
     * - The size of the storedObjects map is equal to the size of the timings map and the size of the accessed map
     */

    private void checkRep() {
        //checks that the buffer does not contain more than capacity number of objects
        assert (this.accessed.size() <= this.capacity &&
                this.timings.size() <= this.capacity &&
                this.storedObjects.size() <= this.capacity);

        //checks that the size of each of the three hashmaps is equal
        assert (this.accessed.size() == this.timings.size() &&
                this.timings.size() == this.storedObjects.size() &&
                this.storedObjects.size() == this.accessed.size());

        //checks that all three hashmaps have strictly the same keys
        for (ConcurrentHashMap.Entry<String, T> g : this.storedObjects.entrySet()) {
            String k = g.getKey();
            assert (this.accessed.containsKey(k));
            assert (this.storedObjects.containsKey(k));
        }

        //checks that all elements have valid timeout times
        for (ConcurrentHashMap.Entry<String, Integer> i : this.timings.entrySet()) {
            String k = i.getKey();
            assert (this.timings.get(k) >= this.time);
        }

    }

    /**AF:
     * Represents a cache that is capable of storing objects for a specified period of time with a collection of 3 hashmaps
     * - One hashmap keeps track of the objects in the cache, with the keys being the object IDs and values being the object the IDs point to
     * - Another hashmap keeps track of the timeout/staleness time for the objects, with the keys being object IDs and values being time
     * that the object corresponding to that ID will become "stale"
     * - Another hashmap keeps track of the access time for the objects, with the keys being object IDs and values being access times.
     * The access times are the times when the object that points to the object ID was last used.
     */

    /**THREAD SAFETY CONDITION:
     * All public methods are synchronized. The field time is volatile.
     * All hashmaps used are of a thread-safe data type
     * The capacity and timeout fields are immutable and final
     */

    /**
     * Create a buffer with a fixed capacity and a timeout value.
     * Objects in the buffer that have not been refreshed within the
     * timeout period are removed from the cache.
     *
     * @param capacity the number of objects the buffer can hold
     * @param timeout  the duration, in seconds, an object should
     *                 be in the buffer before it times out
     */
    public FSFTBuffer(int capacity, int timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
        getCurrentTime();
        checkRep();
    }

    /**
     * Create a buffer with default capacity and timeout values.
     */
    public FSFTBuffer() {
        this(DSIZE, DTIMEOUT);
        checkRep();
    }

    /**
     * Updates the time when an object will time out
     */
    private synchronized void updateTime(int t, String id) {
        t += this.timeout;
        //updates timing map's value for the object with the string id to now timeout at the new time
        this.timings.replace(id, t);
        getCurrentTime();
    }

    //writes to time
    private synchronized void getCurrentTime() {
        this.time = (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * effects: Removes all objects that have timed out every time this is called
     */

    private synchronized void removeStaleObjects() {

        ArrayList<String> toRemove = new ArrayList<>();
        getCurrentTime(); //update buffer's time
        for (ConcurrentHashMap.Entry<String, Integer> e : this.timings.entrySet()) {
            //if value of entry is less or equal to the current time, then flag for removal by adding to toRemove list
            String k = e.getKey();

            if (e.getValue() <= this.time) {

                toRemove.add(k);
            }
        }
        //removes entries that were flagged for removal
        for (String s : toRemove) {
            this.timings.remove(s);
            this.storedObjects.remove(s);
            this.accessed.remove(s);
        }

        getCurrentTime();
    }

    /**
     * Add a value to the buffer.
     * If the buffer is full then remove the least recently accessed
     * object to make room for the new object.
     * NOTE: REPEATED PUTS WILL UPDATE BOTH THE ACCESSED TIME AND THE TIMEOUT TIME, even though result is false
     *
     * @return false if the object is already in the buffer, otherwise true
     */

    public synchronized boolean put(T t) {
        checkRep();
        removeStaleObjects();
        //first check if there is already a similar object in the buffer. If it exists, then we won't put object in and will return false
        if (this.touch(t.id()) && this.update(t)) {
            return false;
        }
        int k = this.storedObjects.size();

        if (k < this.capacity) {
            this.storedObjects.put(t.id(), t); //to replace 0 with time
            //associates a timeout time to the same object
            getCurrentTime();
            int it = this.time;//object initial time
            this.timings.put(t.id(), it + timeout); //store object's timeout
            //updates accessed hashmap to ensure object is also accessed upon put
            this.accessed.put(t.id(), it);
        } else {
            removeLRUObject();
            this.storedObjects.put(t.id(), t); //to replace 0 with time
            //associates a timeout time to the same object
            getCurrentTime();
            int it = this.time;//object initial time
            this.timings.put(t.id(), it + timeout); //store object's timeout
            //updates accessed hashmap to ensure object is also accessed upon put
            this.accessed.put(t.id(), it);
        }
        checkSize();
        checkRep();

        if (storedObjects.containsKey(t.id())) return true;
        else return false;
    }

    /**
     * checks the size of the FSFTBuffer to ensure that it is no larger than the capacity
     * <p>
     * effect: If the size is larger than the capacity, then remove the least recently used object
     */
    private void checkSize() {
        //if size has been exceeded, remove LRUs until we get back to capacity
        while (this.storedObjects.size() > this.capacity) {
            removeLRUObject();
        }
    }

    /**
     * effect: Removes the least recently used object from the FSFTBuffer
     */
    private void removeLRUObject() {
        int lowest = 2147483647;
        String lowestId = "";

        for (HashMap.Entry<String, Integer> x : this.accessed.entrySet()) {
            int x_time = x.getValue();

            if (x_time < lowest) {
                lowest = x_time;
                lowestId = x.getKey();

            }
        }

        this.timings.remove(lowestId);
        this.storedObjects.remove(lowestId);
        this.accessed.remove(lowestId);
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the
     * buffer
     * @throws IOException if no objects with the id exist inside the FSFTBuffer
     */
    public synchronized T get(String id) throws IOException {

        checkRep();
        //first removes any stale objects from the buffers and checks for mutation
        removeStaleObjects();

        //if we can find the object in the map, then we update the value to time t
        if (this.storedObjects.containsKey(id)) {
            getCurrentTime();

            //code to update accessed
            int t = this.time;
            this.accessed.replace(id, t);

            checkRep();
            return storedObjects.get(id);
        }

        //if we cannot find the object in the map, then we need to throw a checked IOException
        else {
            checkRep();
            throw new IOException();
        }
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its
     * timeout is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public synchronized boolean touch(String id) {
        checkRep();
        removeStaleObjects();
        getCurrentTime();
        int t = this.time;

        if (this.storedObjects.containsKey(id)) {
            updateTime(t, id);
            checkRep();
            return true;
        }
        checkRep();
        return false;
    }

    /**
     * Update an object in the buffer.
     * This method updates an object and acts like a "touch" to
     * renew the object in the cache.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     * effect: IF TRUE, TREATS AN UPDATE OPERATION AS A GET + TOUCH AND UPDATES THE OBJECT'S ACCESSED TIME AND THE TIMEOUT TIME
     * IF FALSE: REMOVES THE OBJECT AND PUTS IN A NEW OBJECT MATCHING THE NEW DESCRIPTION, UPDATING THE ACCESSED AND TIMEOUT TIME
     */

    public synchronized boolean update(T t) {
        checkRep();
        getCurrentTime();
        boolean notUpdated = true;

        for (HashMap.Entry<String, T> key : this.storedObjects.entrySet()) {
            if (key.getValue().equals(t)) {
                if (!(key.getKey().equals(t.id()))) {

                    storedObjects.remove(key.getKey());
                    timings.remove(key.getKey());
                    accessed.remove(key.getKey());
                    this.put(t);

                    notUpdated = false;
                    break;
                }
            }
        }
        if (notUpdated) {
            int tl = this.time;
            updateTime(tl, t.id());
            this.accessed.put(t.id(), tl);
            checkRep();
            return true;
        } else {
            checkRep();
            return false;
        }
    }
}