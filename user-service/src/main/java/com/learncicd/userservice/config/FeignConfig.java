package com.learncicd.userservice.config;

import feign.Client;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  ✅ Feign Interceptor → forwards JWT in headers for downstream calls.
 *  ✅ Filters → validate/allow/block requests.
 *  ✅ FeignConfig + ErrorDecoder → customize error handling.
 *
 * FeignConfig + CustomErrorDecoder ::
 * ✅ Configure Feign Error Decoder
 * ✅ Feign’s default ErrorDecoder is not very helpful.
 * ✅ You can replace it with a custom ErrorDecoder to handle errors more gracefully.
 * ✅ In your CustomErrorDecoder, you parse the error response from the downstream service and throw a CustomException.
 * ✅ This way, you can handle errors from downstream services in a consistent way.
 *
 *  ✅ FeignConfig registers beans that customize Feign behavior.
 *  ✅ CustomErrorDecoder is used to translate HTTP error responses from downstream services into custom exceptions.
 *  ✅ Example: if bookmark-service returns a 400 with a JSON body, the decoder parses that body into an ErrorResponse and throws a CustomException.
 *  ✅ This gives us clean, domain-specific exceptions instead of raw Feign errors.
 */
@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    @Bean
    public Client feignClient() {
        return new feign.okhttp.OkHttpClient(); // or return new feign.hc5.ApacheHttp5Client();

    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL; // logs full request/response including body
    }

}
