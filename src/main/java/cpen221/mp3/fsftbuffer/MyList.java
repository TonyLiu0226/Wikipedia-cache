package cpen221.mp3.fsftbuffer;

import java.util.ArrayList;
import java.util.List;

public class MyList implements Bufferable {
    private List<Integer> myList = new ArrayList<>();
    private String id;
    public MyList (List<Integer> list){
        myList.addAll(list);
        this.id = this.myList.toString();
    }

    public void add(int i){
        myList.add(i);
        this.id = this.myList.toString();
    }

    @Override
    public String id() {
        return this.id;
    }
}
