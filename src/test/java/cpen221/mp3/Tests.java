package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.fsftbuffer.MyList;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Tests {
    /* the default buffer size is 32 objects */
    public static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    public static final int DTIMEOUT = 3600;

    FSFTBuffer<MyList> tb = new FSFTBuffer<>(2, 10);
    FSFTBuffer<MyList> t2 = new FSFTBuffer<>(1, 3);
    FSFTBuffer<MyList> t3 = new FSFTBuffer<>(2, 4);
    FSFTBuffer<MyList> t4 = new FSFTBuffer<>(4, 6);

    List<Integer> list = new ArrayList<Integer>();
    List<Integer> list2 = new ArrayList<Integer>();
    List<Integer> list23 = new ArrayList<Integer>();
    List<Integer> list1a = new ArrayList<>();
    List<Integer> list1b = new ArrayList<>();
    List<Integer> list1c = new ArrayList<>();
    List<Integer> list1d = new ArrayList<>();
    List<Integer> list1e = new ArrayList<>();
    List<Integer> list1f = new ArrayList<>();
    List<Integer> list1g = new ArrayList<>();
    List<Integer> list1h = new ArrayList<>();
    List<Integer> list1i = new ArrayList<>();
    List<Integer> list1j = new ArrayList<>();
    List<Integer> list1k = new ArrayList<>();
    List<Integer> list1l = new ArrayList<>();

    @BeforeAll
    public void initiateLists() {
        list1a.add(1);
        list1b.add(2);
        list1c.add(3);
        list1d.add(4);
        list1e.add(5);
        list1f.add(6);
        list1g.add(7);
        list1h.add(8);
        list1i.add(9);
        list1j.add(10);
        list1k.add(11);
        list1l.add(12);

    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testUpdateMethod() throws InterruptedException, IOException {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(60);
        MyList i = new MyList(list);

        tb.put(i);
        Assert.assertEquals(true, tb.update(i));
        Thread.sleep(1000);
        i.add(10);
        Thread.sleep(5000);
        Assert.assertEquals(false, tb.update(i));
        tb.get(i.id());
    }

    @Test
    public void testPut() throws InterruptedException, IOException {
        list.add(0);
        list2.add(2);
        list2.add(4);
        list23.add(5);

        MyList j = new MyList(list);
        MyList k = new MyList(list);
        MyList l = new MyList(list2);
        MyList m = new MyList(list23);
        tb.put(j);
        Thread.sleep(1000);
        Assert.assertEquals(false, tb.put(k));
        Thread.sleep(11000);
        //tests to see if old object j has timed out
        Assert.assertEquals(true, tb.put(l));
        Thread.sleep(1000);
        //should be able to add object as size up till now should only be 1
        Assert.assertEquals(true, tb.put(m));
        Thread.sleep(2000);
        Assert.assertEquals(false, tb.put(l));
        Thread.sleep(1000);
        //accesses L via get (without changing timeout time)
        Assert.assertEquals(l, tb.get(l.id()));
        Thread.sleep(1000);
        //touches M to extend its timeout time. However, M should not be accessed using a touch
        Assert.assertEquals(true, tb.touch(m.id()));
        Thread.sleep(1000);
        //Despite object M timing out later, M should be kicked from buffer as it was not accessed after creation, but L was accessed after creation
        Assert.assertEquals(true, tb.put(k));
        //wait 7 seconds later. l should have timed out
        Thread.sleep(7000);
        Assert.assertEquals(true, tb.put(l));
    }

    @Test(expected = IOException.class)
    public void testTouchAndGet() throws InterruptedException, IOException {
        //adds an empty list to the buffer
        list.clear();
        MyList l = new MyList(list);
        tb.put(l);
        Thread.sleep(3000);
        tb.touch(l.id());
        Thread.sleep(8000);
        //Ensures the object is still there 11 seconds after initially initiated, as it should be valid for 13 sec as we touched it at 3 sec.
        Assert.assertEquals(l, tb.get(l.id()));
        //Waits for the object to time out
        Thread.sleep(10000);
        //Since there is no object in the buffer left, test that get will throw an exception in this case
        tb.get(l.id());
    }

    @Test(expected = IOException.class)
    public void testUpdateChangesBothLRUAndStaleness() throws InterruptedException, IOException {
        initiateLists();
        MyList one = new MyList(list1a);
        MyList two = new MyList(list1b);
        MyList three = new MyList(list1c);

        Assert.assertEquals(true, t3.put(one));
        Thread.sleep(1000);
        Assert.assertEquals(true, t3.put(two));
        Assert.assertEquals(one, t3.get(one.id()));
        Thread.sleep(1000);
        //updates one
        t3.update(one);
        //There should still be 2 objects existing at the specified time
        //In this case, since one has been accessed more recently, kick two out of the buffer
        Thread.sleep(2000);
        Assert.assertEquals(true, t3.put(three));
        Assert.assertEquals(one, t3.get(one.id()));
        Assert.assertEquals(three, t3.get(three.id()));
        //Two should not exist, so throw IOException
        t3.get(two.id());
    }

    @Test(expected = IOException.class)
    public void testConcurrentPuts() throws InterruptedException, IOException {
        initiateLists();
        MyList l1 = new MyList(list1a);
        MyList l2 = new MyList(list1b);

        //an arraylist of myLists for the purpose of adding
        ArrayList<MyList> toAdd = new ArrayList<MyList>();
        toAdd.add(l1);
        toAdd.add(l2);

        ThreadMetho[] threads = new ThreadMetho[2];
        //creates 2 threads
        for (int i = 0; i < 2; i++) {
            threads[i] = new ThreadMetho((i * threads.length) / 2, ((i + 1) * threads.length) / 2, t2, toAdd);
            threads[i].start();
            Thread.sleep(1);
        }

        //joins threads after finishing
        for (int i = 0; i < 2; i++) {
            threads[i].join();
        }

        //Asserts that thread 2 remains but thread1 does not remain
        Assert.assertEquals(l2, t2.get(l2.id()));
        //should throw IOException
        t2.get(l1.id());

    }

    @Test(expected = IOException.class)
    public void testConcurrentPuts2() throws InterruptedException, IOException {
        initiateLists();
        MyList l1 = new MyList(list1a);
        MyList l2 = new MyList(list1b);

        //an arraylist of myLists for the purpose of adding
        ArrayList<MyList> toAdd = new ArrayList<MyList>();
        toAdd.add(l2);
        toAdd.add(l1);

        ThreadMetho[] threads = new ThreadMetho[2];
        //creates 2 threads
        for (int i = 0; i < 2; i++) {
            threads[i] = new ThreadMetho((i * threads.length) / 2, ((i + 1) * threads.length) / 2, t2, toAdd);
            threads[i].start();
            Thread.sleep(1);
        }

        //joins threads after finishing
        for (int i = 0; i < 2; i++) {
            threads[i].join();
        }

        //Asserts that thread 1 remains but thread 2 does not remain
        Assert.assertEquals(l1, t2.get(l1.id()));
        //should throw IOException
        t2.get(l2.id());

    }

    @Test
    public void testConcurrentGet() throws InterruptedException, IOException {
        initiateLists();
        MyList m1 = new MyList(list1a);
        MyList m2 = new MyList(list1b);
        MyList m3 = new MyList(list1c);
        MyList m4 = new MyList(list1d);
        MyList m5 = new MyList(list1e);
        MyList m6 = new MyList(list1f);

        ArrayList<MyList> toGet = new ArrayList<MyList>();
        toGet.add(m1);
        toGet.add(m2);
        toGet.add(m3);
        toGet.add(m4);

        ArrayList<MyList> toAdd = new ArrayList<MyList>();
        toAdd.add(m5);
        toAdd.add(m6);

        //puts 4 objects in the buffer before attempting to touch them
        Assert.assertEquals(true, t4.put(m1));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m2));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m3));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m4));
        Thread.sleep(100);

        ThreadGet[] threads = new ThreadGet[2];
        threads[0] = new ThreadGet(0, 1, t4, toGet);
        threads[1] = new ThreadGet(2, 3, t4, toGet);
        threads[0].start();
        threads[1].start();

        //joins threads after finishing
        for (int i = 0; i < 2; i++) {
            threads[i].join();
        }

        Thread.sleep(100);
        //now we are at capacity, so put in 2 more objects concurrently and ensure that old objects 1 and 3 are kicked out
        ThreadMetho[] thread = new ThreadMetho[2];
        //creates 2 threads
        for (int i = 0; i < 2; i++) {
            thread[i] = new ThreadMetho((i * thread.length) / 2, ((i + 1) * thread.length) / 2, t4, toAdd);
            thread[i].start();
            Thread.sleep(1);
        }

        //joins threads after finishing
        for (int i = 0; i < 2; i++) {
            thread[i].join();
        }

        Assert.assertEquals(m2, t4.get(m2.id()));
        Assert.assertEquals(m4, t4.get(m4.id()));
        Assert.assertEquals(m5, t4.get(m5.id()));
        Assert.assertEquals(m6, t4.get(m6.id()));
        Thread.sleep(6000); //waits 6 seconds so all objects can time out before putting objects for next test
    }

    //add further test cases for concurrent update and concurrent touch
    @Test(expected = IOException.class)
    public void testConcurrentTouch() throws InterruptedException, IOException {
        initiateLists();
        MyList m1 = new MyList(list1a);
        MyList m2 = new MyList(list1b);
        MyList m3 = new MyList(list1c);
        MyList m4 = new MyList(list1d);
        MyList m5 = new MyList(list1e);
        ArrayList<MyList> toTouch = new ArrayList<MyList>();
        toTouch.add(m1);
        toTouch.add(m2);
        toTouch.add(m3);
        toTouch.add(m4);

        //puts 4 objects in the buffer before attempting to touch them
        Assert.assertEquals(true, t4.put(m1));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m2));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m3));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m4));
        Thread.sleep(1000);

        ThreadTouch[] tch = new ThreadTouch[2];
        tch[0] = new ThreadTouch(0, 1, t4, toTouch);
        tch[1] = new ThreadTouch(2, 3, t4, toTouch);
        tch[0].start();
        tch[1].start();

        //joins threads after finishing
        for (int i = 0; i < 2; i++) {
            tch[i].join();
        }

        Thread.sleep(1000);
        //adds a new object to the buffer (should be at capacity) Buffer should kick out 1st object as LRU shouldn't be updated
        Assert.assertEquals(true, t4.put(m5));
        Thread.sleep(3000);
        //object 3 should have timed out now
        Assert.assertEquals(m2, t4.get(m2.id()));
        Assert.assertEquals(m4, t4.get(m4.id()));
        Assert.assertEquals(m5, t4.get(m5.id()));
        t4.get(m3.id()); //should throw IOException here as 3 should have timed out
        Thread.sleep(3000); //allows buffer to empty out
    }

    @Test(expected = IOException.class)
    public void testConcurrentUpdate() throws InterruptedException, IOException {
        initiateLists();
        MyList m1 = new MyList(list1a);
        MyList m2 = new MyList(list1b);
        MyList m3 = new MyList(list1c);
        MyList m4 = new MyList(list1d);
        MyList m5 = new MyList(list1e);
        MyList m6 = new MyList(list1f);
        ArrayList<MyList> toTouch = new ArrayList<MyList>();
        ArrayList<MyList> toAdd = new ArrayList<MyList>();
        toTouch.add(m1);
        toTouch.add(m2);
        toTouch.add(m3);
        toTouch.add(m4);
        toAdd.add(m5);
        toAdd.add(m6);

        //puts 4 objects in the buffer before attempting to touch them
        Assert.assertEquals(true, t4.put(m1));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m2));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m3));
        Thread.sleep(1000);
        Assert.assertEquals(true, t4.put(m4));
        Thread.sleep(1000);

        ThreadUpdate[] tch = new ThreadUpdate[2];
        tch[0] = new ThreadUpdate(0, 1, t4, toTouch);
        tch[1] = new ThreadUpdate(2, 3, t4, toTouch);
        tch[0].start();
        tch[1].start();

        //joins threads after finishing
        for (int i = 0; i < 2; i++) {
            tch[i].join();
        }

        Thread.sleep(1000);

        //puts 2 new objects in the buffer, should evict objects 1 and 3 as they were simultaneously least recently accessed
        ThreadMetho[] thread = new ThreadMetho[2];
        //creates 2 threads
        for (int i = 0; i < 2; i++) {
            thread[i] = new ThreadMetho((i * thread.length) / 2, ((i + 1) * thread.length) / 2, t4, toAdd);
            thread[i].start();
            Thread.sleep(1);
        }

        Thread.sleep(2000);
        //verify that objects 2, 4, 5 and 6 remain, and that object 3 throws an exception
        Assert.assertEquals(m2, t4.get(m2.id()));
        Assert.assertEquals(m4, t4.get(m4.id()));
        Assert.assertEquals(m5, t4.get(m5.id()));
        Assert.assertEquals(m6, t4.get(m6.id()));
        t4.get(m3.id());
    }

    @Test(expected = IOException.class)
    public void testConcurrentGetPut() throws InterruptedException, IOException {
        initiateLists();
        MyList m1 = new MyList(list1a);
        MyList m2 = new MyList(list1b);
        MyList m3 = new MyList(list1c);
        MyList m4 = new MyList(list1d);
        MyList m5 = new MyList(list1e);
        MyList m6 = new MyList(list1f);
        ArrayList<MyList> toGet = new ArrayList<MyList>();
        ArrayList<MyList> toPut = new ArrayList<MyList>();
        toPut.add(m1);
        toPut.add(m1);
        toPut.add(m3);
        toGet.add(m3);

        ThreadMetho[] thread = new ThreadMetho[2];
        //creates 2 threads
        for (int i = 0; i < 2; i++) {
            thread[i] = new ThreadMetho((i * thread.length) / 2, ((i + 1) * thread.length) / 2, t3, toPut);
            thread[i].start();
        }

        //joins threads after finishing
        for (int i = 0; i < 2; i++) {
            thread[i].join();
        }
        Thread.sleep(1000);
        Assert.assertEquals(m1, t3.get(m1.id()));

        ThreadMetho tm = new ThreadMetho(2, 3, t3, toPut);
        ThreadGet tg = new ThreadGet(0, 0, t3, toGet);

        tm.start();
        tg.start();
        tm.join();
        tg.join();

        Assert.assertEquals(m3, t3.get(m3.id()));
        //put in 4, ensures it kicks out 1
        Assert.assertEquals(true, t3.put(m4));
        t3.get(m1.id());

    }

    @Test
    public void test4Threads() throws InterruptedException {
        initiateLists();
        MyList m1 = new MyList(list1a);
        MyList m2 = new MyList(list1b);
        MyList m3 = new MyList(list1c);
        MyList m4 = new MyList(list1d);
        MyList m5 = new MyList(list1e);
        MyList m6 = new MyList(list1f);
        MyList m7 = new MyList(list1g);
        MyList m8 = new MyList(list1h);
        MyList m9 = new MyList(list1i);
        MyList m10 = new MyList(list1j);

        ArrayList<MyList> toAdd = new ArrayList<MyList>();
        toAdd.add(m1);
        toAdd.add(m2);
        toAdd.add(m3);
        toAdd.add(m4);
        toAdd.add(m5);
        toAdd.add(m6);
        toAdd.add(m7);
        toAdd.add(m8);
        toAdd.add(m9);
        toAdd.add(m10);

        ThreadMetho[] threads = new ThreadMetho[10];
        for (int i = 0; i < 4; i++) {
            threads[i] = new ThreadMetho((i * threads.length) / 10, ((i + 1) * threads.length) / 10, t2, toAdd);
            threads[i].start();
            Thread.sleep(1);
        }

        //joins threads after finishing
        for (int i = 0; i < 4; i++) {
            threads[i].join();
        }

        Assert.assertEquals(false, t2.put(m4));

    }

}
