# Real Estate CRM

Submission-ready full-stack real estate CRM built with Spring Boot, MySQL, and a Spring-served HTML/CSS/JavaScript frontend.

## Highlights

- Spring Boot + Maven backend with layered architecture
- MySQL persistence with JPA/Hibernate and auto schema update
- JWT authentication with `ADMIN` and `AGENT` roles
- India-focused CRM UI with responsive dashboard and admin screens
- Leads, properties, clients, brokers, and bookings modules
- Booking payment tracking and export support
- Swagger API documentation
- Seed data for Indian cities and demo accounts
- Integration tests for auth, properties, brokers, and bookings

## Modules

### Authentication

- Login with JWT
- Role-based access for admin and broker users
- `/api/auth/login`
- `/api/auth/me`

### Leads

- Create, update, list, delete leads
- Filter by stage, source, broker, and follow-up date
- Convert a lead into a client
- Export leads to CSV

### Properties

- CRUD operations
- Assign broker to property
- Filter by city, locality, type, configuration, featured flag, price, and status
- Pagination support

### Clients

- CRUD operations
- Preferred locality tracking
- Booking relationship view
- Export clients to CSV

### Brokers

- CRUD operations for broker users
- Broker alias APIs for presentation-friendly terminology
- Property, booking, and lead counts per broker

### Bookings

- CRUD operations
- Property/client/broker relationship handling
- Pending, confirmed, and cancelled status tracking
- Payment tracking with booking amount, amount paid, status, payment date, and reference
- Validation for single confirmed booking per property
- Export bookings to Excel

### Dashboard

- Total counts for leads, properties, clients, and bookings
- Inventory valuation
- Booking conversion metrics
- Collections and outstanding payments
- Property type and city mix
- Lead stage and source mix
- Top broker leaderboard
- Upcoming bookings and lead follow-ups

## Tech Stack

- Java 17
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- MySQL
- Maven
- OpenAPI / Swagger
- HTML, CSS, JavaScript
- Apache POI
- JUnit + MockMvc

## Project Structure

```text
src/main/java/com/crm
  config
  controller
  dto
  entity
  exception
  repository
  security
  service

src/main/resources
  static
  application.properties
```

## Database Setup

Create the database in MySQL:

```sql
CREATE DATABASE real_estate_crm;
```

Create the MySQL user if needed:

```sql
CREATE USER IF NOT EXISTS 'real_estate_crm'@'localhost' IDENTIFIED BY 'puran';
GRANT ALL PRIVILEGES ON real_estate_crm.* TO 'real_estate_crm'@'localhost';
FLUSH PRIVILEGES;
```

Current defaults are configured in:

- [/Users/puranjaijain/IdeaProjects/CRM/src/main/resources/application.properties](/Users/puranjaijain/IdeaProjects/CRM/src/main/resources/application.properties)

## Run Locally

```bash
mvn spring-boot:run
```

Open:

- [http://localhost:8080](http://localhost:8080)
- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8080/presentation.html](http://localhost:8080/presentation.html)

## Demo Accounts

- Admin: `admin@realestatecrm.com / admin123`
- Broker: `aarav@realestatecrm.com / agent123`
- Broker: `diya@realestatecrm.com / agent123`
- Broker: `vikram@realestatecrm.com / agent123`

## Business Rules

- Only one confirmed booking is allowed for a property
- Confirmed bookings automatically mark the property as sold
- Deleting or changing bookings re-syncs property availability
- Booking amount must be greater than zero
- Amount paid cannot exceed booking amount
- Lead conversion creates a client and marks the lead as converted

## Testing

Run the test suite:

```bash
mvn test
```

Current integration tests cover:

- Auth login and current user
- Broker alias API
- Property create and filter behavior
- Booking conflict rules

## Suggested Viva Flow

1. Open the login page and explain role-based access.
2. Show the dashboard and explain the analytics cards.
3. Open Leads and show follow-up workflow plus lead conversion.
4. Open Properties and explain search/filter features.
5. Open Bookings and show payment tracking plus confirmation rules.
6. Show Swagger and the export buttons.

## Future Scope

- Document upload and management
- Lead reminders and notification system
- Payment receipt generation
- Audit trail and activity timeline
- Charts with richer visual analytics
- Email or WhatsApp follow-up integration
