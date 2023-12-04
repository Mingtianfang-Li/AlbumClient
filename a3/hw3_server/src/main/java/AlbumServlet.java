import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@WebServlet(name = "AlbumServlet", value = "/AlbumServlet")
public class AlbumServlet extends HttpServlet {
    Regions region = Regions.US_WEST_2;
    AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
//    AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
//        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
//        .build();
    DynamoDB docDynamoDB = new DynamoDB(dynamoDB);

    private ExecutorService threadPool = Executors.newFixedThreadPool(200);
    private final String TABLE_NAME = "Albums";
    private final String REVIEW_TABLE_NAME = "Review_Albums";

    private final String RABBITMQ_HOST = "34.219.4.94";
    private final String QUEUE_NAME = "likes_dislikes_queue";

    private ConnectionFactory factory;
    private Connection connection;
    private static Map<String, String> dataHashMap = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();

        // Initialize RabbitMQ connection
        factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);

        try {
            connection = factory.newConnection();
        } catch (Exception e) {
            throw new ServletException("Error initializing RabbitMQ", e);
        }
    }

    @Override
    public void destroy() {
        // Close RabbitMQ connection on servlet destroy
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Shut down the thread pool
        threadPool.shutdown();

        super.destroy();
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createTableIfNotExists();
        response.setContentType("application/json");

        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid request. Please provide the album ID in the URL.");
            return;
        }

        // Extracting the albumID from the URL path
        String[] pathParts = pathInfo.split("/");
        String albumID = pathParts[1]; // Assuming the albumID is the first part of the path

        GetItemRequest getItemRequest = new GetItemRequest().withKey(new HashMap<String, AttributeValue>() {{
            put("albumID", new AttributeValue(albumID));
        }}).withTableName(TABLE_NAME);

        try {
            GetItemResult result = dynamoDB.getItem(getItemRequest);
            if (result.getItem() != null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(result.getItem().toString());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("Album not found for the given ID.");
            }
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error retrieving album information from the database.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        createTableIfNotExists();
        response.setContentType("application/json");
        String pathInfo = request.getPathInfo();
        if (!pathInfo.equals("/review")) {
            try (BufferedReader reader = request.getReader()) {
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                // Convert the JSON data into an instance of ImageMetaData
                ImageMetaData albumData = new Gson().fromJson(sb.toString(), ImageMetaData.class);

                // You can now access the albumID and imageSize fields
                String albumID = albumData.getAlbumID();
                String imageSize = albumData.getImageSize();

                // Execute the task in the thread pool
                threadPool.execute(() -> {
                    Table table = docDynamoDB.getTable(TABLE_NAME);
                    Item item = new Item().withPrimaryKey("albumID", albumID).withString("imageSize", imageSize);
                    table.putItem(item);
                });

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write("Album data processing initiated.");
            } catch (Exception ex) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Error processing album data.");
            }
        } else {
            try {
                // Read JSON data from the request body
                BufferedReader reader = request.getReader();
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                // Convert the JSON data into an instance of ReviewData
                ReviewData reviewData = new Gson().fromJson(sb.toString(), ReviewData.class);

                // Validate the received data (you can add more validation as needed)
                if (isValidReviewData(reviewData)) {
                    // Extract relevant information
                    String albumID = reviewData.getAlbumID();
                    String reviewType = reviewData.getReviewType();

                    // Store the data in the HashMap
                    dataHashMap.put(albumID, reviewType);

                    // Publish message to RabbitMQ (if needed)
                    publishToRabbitMQ(albumID, reviewType);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().write("Review data processing initiated.");
                } else {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Invalid review data received.");
                }
            } catch (Exception ex) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error processing review data.");
            }
        }
    }
//    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        response.setContentType("application/json");
//
//        // Extract the path info from the request
//        String pathInfo = request.getPathInfo();
//
//        // Check if the path matches "/review"
//        if (pathInfo != null && pathInfo.equals("/review")) {
//            try {
//                // Read JSON data from the request body
//                BufferedReader reader = request.getReader();
//                StringBuilder sb = new StringBuilder();
//                String line;
//
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//
//                // Convert the JSON data into an instance of ReviewData
//                ReviewData reviewData = new Gson().fromJson(sb.toString(), ReviewData.class);
//
//                // Validate the received data (you can add more validation as needed)
//                if (isValidReviewData(reviewData)) {
//                    // Extract relevant information
//                    String albumID = reviewData.getAlbumID();
//                    String reviewType = reviewData.getReviewType();
//
//                    // Store the data in the HashMap
//                    dataHashMap.put(albumID, reviewType);
//
//                    // Publish message to RabbitMQ (if needed)
//                    publishToRabbitMQ(albumID, reviewType);
//
//                    response.setStatus(HttpServletResponse.SC_OK);
//                    response.getWriter().write("Review data processing initiated.");
//                } else {
//                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//                    response.getWriter().write("Invalid review data received.");
//                }
//            } catch (Exception ex) {
//                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//                response.getWriter().write("Error processing review data.");
//            }
//        } else {
//            // If the path is not "/review", return a 404 status
//            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//            response.getWriter().write("Not Found");
//        }
//    }

    private void publishToRabbitMQ(String albumID, String reviewType) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RABBITMQ_HOST);
            factory.setPort(5672);

            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                // Declare a queue
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                // Prepare the message content (customize based on your needs)
                String message = "AlbumID: " + albumID + ", ReviewType: " + reviewType;

                // Publish the message to the queue
                channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
                System.out.println(" [x] Sent '" + message + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private boolean isValidReviewData(ReviewData reviewData) {
        if (reviewData == null) {
            return false;
        }

        // Validate other fields as needed
        String albumID = reviewData.getAlbumID();
        String reviewType = reviewData.getReviewType();

        // Check if albumID and reviewType are not null or empty
        return albumID != null && !albumID.isEmpty() && reviewType != null && !reviewType.isEmpty();
    }

    private boolean isUrlValid(String[] urlParts) {
        if (urlParts.length > 2){
            return false;
        }
        String albumID = urlParts[1];
        for (char c : albumID.toCharArray()){
            if (!Character.isDigit(c)){
                return false;
            }
        }
        return true;
    }

    private void createTableIfNotExists() {
        try {
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(TABLE_NAME);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table status: " + tableDescription.getTableStatus());
        } catch (ResourceNotFoundException e) {
            // Create the table if it doesn't exist
            ArrayList<KeySchemaElement> keySchema = new ArrayList<>();
            keySchema.add(new KeySchemaElement().withAttributeName("albumID").withKeyType(KeyType.HASH));

            ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
            attributeDefinitions.add(new AttributeDefinition().withAttributeName("albumID").withAttributeType(ScalarAttributeType.S));

            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(TABLE_NAME)
                    .withKeySchema(keySchema)
                    .withAttributeDefinitions(attributeDefinitions)
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

            CreateTableResult table = dynamoDB.createTable(createTableRequest);
        }
    }
}
