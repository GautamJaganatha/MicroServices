package com.microserrvices.OrderService.external.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microserrvices.OrderService.execption.CustomExecption;
import com.microserrvices.OrderService.external.response.CustomErrorResponse;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class CustomErrorDecoder implements ErrorDecoder {
    @SneakyThrows
    @Override
    public Exception decode(String s, Response response) {
        ObjectMapper objectMapper
                = new ObjectMapper();

        log.info("::{}", response.request().url());
        log.info("::{}", response.request().headers());
        try {
            // Buffering the response body to allow multiple reads
            String bodyString = Util.toString(response.body().asReader());
            log.info("Response Body: {}", bodyString);  // Log the response body

            // Deserialize the response body
            CustomErrorResponse customErrorResponse =
                    objectMapper.readValue(bodyString, CustomErrorResponse.class);

            // Return a custom exception with error details
            return new CustomExecption(customErrorResponse.getErrorMessage(),
                    customErrorResponse.getErrorCode(),
                    response.status());
        } catch (IOException e) {
            // Catch block in case of deserialization errors or empty body
            log.error("Failed to deserialize response body", e);
            throw new CustomExecption("Internal Server Error",
                    "INTERNAL_SERVER_ERROR",
                    500);
        }





//        try {
//            CustomErrorResponse customErrorResponse =
//                    objectMapper.readValue(response.body().asInputStream(),
//                            CustomErrorResponse.class);
//            return new CustomExecption(customErrorResponse.getErrorMessage(),
//                    customErrorResponse.getErrorCode(),
//                    response.status()
//                    );
//        } catch (IOException e) {
//            throw new CustomExecption("Internal Server Error",
//                    "INTERNAL_SERVER_ERROR",
//                    500);
//        }
    }
}
