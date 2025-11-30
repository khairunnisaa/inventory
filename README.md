# Warehouse Management API

A simple RESTful API for managing a shop warehouse inventory, built with Java 17 and Spring Boot 3.

## 📚 Documentation

- **[Architecture Documentation](./ARCHITECTURE.md)**: Comprehensive guide to the system architecture, design decisions, and patterns used

## Tech Stack

- Java 17
- Spring Boot 3.3.0
- Spring Web
- Spring Data JPA
- H2 in-memory database
- Maven
- Lombok
- Spring Validation

## How to Run

### Prerequisites

- Java 17 or higher
- Maven 3.8 or higher

### Steps

1. Clone the repository:
```bash
git clone https://github.com/khairunnisaa/inventory.git
cd inventory
```

2. Build and run the application:
```bash
mvn clean spring-boot:run
```

The application will start on `http://localhost:8080`.

3. Access H2 Console (optional):
   - Navigate to `http://localhost:8080/h2-console`
   - JDBC URL: `jdbc:h2:mem:warehouse-db`
   - User: `sa`
   - Password: (leave empty)

## API Overview

### Items

#### List all items
```
GET /api/items
```

#### Get item by ID
```
GET /api/items/{id}
```

#### Create new item
```
POST /api/items
Content-Type: application/json

{
  "name": "Coffee Beans",
  "description": "250g medium roast",
  "basePrice": 150000,
  "stockQuantity": 20,
  "hasVariants": false
}
```

#### Update item
```
PUT /api/items/{id}
Content-Type: application/json

{
  "name": "Coffee Beans",
  "description": "250g medium roast",
  "basePrice": 160000,
  "stockQuantity": 25,
  "hasVariants": false
}
```

#### Delete item
```
DELETE /api/items/{id}
```

### Item Variants

#### List all variants
```
GET /api/variants
```

#### Get variant by ID
```
GET /api/variants/{id}
```

#### Create new variant
```
POST /api/variants
Content-Type: application/json

{
  "item": {
    "id": 1
  },
  "sku": "TSHIRT-BLACK-M",
  "name": "Black - M",
  "price": 110000,
  "stockQuantity": 5
}
```

#### Update variant
```
PUT /api/variants/{id}
Content-Type: application/json

{
  "item": {
    "id": 1
  },
  "sku": "TSHIRT-BLACK-M",
  "name": "Black - M",
  "price": 115000,
  "stockQuantity": 3
}
```

#### Delete variant
```
DELETE /api/variants/{id}
```

### Orders

#### List all orders
```
GET /api/orders
```

#### Get order by ID
```
GET /api/orders/{id}
```

