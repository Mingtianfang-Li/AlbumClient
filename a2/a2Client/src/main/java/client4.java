package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class client4 {

    private static final int threadGroupSize = 10;
    private static final int numThreadGroups = 10;
    private static final int delay = 2;
    private static final String SERVER_URL = "http://18.246.212.185:8080//AlbumServlet_war/Albums/";

    // Variables to store latency and response data
    private static CopyOnWriteArrayList<Long> latencies = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<ResponseData> responseDataList = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        long overallStartTime = System.currentTimeMillis(); // Record the start time

        for (int group = 0; group < numThreadGroups; group++) {
            long startTime = System.currentTimeMillis(); // Record the start time for this group

            // Create a thread pool for this group with the specified number of threads.
            final CountDownLatch completed = new CountDownLatch(threadGroupSize);

            for (int i = 0; i < threadGroupSize; i++) {
                // Create and start a thread for each request
                Thread thread = new Thread(new HttpRequestTask(completed));
                thread.start();
            }

            completed.await();
            long endTime = System.currentTimeMillis(); // Record the end time for this group

            long groupExecutionTime = endTime - startTime;
            System.out.println("Thread group " + (group + 1) + " completed in " + groupExecutionTime + " milliseconds");

            // Sleep for the specified delay before starting the next group
            Thread.sleep(delay * 1000);
        }

        long overallEndTime = System.currentTimeMillis(); // Record the end time

        long totalExecutionTime = overallEndTime - overallStartTime;
        System.out.println("All thread groups completed. Total time taken: " + totalExecutionTime + " milliseconds");

        int totalRequests = threadGroupSize * numThreadGroups * 1000; // Number of requests sent
        long wallTimeInSeconds = totalExecutionTime / 1000; // Convert to seconds
        int throughput = totalRequests / (int) wallTimeInSeconds; // Requests processed per second

        System.out.println("Wall Time: " + wallTimeInSeconds + " seconds");
        System.out.println("Throughput: " + throughput + " requests per second");

        // Calculate and display statistics
        calculateAndDisplayStatistics();
    }

    private static class HttpRequestTask implements Runnable {
        private final CountDownLatch completed;

        public HttpRequestTask(CountDownLatch completed) {
            this.completed = completed;
        }

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                try {
                    URL url = new URL(SERVER_URL);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    // Set the request method to POST
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    // Create the JSON body
                    String jsonInputString = "{\"albumID\": \"" + generateRandomID() + "\", \"imageSize\": \"" + generateRandomSize() + "\"}";

                    // Get the output stream of the connection
                    OutputStream outputStream = connection.getOutputStream();
                    byte[] input = jsonInputString.getBytes("utf-8");
                    outputStream.write(input, 0, input.length);

                    int responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        System.out.println("File upload failed with response code: " + responseCode);
                    }

                    // Close the connection
                    connection.disconnect();

                    completed.countDown();
                } catch (IOException e) {
                    System.err.println("Thread " + Thread.currentThread().getId() + ": Fatal transport error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        private static String generateRandomID() {
            return String.valueOf(ThreadLocalRandom.current().nextInt(1000) + 1);
        }

        private static String generateRandomSize() {
            return String.valueOf(ThreadLocalRandom.current().nextInt(500) + 100);
        }
    }

    // Nested class to store response data
    private static class ResponseData {
        long startTime;
        long latency;
        int responseCode;

        public ResponseData(long startTime, long latency, int responseCode) {
            this.startTime = startTime;
            this.latency = latency;
            this.responseCode = responseCode;
        }
    }

    // Calculate and display statistics
    private static void calculateAndDisplayStatistics() {
        System.out.println("Statistics for POST requests:");

        // Calculate mean response time
        long totalLatency = 0;
        for (Long latency : latencies) {
            totalLatency += latency;
        }
        double meanResponseTime = (double) totalLatency / latencies.size();
        System.out.println("Mean Response Time: " + meanResponseTime + " milliseconds");

        // Calculate median response time
        latencies.sort(Long::compare);
        double medianResponseTime = calculateMedian(latencies);
        System.out.println("Median Response Time: " + medianResponseTime + " milliseconds");

        // Calculate p99 (99th percentile) response time
        int p99Index = (int) Math.ceil(0.99 * latencies.size());
        long p99ResponseTime = latencies.get(p99Index);
        System.out.println("P99 Response Time: " + p99ResponseTime + " milliseconds");

        // Calculate min and max response time
        long minResponseTime = latencies.get(0);
        long maxResponseTime = latencies.get(latencies.size() - 1);

        System.out.println("Min Response Time: " + minResponseTime + " milliseconds");
        System.out.println("Max Response Time: " + maxResponseTime + " milliseconds");
    }

    private static double calculateMedian(CopyOnWriteArrayList<Long> latencies) {
        int size = latencies.size();
        if (size % 2 == 0) {
            int midIndex1 = size / 2;
            int midIndex2 = midIndex1 - 1;
            return (latencies.get(midIndex1) + latencies.get(midIndex2)) / 2.0;
        } else {
            int midIndex = size / 2;
            return latencies.get(midIndex);
        }
    }
}