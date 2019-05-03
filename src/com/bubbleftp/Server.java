package com.bubbleftp;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {
    public static final int PORT = 2121;
    private static final int NOF_CONCURRENT_CONNECTIONS = 2;

    private ExecutorService threadpool;
    private ServerSocket server;

    public Server() throws IOException {
        server = new ServerSocket(PORT);
        threadpool = Executors.newFixedThreadPool(NOF_CONCURRENT_CONNECTIONS);

        installSignalHandlers();
    }

    public void run() {
        for (; ; ) {
            try {
                threadpool.execute(new ClientHandler(server.accept()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void installSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownAndAwaitTermination();
            }
        });
    }

    private void shutdownAndAwaitTermination() {
        System.out.println("Shutting down");
        threadpool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!threadpool.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("Connections in progress, waiting for them to finish, 60sec timeout");

                threadpool.shutdownNow(); // Cancel currently executing tasks

                // Wait a while for tasks to respond to being cancelled
                if (!threadpool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();

            threadpool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String args[]) throws IOException {
        Server s = new Server();
        new Thread(s).run();
    }
}
