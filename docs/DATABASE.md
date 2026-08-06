# Database Schema

## Entity: `leads`

**Java:** `backend/src/main/java/com/skyheights/realestate/entity/Lead.java`  
**Repository:** `LeadRepository extends JpaRepository<Lead, Long>`  
**Mapper:** `LeadMapper (MapStruct)`

### DDL (PostgreSQL / H2)

```sql
CREATE TABLE leads (
    id                  BIGSERIAL PRIMARY KEY,
    customer_name       VARCHAR(255) NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    location            VARCHAR(255),
    property_type       VARCHAR(100),
    configuration       VARCHAR(50),   -- 2 BHK / 3 BHK / 4 BHK
    budget              VARCHAR(100),  -- ₹85 Lakhs / ₹1.2 Crore / ₹1.6 Crore
    purpose             VARCHAR(100),  -- Buying / Investment / Self-use
    timeline            VARCHAR(100),  -- Immediate / 3 months / 6 months / 1 year
    conversation_summary TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for search
CREATE INDEX idx_leads_phone ON leads(phone);
CREATE INDEX idx_leads_name ON leads(customer_name);
CREATE INDEX idx_leads_location ON leads(location);
CREATE INDEX idx_leads_created ON leads(created_at DESC);
```

### JPA Entity Fields

| Field | Java Type | JPA Column | Validation |
|-------|-----------|------------|------------|
| `id` | `Long` | `id` PK `IDENTITY` | Auto |
| `customerName` | `String` | `customer_name` | `@NotBlank` |
| `phone` | `String` | `phone` | `@Pattern("^[6-9]\\d{9}$")` |
| `location` | `String` | `location` | nullable |
| `propertyType` | `String` | `property_type` | nullable |
| `configuration` | `String` | `configuration` | nullable |
| `budget` | `String` | `budget` | nullable |
| `purpose` | `String` | `purpose` | nullable |
| `timeline` | `String` | `timeline` | nullable |
| `conversationSummary` | `String` | `conversation_summary` TEXT | nullable |
| `createdAt` | `LocalDateTime` | `created_at` | `@PrePersist` now() |
| `updatedAt` | `LocalDateTime` | `updated_at` | `@PrePersist` + `@PreUpdate` |

### Sample Data

```sql
INSERT INTO leads (customer_name, phone, location, property_type, configuration, budget, purpose, timeline, conversation_summary)
VALUES ('Rajesh Kumar', '9876543210', 'Sector 150 Noida', 'Apartment', '3 BHK', '1.2 Crore', 'Investment', '6 months', 'Wants 3BHK for investment, budget 1.2Cr, timeline 6 months');
```

### Configuration

**`application.properties` (H2 default for local dev, no setup needed):**

```properties
spring.datasource.url=jdbc:h2:mem:realestate;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect
```

**PostgreSQL (prod, via env):**

```properties
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/realestate}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

Docker Compose includes `postgres:16-alpine` healthcheck `pg_isready`.

### Migrations

- `ddl-auto=update` for demo (creates/updates table on startup)
- For production, use Flyway: `V1__create_leads_table.sql` with above DDL

### Repository Methods

```java
Optional<Lead> findByPhone(String phone);
List<Lead> findByCustomerNameContainingIgnoreCase(String name);
List<Lead> findByLocationContainingIgnoreCase(String location);
List<Lead> findByPropertyType(String propertyType);
```

### Testing

```java
@SpringBootTest
LeadServiceTest.testCreateAndFetchLead() // uses H2, @ActiveProfiles("test")
```

Manual: `curl http://localhost:8080/api/leads | jq` after `POST /api/call-summary`.

### Retention & Scaling

- Leads are append-only; deletes via `DELETE /api/leads/{id}`
- For scale: partition by `created_at`, add `status` enum, archive after 1 year
