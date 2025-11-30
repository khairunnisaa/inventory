# Warehouse Management API - Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture Layers](#architecture-layers)
3. [Design Patterns](#design-patterns)
4. [Key Design Decisions](#key-design-decisions)
5. [Data Model](#data-model)
6. [Transaction Management](#transaction-management)
7. [Concurrency Control](#concurrency-control)
8. [Error Handling Strategy](#error-handling-strategy)
9. [API Design](#api-design)
10. [Security Considerations](#security-considerations)
11. [Scalability Considerations](#scalability-considerations)

---

## Overview

This warehouse management system is built using **Spring Boot 3.x** with a **layered architecture** pattern. The system follows RESTful principles and implements robust transaction management, optimistic locking, and comprehensive validation to ensure data integrity and prevent race conditions.

### Core Principles
- **Separation of Concerns**: Each layer has a distinct responsibility
- **Single Responsibility**: Each class has one clear purpose
- **Dependency Inversion**: Controllers depend on service interfaces, services depend on repository interfaces
- **Fail-Fast Validation**: Validate input as early as possible
- **Transaction Safety**: Ensure atomic operations for critical business logic

---

## Architecture Layers

The application follows a **4-layer architecture**:

```
┌─────────────────────────────────────┐
│      Controller Layer (REST API)    │  ← HTTP Requests/Responses
├─────────────────────────────────────┤
│      Service Layer (Business Logic) │  ← Business Rules & Validation
├─────────────────────────────────────┤
│      Repository Layer (Data Access) │  ← Database Operations
├─────────────────────────────────────┤
│      Entity Layer (Domain Model)    │  ← JPA Entities
└─────────────────────────────────────┘
```

### 1. Controller Layer (`com.example.warehouse.controller`)

**Purpose**: Handle HTTP requests and responses, input validation, and DTO conversion.

**Responsibilities**:
- Receive HTTP requests
- Validate request DTOs using `@Valid`
- Convert between Entity and DTO objects
- Return appropriate HTTP status codes
- Handle content negotiation (JSON)

**Key Classes**:
- `ItemController`: CRUD operations for items
- `ItemVariantController`: CRUD operations for variants
- `OrderController`: Order creation and retrieval

**Design Decision**: Controllers return DTOs instead of entities to:
- Prevent circular reference issues in JSON serialization
- Control what data is exposed to clients
- Avoid lazy loading exceptions
- Maintain API contract stability

**Example**:
```java
@RestController
@RequestMapping("/api/items")
public class ItemController {
    @GetMapping
    public List<ItemDto> getAll() {
        return itemService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
```

### 2. Service Layer (`com.example.warehouse.service`)

**Purpose**: Implement business logic, enforce business rules, and coordinate transactions.

**Responsibilities**:
- Business rule validation
- Transaction management
- Coordinate multiple repository operations
- Stock management and validation
- Order processing logic
- Logging for observability

**Key Classes**:
- `ItemService`: Item business logic
- `ItemVariantService`: Variant business logic
- `OrderService`: Complex order processing with stock validation

**Design Decision**: Services are `@Transactional` by default because:
- Most operations require database consistency
- Stock updates must be atomic
- Order creation involves multiple database operations

**Example**:
```java
@Service
@Transactional
public class OrderService {
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Order createOrder(CreateOrderRequest request) {
        // 1. Validate all order lines
        // 2. Check stock availability
        // 3. Reduce stock atomically
        // 4. Create order
    }
}
```

### 3. Repository Layer (`com.example.warehouse.repository`)

**Purpose**: Abstract database access using Spring Data JPA.

**Responsibilities**:
- Provide CRUD operations
- Define custom queries when needed
- Handle entity persistence
- Support optimistic locking

**Key Interfaces**:
- `ItemRepository extends JpaRepository<Item, Long>`
- `ItemVariantRepository extends JpaRepository<ItemVariant, Long>`
- `OrderRepository extends JpaRepository<Order, Long>`

**Design Decision**: Use Spring Data JPA because:
- Reduces boilerplate code
- Provides automatic query generation
- Supports pagination and sorting out of the box
- Easy to extend with custom queries

### 4. Entity Layer (`com.example.warehouse.entity`)

**Purpose**: Represent domain models and database schema.

**Responsibilities**:
- Define database schema via JPA annotations
- Enforce data constraints
- Support optimistic locking
- Define relationships between entities

**Key Entities**:
- `Item`: Represents a product/item
- `ItemVariant`: Represents a variant of an item (e.g., size, color)
- `Order`: Represents a customer order
- `OrderLine`: Represents a line item in an order

**Design Decision**: Use JPA entities with annotations because:
- Type-safe database access
- Automatic schema generation
- Relationship management
- Validation at the entity level

---

## Design Patterns

### 1. DTO (Data Transfer Object) Pattern

**Purpose**: Transfer data between layers without exposing internal entity structure.

**Implementation**:
- `ItemDto`: Exposes item data without circular references
- `ItemVariantDto`: Exposes variant data with itemId reference
- `OrderDto`: Exposes order data with nested OrderLineDto
- `CreateOrderRequest`: Request DTO for order creation

**Benefits**:
- Prevents circular reference issues
- Controls API contract
- Allows versioning of API without changing entities
- Separates internal model from external API

### 2. Repository Pattern

**Purpose**: Abstract data access logic.

**Implementation**: Spring Data JPA repositories provide:
- Standard CRUD operations
- Custom query methods
- Pagination support

**Benefits**:
- Testability (easy to mock)
- Flexibility (can switch implementations)
- Reduces boilerplate code

### 3. Service Layer Pattern

**Purpose**: Encapsulate business logic separate from presentation and data access.

**Implementation**: Services coordinate between repositories and enforce business rules.

**Benefits**:
- Reusability (can be used by different controllers)
- Testability (can test business logic independently)
- Transaction management at the right level

### 4. Exception Handling Pattern

**Purpose**: Centralized error handling with consistent responses.

**Implementation**: `GlobalExceptionHandler` with `@ControllerAdvice`:
- Maps exceptions to HTTP status codes
- Provides consistent error response format
- Logs errors for debugging

**Benefits**:
- Consistent error responses
- Centralized error handling
- Better error logging and monitoring

---

## Key Design Decisions

### 1. Why H2 In-Memory Database?

**Decision**: Use H2 in-memory database for development and testing.

**Rationale**:
- **Zero Configuration**: No external database setup required
- **Fast Development**: Quick startup and testing
- **Portability**: Works on any machine without database installation
- **Testing**: Perfect for integration tests

**Trade-offs**:
- Data is lost on application restart (acceptable for demo/test)
- Not suitable for production (would use PostgreSQL/MySQL)

### 2. Why Optimistic Locking?

**Decision**: Implement optimistic locking using `@Version` annotation.

**Rationale**:
- **Performance**: Better than pessimistic locking for read-heavy workloads
- **Scalability**: Allows concurrent reads
- **Conflict Detection**: Detects concurrent modifications
- **Spring Boot Support**: Built-in support via JPA

**Implementation**:
```java
@Entity
public class Item {
    @Version
    @Column(nullable = false)
    private Long version = 0L;
}
```

**How It Works**:
1. Each entity has a `version` field
2. On update, Hibernate checks if version matches
3. If version differs, throws `OptimisticLockingFailureException`
4. Service layer catches and returns 409 Conflict

### 3. Why Transaction Isolation Level REPEATABLE_READ?

**Decision**: Use `REPEATABLE_READ` isolation level for order creation.

**Rationale**:
- **Consistency**: Prevents dirty reads and non-repeatable reads
- **Stock Accuracy**: Ensures stock quantities are consistent during order processing
- **Prevents Race Conditions**: Multiple reads of the same data return the same value

**Implementation**:
```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public Order createOrder(CreateOrderRequest request) {
    // Stock checks and updates happen in this transaction
}
```

**Isolation Levels Explained**:
- **READ_UNCOMMITTED**: Lowest isolation, allows dirty reads
- **READ_COMMITTED**: Prevents dirty reads (default in most databases)
- **REPEATABLE_READ**: Prevents dirty reads and non-repeatable reads (our choice)
- **SERIALIZABLE**: Highest isolation, prevents all anomalies (too restrictive)

### 4. Why Two-Pass Order Processing?

**Decision**: Validate all order lines first, then update stock.

**Rationale**:
- **Fail-Fast**: Reject invalid orders before any changes
- **Atomicity**: All-or-nothing approach
- **Better Error Messages**: Can report all issues at once
- **Transaction Efficiency**: Only one transaction for the entire operation

**Implementation**:
```java
// First pass: Validate and check stock
for (OrderLineRequest lineReq : request.getLines()) {
    // Validate item exists
    // Check stock availability
    // Calculate totals
}

// Second pass: Update stock (only if all validations pass)
for (OrderLine line : orderLines) {
    // Reduce stock atomically
}
```

### 5. Why DTOs Instead of Entities in Controllers?

**Decision**: Always return DTOs from controllers, never entities.

**Rationale**:
- **Circular References**: Entities have bidirectional relationships that cause JSON serialization issues
- **Lazy Loading**: Entities may trigger lazy loading exceptions
- **API Stability**: DTOs provide a stable API contract
- **Security**: Control what data is exposed
- **Performance**: Only serialize what's needed

**Example Problem (Without DTOs)**:
```json
// Entity serialization causes infinite loop:
{
  "id": 1,
  "order": {
    "id": 1,
    "lines": [{
      "id": 1,
      "order": {
        "id": 1,
        // ... infinite recursion
      }
    }]
  }
}
```

**Solution (With DTOs)**:
```json
// Clean, controlled structure:
{
  "id": 1,
  "orderNumber": "ORD-123",
  "lines": [{
    "id": 1,
    "itemId": 1,
    "itemName": "T-Shirt"
    // No circular reference
  }]
}
```

### 6. Why Defer Data Source Initialization?

**Decision**: Set `defer-datasource-initialization: true` in application.yml.

**Rationale**:
- **Schema First**: Hibernate must create tables before `data.sql` runs
- **Order Matters**: SQL scripts need tables to exist
- **Spring Boot Behavior**: By default, `data.sql` runs before schema creation

**Configuration**:
```yaml
spring:
  jpa:
    defer-datasource-initialization: true
  sql:
    init:
      mode: always
```

---

## Data Model

### Entity Relationships

```
Item (1) ────────< (Many) ItemVariant
  │
  │ (Many)
  │
  ▼
OrderLine (Many) ────────< (1) Order
  │
  │ (Many)
  │
  ▼
ItemVariant (optional)
```

### Key Relationships

1. **Item → ItemVariant**: One-to-Many
   - An item can have multiple variants (e.g., sizes, colors)
   - Variant belongs to exactly one item

2. **Order → OrderLine**: One-to-Many
   - An order contains multiple line items
   - Each line item belongs to exactly one order

3. **OrderLine → Item**: Many-to-One
   - Each line item references one item
   - An item can appear in multiple order lines

4. **OrderLine → ItemVariant**: Many-to-One (Optional)
   - Line item can reference a variant (if item has variants)
   - Can be null if item doesn't have variants

### Database Constraints

**Unique Constraints**:
- `items.name`: Item names must be unique
- `item_variants.sku`: SKU codes must be unique
- `orders.orderNumber`: Order numbers must be unique

**Foreign Key Constraints**:
- `item_variants.item_id` → `items.id`
- `order_lines.order_id` → `orders.id`
- `order_lines.item_id` → `items.id`
- `order_lines.variant_id` → `item_variants.id`

**Indexes**:
- `idx_item_name`: Fast lookup by item name
- `idx_variant_sku`: Fast lookup by SKU
- `idx_variant_item`: Fast lookup of variants by item
- `idx_order_createdAt`: Fast sorting/filtering by date

---

## Transaction Management

### Transaction Strategy

**Default**: All service methods are `@Transactional` with default settings.

**Order Creation**: Uses `REPEATABLE_READ` isolation level for stock consistency.

### Transaction Flow for Order Creation

```
1. Begin Transaction (REPEATABLE_READ)
   │
2. Validate Order Lines
   ├─ Check item exists
   ├─ Check variant exists (if provided)
   ├─ Validate item-variant relationship
   └─ Check stock availability
   │
3. If validation fails → Rollback → Return Error
   │
4. If validation passes:
   ├─ Re-fetch entities (get latest version)
   ├─ Reduce stock quantities
   ├─ Create order entity
   ├─ Create order line entities
   └─ Save all entities
   │
5. Commit Transaction
   │
6. Return OrderDto
```

### Why Re-fetch Entities During Stock Update?

**Decision**: Re-fetch `Item` and `ItemVariant` entities before updating stock.

**Rationale**:
- **Latest Version**: Get the most recent version number for optimistic locking
- **Consistency**: Ensure we're working with the latest data
- **Race Condition Prevention**: If another transaction modified the entity, we'll detect it

**Implementation**:
```java
// Re-fetch to get latest version
ItemVariant variant = variantRepository.findById(variantId)
    .orElseThrow(...);
    
int newStock = variant.getStockQuantity() - quantity;
variant.setStockQuantity(newStock);
variantRepository.save(variant); // Optimistic lock check happens here
```

---

## Concurrency Control

### Optimistic Locking

**How It Works**:
1. Each entity has a `version` field (initialized to 0)
2. On every update, Hibernate increments the version
3. Before update, Hibernate checks if version matches database
4. If version differs → `OptimisticLockingFailureException`

**Handling**:
```java
try {
    variantRepository.save(variant);
} catch (OptimisticLockingFailureException e) {
    throw new OptimisticLockException(
        "Data was updated by another transaction. Please try again."
    );
}
```

**HTTP Response**: 409 Conflict with user-friendly message

### Why Not Pessimistic Locking?

**Decision**: Use optimistic locking instead of pessimistic locking.

**Rationale**:
- **Performance**: Pessimistic locking blocks other transactions
- **Deadlock Risk**: Pessimistic locks can cause deadlocks
- **Read-Heavy Workload**: Most operations are reads, not writes
- **Conflict Frequency**: Concurrent stock updates are relatively rare

**When to Use Pessimistic Locking**:
- High contention scenarios
- Critical financial transactions
- When conflicts are frequent

---

## Error Handling Strategy

### Exception Hierarchy

```
Exception (Java)
  │
  ├─ ResourceNotFoundException → 404 Not Found
  ├─ InsufficientStockException → 400 Bad Request
  ├─ OptimisticLockException → 409 Conflict
  ├─ IllegalArgumentException → 400 Bad Request
  ├─ IllegalStateException → 409 Conflict
  ├─ MethodArgumentNotValidException → 400 Bad Request (Validation)
  ├─ ConstraintViolationException → 400 Bad Request (DB Constraint)
  └─ Exception → 500 Internal Server Error (Catch-all)
```

### Error Response Format

```json
{
  "timestamp": "2025-11-30T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Not enough stock for T-Shirt. Available: 5, Requested: 10",
  "path": "/api/orders",
  "details": {
    "field": "validation error message"
  }
}
```

### Global Exception Handler

**Purpose**: Centralize error handling and provide consistent responses.

**Implementation**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(...) {
        // Return 404
    }
    
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(...) {
        // Return 400
    }
    
    // ... other handlers
}
```

**Benefits**:
- Consistent error format
- Centralized logging
- Easy to add new exception types
- Clean controller code

---

## API Design

### RESTful Principles

1. **Resource-Based URLs**: `/api/items`, `/api/variants`, `/api/orders`
2. **HTTP Methods**: GET (read), POST (create), PUT (update), DELETE (delete)
3. **Status Codes**: 200 (OK), 201 (Created), 204 (No Content), 400 (Bad Request), 404 (Not Found), 409 (Conflict), 500 (Server Error)
4. **JSON Format**: Request and response bodies use JSON

### API Endpoints

#### Items
- `GET /api/items` - List all items
- `GET /api/items/{id}` - Get item by ID
- `POST /api/items` - Create new item
- `PUT /api/items/{id}` - Update item
- `DELETE /api/items/{id}` - Delete item

#### Variants
- `GET /api/variants` - List all variants
- `GET /api/variants/{id}` - Get variant by ID
- `POST /api/variants` - Create new variant
- `PUT /api/variants/{id}` - Update variant
- `DELETE /api/variants/{id}` - Delete variant

#### Orders
- `GET /api/orders` - List all orders
- `GET /api/orders/{id}` - Get order by ID
- `POST /api/orders` - Create new order (with stock validation)

### Request/Response Examples

**Create Order Request**:
```json
{
  "lines": [
    {
      "itemId": 1,
      "variantId": 1,
      "quantity": 2
    },
    {
      "itemId": 2,
      "quantity": 1
    }
  ]
}
```

**Order Response**:
```json
{
  "id": 1,
  "orderNumber": "ORD-123456789ABC",
  "createdAt": "2025-11-30T12:00:00Z",
  "totalAmount": 370000.00,
  "lines": [
    {
      "id": 1,
      "itemId": 1,
      "itemName": "T-Shirt",
      "variantId": 1,
      "variantName": "Black - M",
      "quantity": 2,
      "unitPrice": 110000.00,
      "lineTotal": 220000.00
    }
  ]
}
```

---

## Security Considerations

### Current State (Development)

**Note**: This is a development/demo application. Production would require:

1. **Authentication**: JWT tokens, OAuth2, or session-based auth
2. **Authorization**: Role-based access control (RBAC)
3. **Input Validation**: Already implemented via `@Valid` annotations
4. **SQL Injection Prevention**: Handled by JPA/Hibernate
5. **CORS Configuration**: Configure allowed origins
6. **Rate Limiting**: Prevent abuse
7. **HTTPS**: Encrypt data in transit
8. **Sensitive Data**: Never log sensitive information

### Validation Layers

1. **Controller Layer**: `@Valid` annotations on request DTOs
2. **Entity Layer**: JPA validation annotations (`@NotNull`, `@NotBlank`, etc.)
3. **Service Layer**: Business rule validation
4. **Database Layer**: Unique constraints, foreign keys, check constraints

---

## Scalability Considerations

### Current Architecture (Single Instance)

The current design works well for:
- Small to medium workloads
- Single application instance
- In-memory database

### Scaling Strategies

**Horizontal Scaling**:
- **Stateless Design**: Controllers are stateless (good for scaling)
- **Database**: Move from H2 to PostgreSQL/MySQL with connection pooling
- **Load Balancer**: Add load balancer for multiple instances
- **Session Management**: Use external session store (Redis)

**Database Scaling**:
- **Read Replicas**: Use read replicas for GET requests
- **Connection Pooling**: Configure HikariCP for optimal connections
- **Caching**: Add Redis cache for frequently accessed items
- **Partitioning**: Partition large tables if needed

**Performance Optimizations**:
- **Lazy Loading**: Already using `FetchType.LAZY` for relationships
- **Pagination**: Add pagination for list endpoints
- **Batch Operations**: Use batch inserts for bulk operations
- **Async Processing**: Move order processing to async queue for high volume

### Monitoring and Observability

**Current**:
- SLF4J logging throughout services
- Error logging in exception handlers

**Production Recommendations**:
- **Application Metrics**: Micrometer + Prometheus
- **Distributed Tracing**: Spring Cloud Sleuth / Zipkin
- **Health Checks**: Spring Boot Actuator
- **Log Aggregation**: ELK Stack or similar

---

## Testing Strategy

### Unit Tests
- **Service Layer**: Test business logic with mocked repositories
- **Controller Layer**: Test HTTP layer with MockMvc

### Integration Tests
- **Repository Layer**: Test database operations with test database
- **End-to-End**: Test complete request/response flow

### Test Coverage
- Happy path scenarios
- Error scenarios (not found, validation failures)
- Edge cases (zero stock, concurrent updates)
- Boundary conditions (negative quantities, null values)

---

## Future Enhancements

### Potential Improvements

1. **Caching**: Add Redis cache for frequently accessed items
2. **Event-Driven**: Publish domain events (order created, stock low)
3. **Search**: Add full-text search for items
4. **Reporting**: Add analytics and reporting endpoints
5. **Multi-tenancy**: Support multiple warehouses/tenants
6. **Audit Trail**: Track all changes with audit logs
7. **Soft Deletes**: Mark items as deleted instead of hard delete
8. **API Versioning**: Support multiple API versions
9. **GraphQL**: Add GraphQL endpoint for flexible queries
10. **WebSocket**: Real-time stock updates

---

## Conclusion

This architecture provides:
- ✅ **Separation of Concerns**: Clear layer boundaries
- ✅ **Testability**: Easy to unit test and integration test
- ✅ **Maintainability**: Clean, organized code structure
- ✅ **Scalability**: Can scale horizontally
- ✅ **Reliability**: Transaction safety and error handling
- ✅ **Performance**: Optimized queries and lazy loading
- ✅ **Security**: Input validation and error handling

The design follows Spring Boot best practices and industry standards for RESTful API development.

