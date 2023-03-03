package cpen221.mp3.wikimediator;

import cpen221.mp3.fsftbuffer.Bufferable;
import cpen221.mp3.fsftbuffer.FSFTBuffer;

import org.fastily.jwiki.core.Wiki;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class WikiMediator {

    private FSFTBuffer<Bufferable> cache;
    private Map<String, Integer> itemsFromSearchAndGet = new HashMap<>();
    private List<String> items = new ArrayList<>();
    private Map<Integer, String> logTitle = new TreeMap<>();
    private Map<Integer, String> logOp = new TreeMap<>();
    private final int initTime = (int) System.currentTimeMillis() / 1000;
    private int currentTime;
    private final Wiki wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();

    /*
    RI:
    - cache: only contains page titles requested from getPage method
    - logTitle: maps the point in time, where a request is made, to the page title associated with that request
                only contains page titles requested from getPage and search method

    - logOp:
       maps the point in time, where a request is made, to the operation associated with that request
       only contains operations requested by users,
       self call within this class will not be counted

    - currentTime: >= 0
    - itemsFromSearchAndGet only contains page titles requested from search and getPage method,

    AF: represent a mediator service for Wikipedia
     */

    /**
     * WikiMediator Constructor
     *
     * @param capacity          the cache capacity
     * @param stalenessInterval number of SECONDS after which a page become stale in the cache
     */
    public WikiMediator(int capacity, int stalenessInterval) {
        this.cache = new FSFTBuffer<>(capacity, stalenessInterval);
        this.currentTime = 0;
    }

    /*
        You must implement the methods with the exact signatures
        as provided in the statement for this mini-project.

        You must add method signatures even for the methods that you
        do not plan to implement. You should provide skeleton implementation
        for those methods, and the skeleton implementation could return
        values like null.
     */

    /**
     * Put/update the page title requested from search and getPage operation
     *
     * @param list a list whose elements needed to add
     */
    private void addItem(List<String> list) {
        for (String pageTittle : list) {
            if (!(itemsFromSearchAndGet.containsKey(pageTittle))) {
                itemsFromSearchAndGet.put(pageTittle, 1);
                items.add(pageTittle);
            } else {
                int occurrence = itemsFromSearchAndGet.get(pageTittle);
                itemsFromSearchAndGet.replace(pageTittle, ++occurrence);
            }
        }
    }

    /**
     * Get a list of pages from Wiki whose page title matched with the provided keyword
     *
     * @param query a keyword string to search the Wiki with
     * @param limit maximum number of entries matched with query to be returned,
     *              a whole number >= 0
     * @return a list of entries whose titles matched with query, empty if no pages matched
     * an empty list will be returned if no titles matched the key word
     * ex: query: "Barack Obama", limit: 3
     * return: [Barack Obama, Barack Obama in Comics, Barack Obama Sr.]
     */
    public List<String> search(String query, int limit) {
        List<String> pagesList = new ArrayList<>(wiki.search(query, limit));
        for (String page : pagesList) {
            updateLog(page, "search");
        }
        addItem(pagesList);
        return pagesList;
    }

    /**
     * Get the text on Wiki associated with a given page title
     *
     * @param pageTitle title of the page requested by the users
     * @return the text associated with the given page title
     * returns an empty string if there's no such page on Wikipedia
     */
    public String getPage(String pageTitle) {

        long start = System.nanoTime();
        String result = "";
        List<String> pageList = new ArrayList<>(wiki.search(pageTitle, 1)); //search page title
        //if no pages can be found, return the empty string
        if (pageList.size() == 0) {
            return result;
        }

        Bufferable page = () -> pageList.get(0); //cast "page" to Bufferable type

        if (cache.put(page)) {

            updateLog(page.id(), "get");
            addItem(pageList);
            result = wiki.getPageText(page.id());
        } else {
            try {
                Bufferable pageInCache = cache.get(pageTitle);
                updateLog(pageInCache.id(), "get");
                addItem(List.of(pageInCache.id()));
                result = wiki.getPageText(pageInCache.id());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * @param zeit unsorted list
     *             effects: sort the provided list in non-ascending order
     */
    private void ranking(List<String> zeit, Map<String, Integer> itemMap) {
        for (int id = 0; id < zeit.size(); id++) {
            int smallest = itemMap.get(zeit.get(id));
            int smallestId = id;

            for (int nxt = id + 1; nxt < zeit.size(); nxt++) {
                int current = itemMap.get(zeit.get(nxt));

                if (current < smallest) {
                    smallest = current;
                    smallestId = nxt;
                }
            }

            if (smallestId != id) {
                String tmp = zeit.get(smallestId);
                zeit.set(smallestId, zeit.get(id));
                zeit.set(id, tmp);
            }
        }
    }

    /**
     * @param limit number of items to be returned if there are many requests,
     *              a whole number > 0
     * @return a list of most common used page titles in search and getPage method, sorted in non-ascending order
     */
    public List<String> zeitgeist(int limit) {
        updateLog("", "zeit");
        List<String> itemsList = new ArrayList<>(items);
        ranking(itemsList, itemsFromSearchAndGet);
        List<String> zeit = new ArrayList<>();
        if (itemsList.size() > limit) {
            for (int i = itemsList.size() - limit; zeit.size() < limit; i++) {
                zeit.add(itemsList.get(i));
            }
        } else {
            zeit.addAll(itemsList);
        }
        return zeit;
    }

    /**
     * update current time of the mediator
     */
    private void updateTime() {
        this.currentTime = (int) System.currentTimeMillis() / 1000 - initTime;
    }

    /**
     * Log in new data whenever users perform an operation
     *
     * @param title  page title, provide empty string if there is no page title associated with the operation
     * @param opName name of the performed operations: search, get, zeit, trending, peak30s and shortestPath
     *               <p>
     *               effects: update logOp and logTitle
     */
    private void updateLog(String title, String opName) {
        updateTime();
        logOp.put(this.currentTime, opName);
        if (!(title.equals(""))) {
            logTitle.put(this.currentTime, title);
        }
    }

    /**
     * @param timeLimitInSeconds the time window to evaluate zeit method
     * @param maxItems           maximum number of items to be returned if there are too many requests within the given time window,
     *                           must be a whole number, >= to 0.
     * @return a list of most common used page titles in search and getPage method in the given time window
     * , sorted in non-ascending order
     */
    public List<String> trending(int timeLimitInSeconds, int maxItems) {

        List<String> itemList = new ArrayList<>();
        Map<String, Integer> itemMap = new HashMap<>();
        for (HashMap.Entry<Integer, String> e : this.logTitle.entrySet()) {
            long logTime = e.getKey();
            String title = e.getValue();
            if (logTime <= currentTime && logTime >= currentTime - timeLimitInSeconds) {
                if (itemMap.containsKey(title)) {
                    int occurrence = itemMap.get(title);
                    itemMap.replace(title, ++occurrence);
                } else {
                    itemMap.put(title, 1);
                    itemList.add(title);
                }
            }
        }
        ranking(itemList, itemMap);

        List<String> resultList = new ArrayList<>();
        if (itemList.size() > maxItems) {
            for (int i = itemList.size() - maxItems; resultList.size() < maxItems; i++) {
                resultList.add(itemList.get(i));
            }
        } else {
            resultList.addAll(itemList);
        }
        updateLog("", "trending");
        return resultList;
    }

    /**
     * @param start the beginning of a time window
     * @param end   the end of a time window
     * @return total number of requests made within a time window,
     * including requests made at the end of the window
     */
    private int computeRequest(long start, long end) {
        int sum = 0;
        for (HashMap.Entry<Integer, String> e : this.logOp.entrySet()) {
            long logTime = e.getKey();
            if (logTime >= start && logTime <= end) {
                sum++;
            }
        }
        return sum;
    }

    /**
     * @param timeWindowInSeconds the length for each time window,
     *                            whole number >= 0
     * @return the largest number of requests made in any given time window
     */
    public int windowedPeakLoad(int timeWindowInSeconds) {
        updateTime();
        int largestSum = 0;
        //compute number of requests for every time window of length "timeWindowInSeconds"
        for (HashMap.Entry<Integer, String> e : this.logOp.entrySet()) {
            long start = e.getKey();
            int sumInTimeWindow = computeRequest(start, start + timeWindowInSeconds);
            if (sumInTimeWindow > largestSum) {
                largestSum = sumInTimeWindow;
            }
        }
        return largestSum;
    }

    /**
     * Compute the largest number of requests made every 30s, including itself
     *
     * @return the largest number of requests made in every 30s, including itself
     */
    public int windowedPeakLoad() {
        updateLog("", "peak30s");
        return windowedPeakLoad(30);
    }

    /**
     * Returns the minimum number of links between 2 given page titles
     *
     * @param pageTitle1 title of the starting page
     * @param pageTitle2 title of the destination page
     * @param timeout the duration after which an exception is thrown, in seconds
     * @return a list of links needed to go from the starting page to the destination page
     * @throws TimeoutException when timeout is reached
     */
    List<String> shortestPath(String pageTitle1, String pageTitle2, int timeout) throws TimeoutException {
        throw new TimeoutException();
    }
}
