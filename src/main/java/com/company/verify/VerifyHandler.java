package com.company.verify;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.*;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.List;
import java.util.Map;

import static com.company.util.ResponseBuilder.buildResponse;

public class VerifyHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final String TABLE_NAME = System.getenv("TABLE_NAME");

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Lambda triggered. Event: " + event);

        try {
            Object paramObj = event.get("queryStringParameters");
            Map<String, String> params = (Map<String, String>) paramObj;

            String token = params.get("token");

            List<Map<String, AttributeValue>> userAttributes = findUserByToken(token);

            if (userAttributes == null || userAttributes.isEmpty()) {
                return buildResponse(404, Map.of("message", "Invalid or expired token"));
            }

            String email = userAttributes.get(0).get("email").s();

            updateUserVerification(email, context);

            return buildResponse(200, Map.of("message", "Email verified successfully!"));

        } catch (Exception ex) {
            context.getLogger().log("Error: " + ex.getMessage());
            return buildResponse(500, Map.of(
                    "message", "Verification failed",
                    "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
            ));
        }
    }

    private List<Map<String, AttributeValue>> findUserByToken(String token) {
        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("#tk = :t")
                .expressionAttributeNames(Map.of("#tk", "token"))
                .expressionAttributeValues(Map.of(":t", AttributeValue.fromS(token)))
                .build();

        ScanResponse response = dynamoDb.scan(request);
        return response.items();
    }

    private void updateUserVerification(String email, Context context) {
        context.getLogger().log("Updating DynamoDB for: " + email);
        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Map.of("email", AttributeValue.fromS(email)))
                .updateExpression("SET verified = :v")
                .expressionAttributeValues(Map.of(":v", AttributeValue.fromBool(true)))
                .build());
    }
}
