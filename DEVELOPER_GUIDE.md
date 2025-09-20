# Spring Boot Job Dispatcher - Developer Guide

## Table of Contents
1. [Overview](#overview)
2. [Project Structure](#project-structure)
3. [Key Concepts](#key-concepts)
4. [Getting Started](#getting-started)
5. [API Endpoints](#api-endpoints)
6. [Authentication & Security](#authentication--security)
7. [Database Schema](#database-schema)
8. [Configuration](#configuration)
9. [Development Workflow](#development-workflow)
10. [Testing](#testing)
11. [Troubleshooting](#troubleshooting)

## Overview

This is a Spring Boot application that provides a **job dispatching service** for managing and executing background tasks. It supports:

- **One-time jobs** - Execute once
- **Repetitive jobs** - Execute on schedule
- **Job grouping/batching** - Group similar jobs together
- **Rate limiting** - Control request frequency
- **JWT Authentication** - Secure API access
- **Real-time updates** - WebSocket notifications
- **Retry mechanism** - Automatic retry with exponential backoff

## Project Structure

```
src/main/java/com/example/jobdispatcher/
├── annotation/           # Custom annotations (@RateLimited)
├── aspect/              # AOP aspects (Rate limiting)
├── config/              # Configuration classes
│   ├── ExecutorConfig.java      # Thread pool configuration
│   ├── RateLimitingConfig.java  # Rate limiting setup
│   ├── WebSecurityConfig.java   # Security configuration
│   └── JwtUtil.java            # JWT utilities
├── controller/          # REST API endpoints
│   ├── JobController.java           # Job management APIs
│   ├── AuthenticationController.java # Login endpoint
│   └── ApiKeyManagementController.java # API key management
├── entity/              # JPA entities (database tables)
│   ├── ScheduledJob.java    # Job records
│   ├── ThreadPool.java      # Thread pool configurations
│   ├── AppServer.java       # Application server info
│   └── ApiKey.java          # API authentication keys
├── enums/               # Enumerations
│   └── JobPriority.java     # Job priority levels
├── filter/              # HTTP filters
│   ├── JwtAuthenticationFilter.java # JWT validation
│   └── ApiKeyAuthenticationFilter.java # API key validation
├── model/               # Request/Response DTOs
│   ├── OneTimeJobRequest.java
│   ├── RepetitiveJobRequest.java
│   └── JobSubmissionResponse.java
├── repository/          # Data access layer
│   ├── ScheduledJobRepository.java
│   ├── ThreadPoolRepository.java
│   ├── AppServerRepository.java
│   └── ApiKeyRepository.java
├── service/             # Business logic
│   ├── JobDispatcherService.java      # Core job dispatching
│   ├── AuthenticationService.java     # JWT authentication
│   ├── RateLimitingService.java       # Rate limiting logic
│   ├── JobGroupingService.java        # Job batching
│   └── JobRetryService.java           # Retry mechanism
└── websocket/          # Real-time communication
    └── JobStatusWebSocketHandler.java
```

## Key Concepts

### 1. Spring Boot Basics

**Spring Boot** is a framework that simplifies Spring application development by providing:
- **Auto-configuration** - Automatically configures beans based on dependencies
- **Starter dependencies** - Pre-configured dependency sets
- **Embedded servers** - Built-in Tomcat server
- **Production-ready features** - Health checks, metrics, etc.

### 2. Dependency Injection (DI)

Spring uses **Dependency Injection** to manage object dependencies:

```java
@Service
public class JobDispatcherService {
    
    @Autowired  // Spring injects this dependency
    private DatabasePersistenceService databasePersistenceService;
    
    // Constructor injection (preferred)
    public JobDispatcherService(DatabasePersistenceService databasePersistenceService) {
        this.databasePersistenceService = databasePersistenceService;
    }
}
```

### 3. Annotations

| Annotation | Purpose | Example |
|------------|---------|---------|
| `@SpringBootApplication` | Main application class | `@SpringBootApplication` |
| `@RestController` | REST API controller | `@RestController` |
| `@Service` | Business logic service | `@Service` |
| `@Repository` | Data access layer | `@Repository` |
| `@Entity` | JPA database entity | `@Entity` |
| `@Autowired` | Dependency injection | `@Autowired` |
| `@Value` | Inject configuration values | `@Value("${app.name}")` |
| `@PostMapping` | HTTP POST endpoint | `@PostMapping("/jobs")` |
| `@GetMapping` | HTTP GET endpoint | `@GetMapping("/jobs/{id}")` |

### 4. JPA (Java Persistence API)

JPA provides object-relational mapping:

```java
@Entity
@Table(name = "scheduled_jobs")
public class ScheduledJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false)
    private String jobId;
    
    @Enumerated(EnumType.STRING)
    private JobStatus status;
}
```

## Getting Started

### 1. Prerequisites

- **Java 11+**
- **Maven 3.6+**
- **MySQL 8.0+** (for production)
- **IDE** (IntelliJ IDEA, Eclipse, VS Code)

### 2. Setup Database

```sql
-- Create database
CREATE DATABASE job_dispatcher;

-- Create user
CREATE USER 'jobuser'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON job_dispatcher.* TO 'jobuser'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configuration

Update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/job_dispatcher
spring.datasource.username=jobuser
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT Configuration
jwt.secret=mySecretKey123456789012345678901234567890
jwt.expiration=3600000

# Rate Limiting
rate-limiting.enabled=true
rate-limiting.default-requests-per-minute=60
```

### 4. Running the Application

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Run application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Authentication

#### Login
```http
POST /api/auth/login
Content-Type: application/x-www-form-urlencoded

apiKeyId=your_api_key&apiKeySecret=your_secret
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "message": "Authentication successful"
}
```

### Job Management

#### Submit One-Time Job
```http
POST /api/jobs/onetime
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "jobId": "job-001",
  "jobName": "Process Data",
  "jobClassName": "com.example.jobs.DataProcessor",
  "canGroup": true,
  "groupKey": "data-processing",
  "groupBufferMillis": 5000
}
```

#### Submit Repetitive Job
```http
POST /api/jobs/repetitive
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "jobId": "job-002",
  "jobName": "Daily Report",
  "jobClassName": "com.example.jobs.ReportGenerator",
  "intervalMillis": 86400000,
  "initialDelayMillis": 0
}
```

#### Get Job Status
```http
GET /api/jobs/{jobId}
Authorization: Bearer <jwt_token>
```

### API Key Management

#### Create API Key
```http
POST /api/keys
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "appServerId": "server-001",
  "description": "Production API Key"
}
```

## Authentication & Security

### JWT Authentication Flow

1. **Client** sends API key and secret to `/api/auth/login`
2. **Server** validates credentials and returns JWT token
3. **Client** includes JWT token in `Authorization: Bearer <token>` header
4. **JwtAuthenticationFilter** validates token on each request

### Security Configuration

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
            .antMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter(), 
                           UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

## Database Schema

### Tables

#### scheduled_jobs
```sql
CREATE TABLE scheduled_jobs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id VARCHAR(255) NOT NULL UNIQUE,
    job_name VARCHAR(255) NOT NULL,
    job_class_name VARCHAR(500) NOT NULL,
    job_type ENUM('ONE_TIME', 'REPETITIVE') NOT NULL,
    status ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED') NOT NULL,
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'CRITICAL') DEFAULT 'NORMAL',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    execution_time_ms BIGINT NULL,
    error_message TEXT NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    next_retry_at TIMESTAMP NULL,
    last_retry_at TIMESTAMP NULL,
    can_group BOOLEAN DEFAULT FALSE,
    group_key VARCHAR(255) NULL,
    group_buffer_millis BIGINT NULL,
    interval_millis BIGINT NULL,
    initial_delay_millis BIGINT NULL,
    thread_pool_id BIGINT NULL,
    app_server_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### api_keys
```sql
CREATE TABLE api_keys (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_name VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    client_id VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    usage_count BIGINT DEFAULT 0,
    rate_limit_per_minute INT DEFAULT 60,
    rate_limit_per_hour INT DEFAULT 1000,
    allowed_job_types VARCHAR(255),
    created_by VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `spring.datasource.url` | Database connection URL | - |
| `spring.datasource.username` | Database username | - |
| `spring.datasource.password` | Database password | - |
| `jwt.secret` | JWT signing secret | - |
| `jwt.expiration` | JWT expiration time (ms) | 3600000 |
| `rate-limiting.enabled` | Enable rate limiting | true |
| `rate-limiting.default-requests-per-minute` | Default rate limit | 60 |

### Thread Pool Configuration

```java
@Configuration
public class ExecutorConfig {
    
    @Bean("jobExecutor")
    public ThreadPoolTaskExecutor jobExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("job-");
        executor.initialize();
        return executor;
    }
}
```

## Development Workflow

### 1. Adding a New Job Type

Create a job class:
```java
public class MyCustomJob implements OneTimeJob {
    
    @Override
    public void process() {
        // Your job logic here
        System.out.println("Executing custom job: " + getJobName());
    }
}
```

### 2. Adding New API Endpoints

```java
@RestController
@RequestMapping("/api/custom")
public class CustomController {
    
    @Autowired
    private CustomService customService;
    
    @GetMapping("/data")
    public ResponseEntity<Object> getData() {
        return ResponseEntity.ok(customService.getData());
    }
}
```

### 3. Database Changes

Use Flyway migrations:
```sql
-- V2__Add_new_column.sql
ALTER TABLE scheduled_jobs ADD COLUMN custom_field VARCHAR(255);
```

### 4. Adding Configuration Properties

```java
@ConfigurationProperties(prefix = "custom")
@Component
public class CustomConfig {
    private String property1;
    private int property2;
    
    // Getters and setters
}
```

## Testing

### Unit Tests

```java
@SpringBootTest
class JobDispatcherServiceTest {
    
    @Autowired
    private JobDispatcherService jobDispatcherService;
    
    @Test
    void testJobSubmission() {
        OneTimeJobRequest request = new OneTimeJobRequest();
        request.setJobId("test-job");
        request.setJobName("Test Job");
        request.setJobClassName("com.example.TestJob");
        
        // Test the service
        jobDispatcherService.dispatchOneTimeJob(request);
        
        // Assertions
        assertThat(result).isNotNull();
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class JobControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testJobSubmissionEndpoint() throws Exception {
        mockMvc.perform(post("/api/jobs/onetime")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jobId\":\"test\",\"jobName\":\"Test\"}"))
                .andExpect(status().isAccepted());
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```
Error: Access denied for user 'root'@'localhost'
```
**Solution:** Check database credentials in `application.properties`

#### 2. JWT Token Issues
```
Error: JWT signature does not match
```
**Solution:** Ensure `jwt.secret` is consistent across restarts

#### 3. Rate Limiting Issues
```
Error: Rate limit exceeded
```
**Solution:** Check rate limiting configuration or wait for reset

#### 4. Thread Pool Issues
```
Error: Task rejected from executor
```
**Solution:** Increase thread pool size or queue capacity

### Debugging Tips

1. **Enable SQL Logging:**
   ```properties
   spring.jpa.show-sql=true
   logging.level.org.hibernate.SQL=DEBUG
   ```

2. **Enable Request Logging:**
   ```properties
   logging.level.org.springframework.web=DEBUG
   ```

3. **Check Application Health:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

4. **View Application Metrics:**
   ```bash
   curl http://localhost:8080/actuator/metrics
   ```

### Logging Configuration

Add to `src/main/resources/logback-spring.xml`:
```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.example.jobdispatcher" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

## Best Practices

### 1. Code Organization
- Keep controllers thin (delegate to services)
- Use DTOs for API requests/responses
- Implement proper error handling
- Use meaningful variable and method names

### 2. Security
- Always validate input data
- Use parameterized queries (JPA handles this)
- Implement proper authentication
- Log security events

### 3. Performance
- Use connection pooling
- Implement caching where appropriate
- Monitor thread pool usage
- Use async processing for long-running tasks

### 4. Testing
- Write unit tests for business logic
- Write integration tests for APIs
- Use test profiles for different environments
- Mock external dependencies

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [JWT.io](https://jwt.io/) - JWT token debugger
- [MySQL Documentation](https://dev.mysql.com/doc/)

## Support

For questions or issues:
1. Check the logs for error messages
2. Review this documentation
3. Check Spring Boot documentation
4. Create an issue in the project repository

---

**Happy Coding! 🚀**

