# StadiumEats 🏟️🍔

> **Food ordering system for football stadium spectators**  
> Order from your seat. We deliver to you.

---

## Table of Contents

1. [Team Members](#team-members)
2. [Project Description](#project-description)
3. [Requirements](#requirements)
4. [Prerequisites & Installation](#prerequisites--installation)
5. [How to Run](#how-to-run)
6. [Frontend Screens](#frontend-screens)
7. [API Reference](#api-reference)
8. [Architecture](#architecture)
9. [Design Patterns](#design-patterns)
10. [SOLID Principles](#solid-principles)
11. [Security Measures](#security-measures)
12. [Difficulties & Solutions](#difficulties--solutions)
13. [Conclusion](#conclusion)

---

## Team Members

| # | Name | Role |
|---|------|------|
| 1 | [ILIASS MOUCHRIF] | Chef de projet
| 2 | [YOUSSEF LAARYECH] | Backend Auth |
| 3 | [WALID LIDRISSI] | Backend Commandes|
| 4 | [EL HOUSSAINI MOHAMED TAHA] | Frontend |

---

## Project Description

**StadiumEats** is a full-stack Java web application that allows football stadium spectators to order food directly from their seat via a browser. Workers receive the orders on their dashboard and deliver them to the correct seat. Administrators manage the menu, monitor all orders, and view registered users.

The application is built as a **Single-Page Application (SPA)** — all navigation happens in one HTML file without page reloads. It runs on **Apache Tomcat 9** using **Jakarta Servlets**, with a **MySQL 8** database accessed exclusively through **JDBC PreparedStatements**.

### Three User Roles

| Role | Capabilities |
|------|-------------|
| **CLIENT** | Browse menu, add items to cart, place orders (online card payment or cash on delivery), track live order status |
| **WORKER** | View pending orders, accept delivery (→ IN_DELIVERY), mark as delivered, auto-refreshing dashboard |
| **ADMIN** | Full menu CRUD, view & filter all orders, change order status, view all registered users |

---

## Requirements

### User Stories

- **As a CLIENT**, I want to browse the menu and add items to a cart, so that I can conveniently place food orders from my stadium seat.
- **As a CLIENT**, I want to track the status of my order, so that I know exactly when my food will arrive.
- **As a WORKER**, I want a live dashboard of pending orders, so that I can efficiently deliver food straight to the client's seat.
- **As an ADMIN**, I want to manage menu items and monitor all orders, so that I maintain complete oversight over stadium sales.

### MoSCoW Prioritization

- **Must Have**: Real-time order cart, Secure login system, Order assignment to seating, Three distinct role interfaces (Client, Worker, Admin).
- **Should Have**: Online credit card form mock-up, Live worker dashboard refresh polling.
- **Could Have**: In-depth sales analytical charts, SMS delivery tracking.
- **Won't Have**: External third-party payment gateway integration (Stripe/PayPal), physical ticket scanning.

### Acceptance Criteria

- **Authentication**: Users must securely log into their accounts; passwords must be hashed using BCrypt. Unauthenticated users cannot view the menu or place orders.
- **Ordering System**: A client must successfully be able to create an order designated for their seat. The application MUST gracefully reject empty orders or malformed requests.
- **Worker Dashboard**: A logged-in worker must organically see newly created pending orders pop up without hard page reloads and transition their states reliably to `IN_DELIVERY` then `DELIVERED`.
- **Admin Tools**: The application allows the admin absolute permission to modify database menu records (CRUD) and dynamically override states for any active order.

---

## Prerequisites & Installation

### Common Requirements

- **Java 11** (JDK)
- **Apache Tomcat 9** (Jakarta Servlet 4.0 / `javax.servlet`)
- **MySQL 8.0**
- **Maven 3.8+**

### Windows Setup

```powershell
# 1. Install Java 11
winget install Microsoft.OpenJDK.11

# 2. Install Maven
winget install Apache.Maven

# 3. Verify installations
java -version
mvn -version

# 4. Download Apache Tomcat 9
# https://tomcat.apache.org/download-90.cgi
# Extract to C:\tomcat9

# 5. Setup MySQL database
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/data.sql
```

### Linux / macOS Setup

```bash
# Ubuntu/Debian
sudo apt install openjdk-11-jdk maven mysql-server

# macOS (Homebrew)
brew install openjdk@11 maven mysql

# Setup database
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/data.sql
```

---

## How to Run

### Step 1 — Configure Database

Edit `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/stadiumeats?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.username=root
db.password=yourpassword
```

### Step 2 — Build the WAR

```bash
cd StadiumEats
Update the frontend of my project with the following requirements:

1. MENU UPDATE:
- Replace the current menu categories with:
  - Burger
  - Tacos
  - Pizza
  - Coke
  - Coffee
  - Tea

- Ensure:
  - Proper spelling and consistent naming (e.g., "Pizza" not "Pizaa", "Coke" not "Coce").
  - Each category is displayed clearly in the navigation/menu UI.
  - Icons or images (if used) match each category.
  - The layout remains responsive and visually balanced.

2. CURRENCY UPDATE:
- Change the currency across the entire project to Moroccan Dirham (MAD / DH).
- Apply this globally:
  - Product prices
  - Cart
  - Checkout
  - Invoices / summaries
- Format prices like:
  - 50 DH
  - 120 MAD (choose one format and keep it consistent)

3. CONSISTENCY:
- Ensure all components (frontend + backend if needed) use the same currency.
- Update any hardcoded values or formatting functions.
- Avoid mixing currencies.

4. QUALITY:
- Do not break existing functionality.
- Keep clean, maintainable code.
- Follow best UI/UX practices.

Return the updated code with clear explanations of what was changed.
# Output: target/stadiumeats.war
```

### Step 3 — Deploy to Tomcat 9

```bash
# Copy WAR to Tomcat webapps
cp target/stadiumeats.war /path/to/tomcat9/webapps/

# Start Tomcat
/path/to/tomcat9/bin/startup.sh      # Linux/macOS
C:\tomcat9\bin\startup.bat           # Windows
```

### Step 4 — Access the Application

Open your browser: **http://localhost:8080/stadiumeats/**

### Default Seed Credentials

| Username | Password | Role |
|----------|----------|------|
| `admin` | `password123` | ADMIN |
| `worker1` | `password123` | WORKER |
| `client1` | `password123` | CLIENT |

---

## Frontend Screens

### Screen 1 — Login / Register
Toggle between Login and Register forms. On success the user object is stored in memory and `sessionStorage`, and the user is redirected to their role-specific view. The active tab (Login vs Register) resets to Login on logout.

### Screen 2 — Client: Menu & Cart
- Hero banner with stadium image at the top
- Menu items fetched from `GET /api/menu` and rendered in a responsive CSS grid
- Each card shows image, category badge, name, description, and price with an **+ Add** button
- Cart items are tracked in memory; the navbar Cart button shows a real-time item count badge
- **Cart page** lists all items with quantity controls (+ / −) and a remove button
- Seat number input + payment method selector (Online / Cash on Delivery)
- **Cash on Delivery**: order is posted directly with `"paymentMethod": "CASH"`
- **Online Payment**: opens a credit-card modal with:
  - Live animated card preview (number, name, expiry)
  - Auto-formatted card number (`XXXX XXXX XXXX XXXX`)
  - Inline field validation (16-digit number, MM/YY format, non-expired, 3-4 digit CVV)
  - Loading spinner on the Pay Now button during submission
  - Error displayed inside the modal on failure

### Screen 3 — Client: My Orders
List of the client's own orders with color-coded status badges, item breakdown, seat number, total, and timestamp.

### Screen 4 — Worker Dashboard
- Stat cards: Pending / In Delivery / Delivered today
- Two columns: Pending Orders | Active Deliveries
- **Accept Order** → sets status to `IN_DELIVERY`
- **Mark Delivered** → sets status to `DELIVERED`
- Auto-refreshes every 30 seconds
- Dedicated navbar tab **📦 Orders**

### Screen 5 — Admin Panel
Three tabs accessible from the navbar:

| Tab | Features |
|-----|----------|
| **🍔 Menu** | Table of all items with Edit (modal) and Delete buttons; Add New Item form |
| **📋 Orders** | All orders with filter bar (All / Pending / Confirmed / In Delivery / Delivered / Cancelled), live text search by Order ID or username, and per-order status dropdown |
| **👥 Users** | Read-only table of all registered users with role badges |

### Global UI Elements
- Fixed navbar with logo, username, role badge, role-specific tabs, and Logout
- Active navbar tab always highlighted in green (matching accent color)
- Loading spinner overlay during every API call
- Green success toast and red error toast (bottom-right) with slide-in animation
- Smooth page transitions (fade + slide) between screens
- Fully responsive layout for mobile, tablet, and desktop

---

## API Reference

All endpoints are under context path `/stadiumeats/api`. Session cookie is required for all protected routes.

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| `POST` | `/api/auth/login` | Public | Authenticate user, start session |
| `POST` | `/api/auth/register` | Public | Create new CLIENT account |
| `POST` | `/api/auth/logout` | Authenticated | Invalidate session |
| `GET` | `/api/auth/users` | ADMIN | List all users |
| `GET` | `/api/menu` | Authenticated | Fetch all available menu items |
| `POST` | `/api/menu` | ADMIN | Create a new menu item |
| `PUT` | `/api/menu/{id}` | ADMIN | Update an existing menu item |
| `DELETE` | `/api/menu/{id}` | ADMIN | Remove a menu item |
| `POST` | `/api/orders` | CLIENT | Place a new order |
| `GET` | `/api/orders` | Authenticated | Get orders (filtered by role) |
| `PUT` | `/api/orders/{id}/status` | WORKER / ADMIN | Update order status |

### Order Status Flow

```
PENDING → CONFIRMED → IN_DELIVERY → DELIVERED
                    ↘ CANCELLED
```

---

## Architecture

```
┌────────────────────────────────────────────────────────┐
│                     BROWSER (SPA)                      │
│              index.html — Vanilla JS                   │
│  Login │ Menu+Cart │ My Orders │ Worker │ Admin        │
│  + Credit Card Modal  + Admin Filter Bar               │
└──────────────────────┬─────────────────────────────────┘
                       │  HTTP / JSON  (credentials: include)
                       ▼
┌────────────────────────────────────────────────────────┐
│              TOMCAT 9 (Servlet Container)              │
│                                                        │
│  ┌────────────────────────────────────────────────┐   │
│  │              CorsFilter (/api/*)               │   │
│  └────────────────────┬───────────────────────────┘   │
│                       │                                │
│  ┌─────────────────────────────────────────────────┐  │
│  │           BaseApiServlet (Template Method)      │  │
│  │  ┌───────────┐ ┌────────────┐ ┌─────────────┐  │  │
│  │  │AuthServlet│ │MenuServlet │ │OrderServlet │  │  │
│  │  └─────┬─────┘ └─────┬──────┘ └──────┬──────┘  │  │
│  └────────┼─────────────┼───────────────┼──────────┘  │
│           │             │               │              │
│  ┌────────▼─────────────▼───────────────▼──────────┐  │
│  │        Services (Facade Pattern)                │  │
│  │   AuthService          OrderService             │  │
│  │                     + PaymentStrategy           │  │
│  └────────────────────────┬────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────▼────────────────────────┐  │
│  │           DAO Layer (DAO Pattern)               │  │
│  │  UserDAOImpl  MenuItemDAOImpl  OrderDAOImpl     │  │
│  └────────────────────────┬────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────▼────────────────────────┐  │
│  │    ConnectionFactory (Singleton Pattern)        │  │
│  └────────────────────────┬────────────────────────┘  │
└──────────────────────────┬─────────────────────────────┘
                           │  JDBC / PreparedStatement
                           ▼
┌────────────────────────────────────────────────────────┐
│                    MySQL 8.0                           │
│  users │ menu_items │ orders │ order_items            │
└────────────────────────────────────────────────────────┘
```

---

## Design Patterns

### 1. Singleton — `ConnectionFactory`
Thread-safe double-checked locking with a `volatile` instance field ensures only one DB connection pool exists for the entire application lifetime.  
**Location:** `util/ConnectionFactory.java`

### 2. DAO Pattern — `GenericDAO<T>` + impl per entity
Each entity has an interface (`UserDAO`, `MenuItemDAO`, `OrderDAO`) extending `GenericDAO<T>`. Services always depend on interfaces, never on concrete implementations (enforcing DIP).  
**Location:** `dao/` and `dao/impl/`

### 3. Facade — `AuthService` and `OrderService`
Hide the complexity of multiple DAO calls and business rules behind single service methods. `OrderService.placeOrder()` orchestrates menu validation, payment strategy selection, and transactional persistence in one call.  
**Location:** `service/`

### 4. Template Method — `BaseApiServlet`
Defines the API skeleton: read body → deserialize → role-check → business logic → write JSON. Subclasses (`AuthServlet`, `MenuServlet`, `OrderServlet`) only override the handler methods.  
**Location:** `servlet/BaseApiServlet.java`

### 5. Strategy — `PaymentStrategy`
`OrderService` selects between `OnlinePaymentStrategy` and `CashPaymentStrategy` at runtime based on the order's `paymentMethod` field. Adding a new payment method requires only a new class — `OrderService` is never modified (OCP).  
**Location:** `service/PaymentStrategy.java`, `OnlinePaymentStrategy.java`, `CashPaymentStrategy.java`

---

## SOLID Principles

| Principle | How We Applied It |
|-----------|------------------|
| **S** – Single Responsibility | `AuthService` handles auth only; `OrderService` handles orders only; `ConnectionFactory` manages DB connections only; each Servlet handles one resource |
| **O** – Open/Closed | New payment method = new `PaymentStrategy` impl, zero changes to `OrderService` |
| **L** – Liskov Substitution | `UserDAOImpl`, `MenuItemDAOImpl`, `OrderDAOImpl` each fully satisfy their interface contract and can replace any DAO reference without side effects |
| **I** – Interface Segregation | `GenericDAO<T>` exposes only `findById`, `findAll`, `save`, `update`, `delete`; order-specific queries live in `OrderDAO` only |
| **D** – Dependency Inversion | `OrderService` receives `OrderDAO` and `MenuItemDAO` via constructor injection — it never calls `new OrderDAOImpl()` directly |

---

## Security Measures

| Measure | Where Applied |
|---------|--------------| 
| **BCrypt password hashing** | `AuthService.register()` and `login()` via jBCrypt — plain text is never stored or compared |
| **PreparedStatement everywhere** | All 3 DAO implementations — zero string concatenation in any SQL query |
| **Password never returned** | `passwordHash` field is `transient`; Gson skips it on every response |
| **Role-based access control** | `BaseApiServlet.requireRole()` called at the top of every sensitive handler before any logic executes |
| **No stack traces exposed** | All catch blocks respond with a generic `{"error": "..."}` message — internal details never leak |
| **Session-based authentication** | `HttpSession` with 60-minute timeout; `HttpOnly` cookie flag prevents JS access to session token |
| **CORS filter** | `CorsFilter` adds security headers only on `/api/*` routes and short-circuits `OPTIONS` preflight |
| **Frontend role isolation** | Each role sees only its own navbar and views; all sensitive actions are also enforced server-side |

---

## Difficulties & Solutions

| Difficulty | Solution |
|------------|----------|
| **Transactional order + items save** | Used `Connection.setAutoCommit(false)` + `rollback()` in `OrderDAOImpl.saveWithItems()` to ensure atomicity |
| **Role-filtered API responses** | Single `GET /api/orders` endpoint; server reads the session role and filters: CLIENT → own orders, WORKER → pending/active, ADMIN → all |
| **Strategy pattern for payment** | `PaymentStrategy` interface with `OnlinePaymentStrategy` and `CashPaymentStrategy`; easily extensible to new methods without changing `OrderService` |
| **Singleton thread safety** | Double-checked locking with `volatile` on `ConnectionFactory.instance` prevents race conditions at startup |
| **Password serialization** | `transient` modifier on `User.passwordHash` makes Gson skip it automatically on all responses |
| **CORS for SPA same-origin** | `CorsFilter` handles `OPTIONS` preflight; `BaseApiServlet.service()` short-circuits `OPTIONS` before any routing |
| **Active navbar tab stuck on Cart** | Refactored CSS: Cart button now uses the same neutral-default / green-active pattern as all other tabs; `setNavActive()` explicitly removes `.active` from all buttons before adding it to the target |
| **Logout not working** | Replaced the broken `updateCartUI()` call with `updateCartBadge()`; logout now hides all role-specific nav buttons using the shared `NAV_BTNS` array and calls `switchAuthTab('login')` to reset the form |
| **Credit card modal UX** | Built a live card preview that mirrors user input in real time; validation runs before the API call so the backend is never hit with invalid data |
| **Admin orders table fragile HTML** | Replaced the brittle `.replace()` hack with a dedicated `renderAdminOrderCard()` function that generates clean, self-contained HTML for each row |

---

## Conclusion

**StadiumEats** demonstrates a clean, layered Java web architecture using only standard Jakarta Servlets and JDBC — no ORM, no framework magic. Every design pattern is applied where it provides genuine value: Singleton for shared resources, DAO for data isolation, Facade for business logic encapsulation, Template Method for HTTP boilerplate elimination, and Strategy for payment extensibility.

The dark-themed SPA frontend delivers a polished experience across all three user roles — complete with a live credit-card payment modal, role-aware navigation, real-time toast feedback, filter/search on the admin orders panel, and an auto-refreshing worker dashboard.

> **Deploy:** `mvn clean package` → copy `target/stadiumeats.war` to Tomcat 9 `webapps/` → open `http://localhost:8080/stadiumeats/`
