package cpen221.mp3.server;

import cpen221.mp3.wikimediator.WikiMediator;

public class WikiMediatorServer {
    private final int port;
    private final int n;
    private final WikiMediator wm;

    /**
     * Start a server at a given port number, with the ability to process
     * upto n requests concurrently.
     *
     * @param port         the port number to bind the server to, 9000 <= {@code port} <= 9999
     * @param n            the number of concurrent requests the server can handle, 0 < {@code n} <= 32
     * @param wikiMediator the WikiMediator instance to use for the server, {@code wikiMediator} is not {@code null}
     */
    public WikiMediatorServer(int port, int n, WikiMediator wikiMediator) {
        this.port = port;
        this.n = n;
        this.wm = wikiMediator;
    }

}
