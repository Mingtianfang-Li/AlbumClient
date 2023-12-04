import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.rabbitmq.client.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.cli.*;


import java.util.ArrayList;

public class Consumer {



    private final static String QUEUE_NAME = "likes_dislikes_queue";
    private final static String DYNAMO_TABLE_NAME = "Review_Table"; // Replace with your DynamoDB table name

    AmazonDynamoDB dynamoDB;
    Regions region = Regions.US_WEST_2;
    DynamoDB dynamoDBClient;

    CommandLineParser parser = new DefaultParser();


    public Consumer() {
        this.dynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(region).build();
//        dynamoDB = AmazonDynamoDBClientBuilder.standard()
//                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://localhost:8000", "us-west-2"))
//                .build();
        this.dynamoDBClient = new DynamoDB(dynamoDB);
    }

    public void consumeMessages() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("34.219.4.94");

            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                channel.queueDeclare(QUEUE_NAME, false, false, false, null);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), "UTF-8");
                    System.out.println("Received message: " + message);

                    // Process the message and update DynamoDB
                    updateDynamoDB(message);
                };

                channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
                });

                System.out.println("RabbitMQ Consumer started. Waiting for messages...");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateDynamoDB(String message) {
        // Assuming the message format is "AlbumID: <albumID>, ReviewType: <reviewType>"
        String[] parts = message.split(", ");
        String albumID = parts[0].split(": ")[1];
        String reviewType = parts[1].split(": ")[1];

        // Check if the DynamoDB table exists; if not, create a new one
        createTableIfNotExists(DYNAMO_TABLE_NAME);

        // Perform the DynamoDB update logic here
        // This is a simple example, modify based on your actual DynamoDB schema and update requirements

        // For simplicity, we assume you have a table with primary key "albumID"
        Table table = dynamoDBClient.getTable(DYNAMO_TABLE_NAME);
        Item item = new Item().withPrimaryKey("albumID", albumID).withString("reviewType", reviewType);
        table.putItem(item);

        System.out.println("DynamoDB updated for AlbumID: " + albumID);
    }

    private void createTableIfNotExists(String dynamoTableName) {
        try {
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(DYNAMO_TABLE_NAME);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table status: " + tableDescription.getTableStatus());
        } catch (ResourceNotFoundException e) {
            // Create the table if it doesn't exist
            ArrayList<KeySchemaElement> keySchema = new ArrayList<>();
            keySchema.add(new KeySchemaElement().withAttributeName("albumID").withKeyType(KeyType.HASH));

            ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
            attributeDefinitions.add(new AttributeDefinition().withAttributeName("albumID").withAttributeType(ScalarAttributeType.S));

            CreateTableRequest createTableRequest = new CreateTableRequest()
                    .withTableName(DYNAMO_TABLE_NAME)
                    .withKeySchema(keySchema)
                    .withAttributeDefinitions(attributeDefinitions)
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

            CreateTableResult table = dynamoDB.createTable(createTableRequest);
        }
    }



    public static void main(String[] args) {
        Consumer consumer = new Consumer();
        consumer.consumeMessages();
    }
}
