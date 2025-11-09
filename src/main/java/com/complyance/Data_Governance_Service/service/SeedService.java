package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.model.AuditEntry;
import com.complyance.Data_Governance_Service.model.Post;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.repository.PostRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class SeedService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final Random random = new Random();
    Instant randomCreatedAt = Instant.now().minusSeconds(random.nextInt(60 * 60 * 24 * 30)); // up to ~30 days ago


    public SeedService(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    public String seedUsersAndPosts(int userCount, int postsPerUser) {
        if (userRepository.count() > 0 || postRepository.count() > 0) {
            return "⚠️ Users or Posts already exist — skipping seeding.";
        }

        // ----- Seed Users -----
        List<String> firstNames = List.of("Aarav", "Vivaan", "Diya", "Isha", "Rohan", "Kiran", "Sneha", "Kabir", "Neha", "Riya");
        List<String> lastNames  = List.of("Sharma", "Patel", "Mehta", "Reddy", "Kapoor", "Singh", "Nair", "Gupta", "Bose", "Chopra");
        List<String> roles      = List.of("USER", "EDITOR", "ADMIN");

        List<UserProfile> users = new ArrayList<>();

        for (int i = 0; i < userCount; i++) {
            String first = firstNames.get(random.nextInt(firstNames.size()));
            String last  = lastNames.get(random.nextInt(lastNames.size()));
            String username = (first + "_" + last + "_" + (100 + random.nextInt(900))).toLowerCase();
            String email = username + "@example.com";
            String name = first + " " + last;

            List<String> assignedRoles = new ArrayList<>();
            assignedRoles.add(roles.get(random.nextInt(roles.size())));
            if (random.nextBoolean()) {
                String extraRole = roles.get(random.nextInt(roles.size()));
                if (!assignedRoles.contains(extraRole)) assignedRoles.add(extraRole);
            }

            UserProfile user = UserProfile.builder()
                    .username(username)
                    .email(email)
                    .name(name)
                    .roles(assignedRoles)
                    .status("ACTIVE")
                    .createdAt(randomCreatedAt.plusSeconds(random.nextInt(3600)))
                    .updatedAt(Instant.now())
                    .auditTrail(List.of(
                            AuditEntry.builder()
                                    .action("CREATE")
                                    .details("Seeded test user")
                                    .performedBy("SYSTEM")
                                    .timestamp(Instant.now())
                                    .build()
                    ))
                    .build();

            users.add(user);
        }

        userRepository.saveAll(users);

        // ----- Seed Posts -----
        List<String> sampleTitles = List.of(
                "Building Secure APIs",
                "The Future of AI in Governance",
                "Understanding MongoDB Aggregations",
                "Spring Boot Tips for Scalability",
                "How to Design a Secure Network",
                "Exploring Data Privacy Laws",
                "Async Programming in Java",
                "Effective Logging Strategies",
                "Optimizing REST API Performance",
                "Event-Driven Microservices Explained"
        );

        List<String> sampleContents = List.of(
                "This post explores practical security considerations for API design and deployment.",
                "AI is reshaping governance structures and compliance models globally.",
                "MongoDB aggregation pipelines allow powerful data transformations and analytics.",
                "Spring Boot provides a flexible foundation for building scalable enterprise systems.",
                "Network design plays a key role in ensuring organizational data security.",
                "Privacy-first design is crucial for compliance with emerging data regulations.",
                "Async programming allows high throughput and efficient resource utilization.",
                "Proper logging helps in auditing, debugging, and improving system reliability.",
                "Optimizing REST APIs involves caching, pagination, and load balancing.",
                "Event-driven architecture offers scalability and decoupled system design."
        );

        List<Post> posts = new ArrayList<>();

        for (UserProfile user : users) {
            for (int j = 0; j < postsPerUser; j++) {
                String title = sampleTitles.get(random.nextInt(sampleTitles.size()));
                String content = sampleContents.get(random.nextInt(sampleContents.size()));

                Post post = Post.builder()
                        .userId(user.getId())
                        .title(title)
                        .content(content)
                        .createdAt(randomCreatedAt.plusSeconds(random.nextInt(3600)))
                        .updatedAt(Instant.now())
                        .deleted(false)
                        .build();

                posts.add(post);
            }
        }

        postRepository.saveAll(posts);

        return String.format("✅ Seed completed: %d users and %d posts added.", users.size(), posts.size());
    }
}
