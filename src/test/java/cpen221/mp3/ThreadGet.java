package cpen221.mp3;

import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.fsftbuffer.MyList;

import java.io.IOException;
import java.util.ArrayList;

//method for creating and testing multiple threads in the testcase
public class ThreadGet extends java.lang.Thread {
    int l;
    int h;
    FSFTBuffer f;
    ArrayList<MyList> a;

    ThreadGet(int l, int h, FSFTBuffer f, ArrayList<MyList> a) {
        this.l = l;
        this.h = h;
        this.f = f;
        this.a = a;
    }

    //run method
    public void run() {
        for (int i = l; i < h + 1; i++) {

            try {
                f.get(a.get(i).id());
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
