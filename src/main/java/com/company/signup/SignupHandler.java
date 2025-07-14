package com.company.signup;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.company.model.User;
import com.company.util.ResponseBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.company.util.ResponseBuilder.buildResponse;

public class SignupHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final DynamoDbClient dynamoDb = DynamoDbClient.create();
    private final SesClient sesClient = SesClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String TABLE_NAME = System.getenv("TABLE_NAME");
    private final String FROM_EMAIL = System.getenv("FROM_EMAIL");
    private final String STAGE = "Prod";

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Lambda triggered!");
        context.getLogger().log("Incoming event: " + event);

        try {
            String jsonBody = (String) event.get("body");

            Map<String, String> body = objectMapper.readValue(jsonBody, Map.class);

            String email = body.get("email");
            String uuid = UUID.randomUUID().toString();

            User user = new User(email, uuid, false);
            context.getLogger().log("Saving user to DynamoDB");
            saveUserToDB(user);
            context.getLogger().log("User saved successfully");

            String host = ((Map<String, String>) event.get("headers")).get("Host");
            String verificationLink = buildVerificationLink(host, uuid);

            context.getLogger().log("Sending verification email");
            sendVerificationEmail(email, verificationLink);

            context.getLogger().log("Email sent successfully");

            return buildResponse(200, Map.of("message", "Signup successful! Verification email sent."));

        } catch (Exception ex) {
            context.getLogger().log("Error: " + ex.getMessage());
            return buildResponse(500, Map.of(
                    "error", "Internal server error: " + (ex.getMessage() != null ? ex.getMessage() : "Unknown error")
            ));
        }
    }

    private void saveUserToDB(User user) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(Map.of(
                        "email", AttributeValue.fromS(user.getEmail()),
                        "token", AttributeValue.fromS(user.getToken()),
                        "verified", AttributeValue.fromBool(user.isVerified())
                ))
                .build();
        dynamoDb.putItem(request);
    }

    private void sendVerificationEmail(String toEmail, String verificationLink) {
        String body = "Click the link to verify your email:\n" + verificationLink;

        SendEmailRequest request = SendEmailRequest.builder()
                .destination(Destination.builder().toAddresses(toEmail).build())
                .message(Message.builder()
                        .subject(Content.builder().data("Email Verification").build())
                        .body(Body.builder()
                                .text(Content.builder().data(body).build())
                                .build())
                        .build())
                .source(FROM_EMAIL)
                .build();

        sesClient.sendEmail(request);
    }

    private String buildVerificationLink(String host, String token) {
        return "https://" + host + "/" + STAGE + "/verify?token=" + token;
    }
}