package com.complyance.Data_Governance_Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MongoCheck implements CommandLineRunner {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Override
    public void run(String... args) {
        System.out.println("✅ Connected to MongoDB: " + mongoUri);
    }
}
