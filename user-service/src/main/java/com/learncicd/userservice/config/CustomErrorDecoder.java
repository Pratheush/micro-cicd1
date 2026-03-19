package com.learncicd.userservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learncicd.userservice.client.exception.DownstreamServiceException;
import com.learncicd.userservice.exception.CustomException;
import com.learncicd.userservice.exception.ErrorResponse;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;

/**
 * CustomErrorDecoder converts HTTP errors from the downstream service into domain exceptions.
 * | HTTP response | Converted to                                  |
 * | ------------- | --------------------------------------------- |
 * | 4xx           | `CustomException` (business error)            |
 * | 5xx           | `DownstreamServiceException` (system failure) |
 *
 */
@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    /*@Override
    public Exception decode(String methodKey, Response response) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // this is to map timestamp which is in the error response from the Downstream services, otherwise it will not show properly or mapped in this service from downstream services.

        // this piece of try block code was not able to decode and map incoming error feign-exception into CustomException.
        *//*try {
            if (response.body() != null) {
                try (InputStream is = response.body().asInputStream()) {
                    JsonNode node = objectMapper.readTree(is);
                    String title = node.has("title") ? node.get("title").asText() : "Unknown error";
                    String detail = node.has("detail") ? node.get("detail").asText() : "No details provided";
                    int status = node.has("status") ? node.get("status").asInt() : response.status();
                    log.error("Error decoding response: {}", response.body().asInputStream()); // already consumed the stream. Never call asInputStream() twice.
                    log.info("CustomEception DETAILS: detail: {}, status : {}, HttpStatus: {}", detail, status, HttpStatus.resolve(status));
                    throw new CustomException(detail, HttpStatus.resolve(status));
                }
            } // If body is null, fall back to status only
            return new CustomException("Unexpected error with status " + response.status(), HttpStatus.valueOf(response.status()));
        } catch (IOException e) {
            return new CustomException("Error parsing ProblemDetail response", HttpStatus.valueOf(response.status()));
        }*//*

        // this below code is working and decoding incoming feignexception or error message and here mapping the feign-exception into our CustomException.
        *//*try(InputStream is = response.body().asInputStream()) {
            ErrorResponse errorResponse = objectMapper.readValue(is, ErrorResponse.class);
            throw new CustomException(errorResponse.getMessage(), errorResponse.getStatus());
        } catch (IOException e) {
            throw new CustomException("INTERNAL_SERVER_ERROR");
        }*//*
    }*/


    /**
     * this piece of code is able to decode incoming feign-exception or error message and here mapping the feign-exception into our CustomException.
     * @param methodKey
     * @param response
     * @return
     */
    /*@Override
    public Exception decode(String methodKey, Response response) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        HttpStatus httpStatus = HttpStatus.valueOf(response.status());

        try {
            if (response.body() != null) {
                try (InputStream is = response.body().asInputStream()) {

                    JsonNode node = objectMapper.readTree(is);

                    String message = node.has("message")
                            ? node.get("message").asText()
                            : "Downstream service error";

                    log.error("Feign error from BOOKMARK-PROJECT: status={}, message={}",
                            httpStatus, message);

                    return new CustomException(message, httpStatus);
                }
            }

            return new CustomException(
                    "Unexpected error from BOOKMARK-PROJECT",
                    httpStatus
            );

        } catch (IOException e) {
            return new CustomException(
                    "Error parsing downstream error response",
                    httpStatus
            );
        }
    }*/


    /**
     * this piece of code is able to decode incoming feign-exception or error message and here mapping the feign-exception into our CustomException.
     * @param methodKey
     * @param response
     * @return
     */
    /*@Override
    public Exception decode(String methodKey, Response response) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        HttpStatus httpStatus = HttpStatus.valueOf(response.status());

        try {
            String message = "Downstream service error";

            if (response.body() != null) {
                log.info("CustomErrorDecoder decode response.body is not null");
                try (InputStream is = response.body().asInputStream()) {
                    JsonNode node = objectMapper.readTree(is);
                    message = node.has("message")
                            ? node.get("message").asText()
                            : message;
                    log.info("CustomErrorDecoder decode message : {}", message);
                }
            }

            // 🔥 Differentiate here
            if (httpStatus.is4xxClientError()) {
                log.info("CustomErrorDecoder decode is4xxClientError");
                return new CustomException(message, httpStatus); // business error
            }

            if (httpStatus.is5xxServerError()) {
                log.info("CustomErrorDecoder decode is5xxServerError");
                return new DownstreamServiceException(message, httpStatus); // system error
            }

            log.error("Feign error from BOOKMARK-PROJECT: status={}, message={}",
                    httpStatus, message);


            return new RuntimeException(message);

        } catch (IOException e) {
            return new DownstreamServiceException(
                    "Error parsing downstream error response",
                    httpStatus
            );
        }
    }*/


    /**
     * this piece of code is able to decode incoming feign-exception or error message and here mapping the feign-exception into our CustomException.
     * @param methodKey
     * @param response
     * @return
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        HttpStatus httpStatus = HttpStatus.valueOf(response.status());
        String message = "Downstream service error";
        HttpStatus status = httpStatus;

        try {
            if (response.body() != null) {
                log.info("CustomErrorDecoder: response body is not null");
                try (InputStream is = response.body().asInputStream()) {
                    JsonNode node = objectMapper.readTree(is);

                    // Extract message
                    if (node.has("message")) {
                        message = node.get("message").asText();
                    }

                    // Extract status safely
                    if (node.has("status")) {
                        String rawStatus = node.get("status").asText();
                        try {
                            status = HttpStatus.valueOf(rawStatus.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            // Fallback: try numeric code or Feign response status
                            try {
                                status = HttpStatus.valueOf(Integer.parseInt(rawStatus));
                            } catch (NumberFormatException nfe) {
                                status = httpStatus;
                            }
                        }
                    }

                    log.info("Decoded error from BOOKMARK-PROJECT: status={}, message={}", status, message);
                    //return new CustomException(message, status);
                }
            }

            /**
             *  Differentiate based on status code if no body
             *  In CustomErrorDecoder, we wrap 4xx client errors (401, 403, 422, etc.) into CustomException.
             *  In your GlobalExceptionHandler, you catch CustomException and log it with: log.warn("Business error: {}", ex.getMessage()); // no stack trace
             *  Because you don’t pass ex into the logger, the stack trace is suppressed. Result: Business errors show up as WARN, clean message only.
             */
            if (httpStatus.is4xxClientError()) {
                log.warn("CustomErrorDecoder: 4xx client error");
                return new CustomException(message, httpStatus);
            }

            /**
             * 🔎 System Failures → ERROR with stack trace
             * In your CustomErrorDecoder, you wrap 5xx server errors and parsing issues into DownstreamServiceException.
             * In your GlobalExceptionHandler, you catch DownstreamServiceException and log it with:
             * log.error("System failure occurred: {}", ex.getMessage(), ex); // includes stack trace
             * Passing ex into the logger prints the full stack trace.
             * Result: System failures show up as ERROR, with full diagnostic info.
             */
            if (httpStatus.is5xxServerError()) {
                log.error("CustomErrorDecoder: 5xx server error");
                return new DownstreamServiceException(message, httpStatus);
            }

            log.error("Feign error from BOOKMARK-PROJECT: status={}, message={}", httpStatus, message);
            return new RuntimeException(message);

        } catch (IOException e) {
            log.error("Error parsing downstream error response", e);
            return new DownstreamServiceException("Error parsing downstream error response", httpStatus);
        }
    }

}
