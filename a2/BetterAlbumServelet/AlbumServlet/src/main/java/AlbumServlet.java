import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.google.gson.Gson;


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
    DynamoDB docDynamoDB = new DynamoDB(dynamoDB);

    private ExecutorService threadPool = Executors.newFixedThreadPool(200);

    // String endpoint = "http://localhost:8000"; Default endpoint for DynamoDB Local
    //    AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard()
    //            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "us-west-2"))
    //            .build();
    private final String TABLE_NAME = "Albums";



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
