package com.example.auction_management;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;

@EnableScheduling
@SpringBootApplication
@EnableAsync
public class AuctionManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuctionManagementApplication.class, args);
    }

    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId
    ) {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }
}