#### Create order (with stock validation)
```
POST /api/orders
Content-Type: application/json

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

**Note:** If any requested quantity is greater than available stock, the API will respond with `400 Bad Request` and will not reduce any stock. The transaction is atomic - either all items are sold or none.

## Design Decisions

### Architecture

**Layered Architecture**: The application follows a classic layered architecture pattern:
- **Controller Layer**: Handles HTTP requests/responses, input validation
- **Service Layer**: Contains business logic (stock validation, order processing)
- **Repository Layer**: Data access using Spring Data JPA
- **Entity Layer**: JPA entities representing database tables

This separation ensures:
- Clear separation of concerns
- Easy testing of each layer independently
- Maintainability and scalability

### Data Model

**Item & ItemVariant Model**:
- `Item` represents the main product with optional base price and stock
- `ItemVariant` represents variant combinations (e.g., size, color) with their own price and stock
- Items can have variants (`hasVariants = true`) or be sold directly without variants

**Order & OrderLine Model**:
- `Order` represents a sale transaction with a unique order number
- `OrderLine` represents individual items/variants in an order
- This structure provides a natural place to implement "prevent selling out-of-stock" logic

### Stock Management

**Stock Validation**:
- Implemented in the `OrderService` layer
- Two-phase approach:
  1. **Validation Phase**: Check all order lines for stock availability
  2. **Execution Phase**: If all validations pass, atomically reduce stock
- If any item/variant is out of stock or quantity is insufficient, an `InsufficientStockException` is thrown and the transaction is rolled back
- Uses `@Transactional` to ensure atomicity - either all stock is reduced or none

### Error Handling

**Custom Exceptions**:
- `ResourceNotFoundException`: For missing resources (404)
- `InsufficientStockException`: For stock-related errors (400)

**Global Exception Handler**:
- `@ControllerAdvice` provides centralized exception handling
- Returns consistent error response format with HTTP status codes
- Handles validation errors from `@Valid` annotations

### Technology Choices

- **H2 In-Memory Database**: Easy setup, no external dependencies, perfect for development and testing
- **Lombok**: Reduces boilerplate code (getters, setters, constructors, builders)
- **Spring Data JPA**: Simplifies database operations with repository pattern
- **Spring Validation**: Provides declarative validation on DTOs

## Assumptions

1. **No Authentication/Authorization**: Only basic CRUD is required; no security layer implemented
2. **Currency**: Price currency is not modeled explicitly; assumed to be the shop's default currency
3. **Variant Relationship**: Variants belong to exactly one item (ManyToOne relationship)
4. **Order Processing**: No partial shipment/backorder support. If stock is insufficient for any line, the whole order is rejected
5. **Item Base Price**: Items with variants may not have a base price (price is determined by variant)
6. **Stock Tracking**: Stock is tracked separately for items and variants. Items with variants should not use base item stock
7. **Order Numbers**: Generated using UUID for uniqueness

## Database Schema

### Items Table
- `id` (Long, Primary Key)
- `name` (String, Unique, Not Null)
- `description` (String)
- `base_price` (BigDecimal)
- `stock_quantity` (Integer, Not Null, Default: 0)
- `has_variants` (Boolean, Not Null, Default: false)

### Item Variants Table
- `id` (Long, Primary Key)
- `item_id` (Long, Foreign Key to Items)
- `sku` (String, Unique, Not Null)
- `name` (String, Not Null)
- `price` (BigDecimal, Not Null)
- `stock_quantity` (Integer, Not Null, Default: 0)

### Orders Table
- `id` (Long, Primary Key)
- `order_number` (String, Unique, Not Null)
- `created_at` (Instant, Not Null)
- `total_amount` (BigDecimal, Not Null)

### Order Lines Table
- `id` (Long, Primary Key)
- `order_id` (Long, Foreign Key to Orders)
- `item_id` (Long, Foreign Key to Items)
- `variant_id` (Long, Foreign Key to Item Variants, Nullable)
- `quantity` (Integer, Not Null)
- `unit_price` (BigDecimal, Not Null)
- `line_total` (BigDecimal, Not Null)

## Sample Data

The application comes with sample data pre-loaded:
- T-Shirt (with variants: Black-M, Black-L, White-M, White-L)
- Coffee Beans (no variants)
- Laptop (no variants)

## Testing

Run tests with:
```bash
mvn test
```

The test suite includes:
- Unit tests for `ItemService` (CRUD operations)
- Unit tests for `OrderService` (stock validation, stock reduction)
- Integration tests for `OrderController` (end-to-end order creation)

## Implemented Features

The following features are **already implemented** in this solution:

✅ **Comprehensive Validation**: 
   - Bean validation annotations (`@NotNull`, `@NotBlank`, `@Positive`, `@Size`, etc.)
   - Custom business rule validation in service layer
   - Database-level constraints (unique, foreign keys, check constraints)

✅ **Logging**: 
   - SLF4J logging throughout all service layers
   - Error logging in exception handlers
   - Debug, info, warn, and error level logging

✅ **Error Handling**: 
   - Custom exceptions with appropriate HTTP status codes
   - Global exception handler with consistent error response format
   - Detailed validation error messages

✅ **Optimistic Locking**: 
   - Version-based concurrency control
   - Prevents race conditions during stock updates

✅ **Transaction Management**: 
   - Atomic order processing with `@Transactional`
   - `REPEATABLE_READ` isolation level for stock consistency

## Possible Future Enhancements

The following features are **not implemented** but could be added for production use:

1. **Pagination and Filtering**: Add pagination and filtering on list endpoints (e.g., `GET /api/items?page=0&size=20&sort=name`)
2. **Authentication/Authorization**: Add Spring Security for API protection (JWT, OAuth2, or session-based)
3. **Audit Fields**: Add `createdBy`, `updatedBy`, `createdAt`, `updatedAt` fields to track changes
4. **Structured Error Codes**: Add error codes (e.g., `ERR_STOCK_INSUFFICIENT`) for better client error handling
5. **API Documentation**: Add Swagger/OpenAPI documentation for interactive API exploration
6. **Caching**: Add Redis cache for frequently accessed items to improve performance
7. **Soft Delete**: Implement soft delete (mark as deleted) instead of hard delete for data retention
8. **Stock History**: Track stock changes over time for auditing and reporting
9. **Event-Driven Architecture**: Publish domain events (order created, stock low) for integration
10. **Search Functionality**: Add full-text search for items by name or description

## License

This project is created for assessment purposes.

