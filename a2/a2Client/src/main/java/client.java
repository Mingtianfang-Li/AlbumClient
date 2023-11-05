import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class client {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final int threadGroupSize = 10;
    private static final int numThreadGroups = 10;
    private static final int delay = 2; // in seconds
    private static final String url = "http://34.222.153.0:8080/AlbumServlet_war/Albums/";

    private static final int totalRequests = 100;
    private static HttpRequest request1;
    private static HttpRequest request2;
    private static String jsonData;

    public static void main(String[] args) throws InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(threadGroupSize);
        ExecutorService executorService = Executors.newFixedThreadPool(threadGroupSize);

        request1 = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("User-Agent", "Java 11 HttpClient Bot")
                .build();

        jsonData = "{\"albumID\":\"123\", \"imageSize\":\"321\"}";

        request2 = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .uri(URI.create(url))
                .header("User-Agent", "Java 11 HttpClient Bot")
                .header("Content-Type", "application/json")
                .build();

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numThreadGroups; i++) {
            // Start a thread group
            for (int j = 0; j < threadGroupSize; j++) {
                executorService.execute(() -> {
                    sendRequests(totalRequests, barrier);
                });
            }

            // Wait for delay seconds
            Thread.sleep(delay * 1000);
        }

        long endTime = System.currentTimeMillis();

        // Calculate wall time in seconds
        double wallTime = (endTime - startTime) / 1000.0;
        System.out.println("Wall Time: " + wallTime + " seconds");

        // Calculate throughput
        int totalRequestsSent = numThreadGroups * threadGroupSize * totalRequests * 2;
        double throughput = totalRequestsSent / wallTime;
        System.out.println("Throughput: " + throughput + " requests/second");

        executorService.shutdown();
    }

    private static void sendRequests(int numRequests, CyclicBarrier barrier) {
        for (int i = 0; i < numRequests; i++) {
            CompletableFuture<HttpResponse<String>> response1 = httpClient.sendAsync(request1, HttpResponse.BodyHandlers.ofString());
            CompletableFuture<HttpResponse<String>> response2 = httpClient.sendAsync(request2, HttpResponse.BodyHandlers.ofString());

            try {
                response1.get();
                response2.get();
                barrier.await(); // Wait for all threads to reach the barrier
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
