package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.fastily.jwiki.core.Wiki;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;


public class WikiTests {

    private final int capacity = 24;
    private final int stalenessInterval = 120;
    private final Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();


    @Test
    public void testSearchAndGetFor1Page() {
        WikiMediator wm = new WikiMediator(capacity, stalenessInterval);
        wm.search("Elephant", 1);
        Assertions.assertEquals(wiki.getPageText("Elephant"), wm.getPage("Elephant"));
    }

    @Test
    public void testSearchAndGetForNoResults() {
        WikiMediator wm = new WikiMediator(capacity, stalenessInterval);
        wm.search("ernsoithudtiukhdrty", 5);
        Assertions.assertEquals("", wm.getPage("ernsoithudtiukhdrty"));

    }

    @Test
    public void testSearchAndGetFor5Pages() {
        WikiMediator wm = new WikiMediator(capacity, stalenessInterval);
        wm.search("Barack Obama", 5);
        Assertions.assertEquals(wiki.getPageText("Speeches of Barack Obama"), wm.getPage("Speeches of Barack Obama"));
    }

    @Test
    public void testZeit() throws InterruptedException {
        WikiMediator wm = new WikiMediator(capacity, stalenessInterval);

        wm.getPage("Hello");
        Thread.sleep(1000);
        wm.getPage("Bye");
        Thread.sleep(2000);
        wm.getPage("Bye");
        Thread.sleep(1000);
        wm.getPage("GoodBye");
        Thread.sleep(2000);
        wm.getPage("Hello");
        wm.getPage("Bye");
        Thread.sleep(1000);

        Assertions.assertEquals(List.of("Goodbye", "Hello", "Bye"), wm.zeitgeist(10));
        Assertions.assertEquals(List.of("Goodbye", "Hello", "Bye"), wm.zeitgeist(3));
        Assertions.assertEquals(List.of("Hello", "Bye"), wm.zeitgeist(2));
        Assertions.assertEquals(List.of("Bye"), wm.zeitgeist(1));
        Assertions.assertEquals(new ArrayList<>(), wm.zeitgeist(0));
    }

    @Test
    public void TestTrending() throws InterruptedException {
        WikiMediator wm = new WikiMediator(capacity, stalenessInterval);

        wm.search("Hello", 1);
        Thread.sleep(5000);
        wm.getPage("Hello");
        Thread.sleep(5000);

        wm.search("Bye", 1);
        Thread.sleep(2000);
        wm.getPage("Bye");
        Thread.sleep(2000);
        wm.getPage("Hello");
        Thread.sleep(2000);
        wm.getPage("GoodBye");
        Thread.sleep(1000);
        wm.search("GoodBye", 1);
        Thread.sleep(3000);
        wm.getPage("Bye");

        //test for when limit > number of titles
        Assertions.assertEquals(List.of("Hello", "Goodbye", "Bye"), wm.trending(14, 4));
        //tests for top 3 items
        Assertions.assertEquals(List.of("Hello", "Goodbye", "Bye"), wm.trending(14, 3));
        //tests for top 2 items
        Assertions.assertEquals(List.of("Goodbye", "Bye"), wm.trending(14, 2));
        //tests for no items
        Assertions.assertEquals(List.of(), wm.trending(14, 0));
    }

    @Test
    public void TestPeakLoad() throws InterruptedException {
        WikiMediator wm = new WikiMediator(capacity, stalenessInterval);

        wm.search("Hello", 1);
        Thread.sleep(5000);
        wm.getPage("Hello");
        Thread.sleep(5000);

        wm.search("Bye", 1);
        Thread.sleep(2000);
        wm.getPage("Bye");
        Thread.sleep(2000);
        wm.getPage("Hello");
        Thread.sleep(2000);
        wm.getPage("GoodBye");
        Thread.sleep(1000);
        wm.search("GoodBye", 1);

        Thread.sleep(3000);
        wm.getPage("Bye");

        Assertions.assertEquals(5, wm.windowedPeakLoad(10));
        Assertions.assertEquals(8, wm.windowedPeakLoad());
    }
}
