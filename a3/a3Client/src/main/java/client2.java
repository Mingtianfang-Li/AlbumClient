import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.*;

public class client2 {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final int threadGroupSize = 10;
    private static final int numThreadGroups = 30;
    private static final int delay = 2; // in seconds
    private static final String url = "http://18.246.212.185:8080//AlbumServlet_war/Albums/";

    private static final int totalRequests = 100;
    private static HttpRequest request1;
    private static HttpRequest request2;
    private static String jsonData;
    private static int successfulRequests = 0;
    private static int failedRequests = 0;
    private static List<Long> responseTimes = new ArrayList<>();
    public static void main(String[] args) throws InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(threadGroupSize);
        ExecutorService executorService = Executors.newFixedThreadPool(threadGroupSize);

        request1 = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url + "/123"))
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
                    int[] results = sendRequests(totalRequests, barrier);
                    successfulRequests += results[0];
                    failedRequests += results[1];
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
        System.out.println("Successful Requests: " + (totalRequestsSent - failedRequests));
        System.out.println("Failed Requests: " + failedRequests);
        calculateAndDisplayStatistics(responseTimes);
        executorService.shutdown();
    }

    private static int[] sendRequests(int numRequests, CyclicBarrier barrier) {
        int successfulRequestsCount = 0;
        int failedRequestsCount = 0;
        for (int i = 0; i < numRequests; i++) {
            long startTime = System.currentTimeMillis();

            CompletableFuture<HttpResponse<String>> response1 = httpClient.sendAsync(request1, HttpResponse.BodyHandlers.ofString());
            CompletableFuture<HttpResponse<String>> response2 = httpClient.sendAsync(request2, HttpResponse.BodyHandlers.ofString());

            try {
                response2.get();
                successfulRequestsCount++; // GET request successful
                barrier.await(); // Wait for all threads to reach the barrier
            } catch (Exception e) {
                e.printStackTrace();
                failedRequestsCount++; // GET request failed
            }

            try {
                response1.get();
                successfulRequestsCount++; // POST request successful
                barrier.await(); // Wait for all threads to reach the barrier
            } catch (Exception e) {
                e.printStackTrace();
                failedRequestsCount++; // POST request failed
            }

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;
            responseTimes.add(responseTime);
        }
        return new int[]{successfulRequestsCount, failedRequestsCount};
    }

    private static void calculateAndDisplayStatistics(List<Long> responseTimes) {
        System.out.println("Statistics for Response Times:");
        // Calculate mean response time
        long sumResponseTimes = 0;
        for (Long time : responseTimes) {
            sumResponseTimes += time;
        }
        double meanResponseTime = (double) sumResponseTimes / responseTimes.size();
        System.out.println("Mean Response Time: " + meanResponseTime + " milliseconds");

        // Calculate median response time
        Collections.sort(responseTimes);
        double medianResponseTime;
        if (responseTimes.size() % 2 == 0) {
            medianResponseTime = (responseTimes.get(responseTimes.size() / 2) + responseTimes.get(responseTimes.size() / 2 - 1)) / 2.0;
        } else {
            medianResponseTime = responseTimes.get(responseTimes.size() / 2);
        }
        System.out.println("Median Response Time: " + medianResponseTime + " milliseconds");

        // Calculate p99 (99th percentile) response time
        int p99Index = (int) Math.ceil(0.99 * responseTimes.size());
        long p99ResponseTime = responseTimes.get(p99Index);
        System.out.println("P99 Response Time: " + p99ResponseTime + " milliseconds");

        // Calculate min and max response time
        long minResponseTime = responseTimes.get(0);
        long maxResponseTime = responseTimes.get(responseTimes.size() - 1);
        System.out.println("Min Response Time: " + minResponseTime + " milliseconds");
        System.out.println("Max Response Time: " + maxResponseTime + " milliseconds");
    }
}
