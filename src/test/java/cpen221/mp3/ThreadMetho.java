package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.fsftbuffer.MyList;

import java.util.ArrayList;

//method for creating and testing multiple threads in the testcase
public class ThreadMetho extends java.lang.Thread {
    int l;
    int h;
    FSFTBuffer f;
    ArrayList<MyList> a;

    ThreadMetho(int l, int h, FSFTBuffer f, ArrayList<MyList> a) {
        this.l = l;
        this.h = h;
        this.f = f;
        this.a = a;
    }

    //run method
    public void run() {
        for (int i = l; i < h; i++) {
            f.put(a.get(i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
