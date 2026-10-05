# QueueLess – Hybrid Queue Management System

**Your Turn. Without the Wait.**

QueueLess is a full-stack Java web application that combines online and on-site registrations into one centralized, database-backed queue. Customers receive server-generated tokens and can track their position without refreshing the page. Staff manage services, walk-ins, queue actions and live status from a responsive dashboard.

## 1. Features

- Online customer registration
- Staff walk-in / physical registration
- One centralized queue per service
- Server-side, transaction-safe token generation
- Independent queues for multiple services
- Token counters that never reuse cancelled/skipped numbers
- Live queue tracking using Fetch/AJAX polling
- Staff login using Jakarta Servlet sessions
- BCrypt password hashing
- Service create, edit and deactivate management
- Call Next, Complete, Skip and Cancel actions
- Responsive production-style UI
- Empty/loading/success/error states
- MySQL + JDBC + prepared statements
- Maven WAR deployable to Apache Tomcat 11
- JSP token page (`token.jsp`) populated by a Servlet

## 2. Technology

- Frontend: HTML5, CSS3, Vanilla JavaScript, Fetch API
- Backend: Java 21+, Jakarta Servlets, JSP, JDBC
- Server: Apache Tomcat 11
- Database: MySQL 8+
- Build: Maven
- Passwords: BCrypt

## 3. Project Structure

```text
QueueLess/
├── pom.xml
├── README.md
├── database/
│   └── queueless.sql
└── src/main/
    ├── java/com/queueless/
    │   ├── controller/
    │   ├── dao/
    │   ├── model/
    │   └── util/
    ├── resources/
    │   └── db.properties.example
    └── webapp/
        ├── index.html
        ├── register.html
        ├── token.jsp
        ├── queue-status.html
        ├── login.html
        ├── staff-dashboard.html
        ├── css/style.css
        ├── js/
        └── WEB-INF/web.xml
```

## 4. Requirements

Install:

1. JDK 21 or newer
2. Apache Maven 3.9+
3. MySQL 8+
4. Apache Tomcat 11
5. A modern browser

Verify:

```bash
java -version
mvn -version
mysql --version
```

## 5. Database Setup

Open MySQL Workbench or MySQL command line and run:

```sql
SOURCE /absolute/path/to/QueueLess/database/queueless.sql;
```

Or paste the contents of `database/queueless.sql` into MySQL Workbench and execute it.

The script intentionally creates **zero services, zero customers, zero queue entries and zero counters**. It only creates the required schema.

## 6. Configure Database Credentials

Recommended: use environment variables.

Windows PowerShell example:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/queueless?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
```

For local IDE use, you may copy:

```text
src/main/resources/db.properties.example
```

to:

```text
src/main/resources/db.properties
```

and set:

```properties
db.url=jdbc:mysql://localhost:3306/queueless?useSSL=false&serverTimezone=UTC
db.username=YOUR_MYSQL_USERNAME
db.password=YOUR_MYSQL_PASSWORD
```

`db.properties` is ignored by Git. Never commit real credentials.

## 7. Create the First Staff/Admin Account

There are no fake users in the database. The included setup endpoint is protected by a one-time-style server-side setup key that you provide through an environment variable.

Set a strong setup key before starting Tomcat.

PowerShell:

```powershell
$env:QUEUELESS_SETUP_KEY="change-this-to-a-long-random-key"
```

Start the application, then send a POST request to:

```text
http://localhost:8080/QueueLess/api/setup-user
```

Form fields:

```text
setupKey=change-this-to-a-long-random-key
name=QueueLess Admin
email=admin@example.com
phone=9876543210
password=ChangeMe123!
role=ADMIN
```

You can use Postman or a REST client. After the account is created, the setup endpoint cannot be used unless the same server-side key is supplied. Do not expose the key publicly.

For a staff account, change `role=STAFF`.

## 8. Build the WAR

From the project root:

```bash
mvn clean package
```

The output is:

```text
target/QueueLess.war
```

## 9. Deploy to Tomcat 11

Copy:

```text
target/QueueLess.war
```

to:

```text
<TOMCAT_HOME>/webapps/
```

Start Tomcat.

Windows:

```text
<TOMCAT_HOME>/bin/startup.bat
```

Linux/macOS:

```bash
<TOMCAT_HOME>/bin/startup.sh
```

Open:

```text
http://localhost:8080/QueueLess/
```

## 10. Functional Flow

### Fresh installation

```text
Customers: 0
Services: 0
Queue Entries: 0
Completed: 0
Waiting: 0
Currently Serving: None
```

### Create a service

Login → Service Management → Create Service:

```text
Name: General Consultation
Prefix: A
Average service time: 5
```

### Online registration

Register a customer from `register.html`. The backend creates the next token.

### Walk-in registration

Staff Dashboard → Register Walk-in. The physical customer is inserted into the same service queue.

Example:

```text
A001  Ravi   ONLINE   WAITING
A002  Priya  PHYSICAL WAITING
A003  Arun   ONLINE   WAITING
```

### Queue control

`CALL NEXT` → first WAITING entry becomes SERVING.

`COMPLETE` → current SERVING entry becomes COMPLETED.

`SKIP` → current SERVING entry becomes SKIPPED.

`Cancel` → a WAITING entry becomes CANCELLED.

Cancelled numbers are never reused. If A003 is cancelled, the next registration is A004.

## 11. API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/services` | List active services for public registration |
| GET | `/api/services` | List all services for authenticated staff |
| POST | `/api/services` | Create service |
| PUT | `/api/services` | Update service |
| DELETE | `/api/services?id=` | Deactivate service |
| POST | `/api/register` | Register online or physical customer |
| GET | `/token?token=` | Server-side token lookup and JSP forwarding |
| GET | `/api/queue/status?token=` | Customer queue status |
| GET | `/api/queue/list?serviceId=` | Staff queue entries |
| POST | `/api/queue/action` | Call next / complete / skip / cancel |
| GET | `/api/dashboard?serviceId=` | Staff dashboard statistics |
| GET | `/api/system-stats` | Customer/service/queue totals for staff overview |
| POST | `/api/login` | Staff/admin login |
| POST | `/api/logout` | Session logout |
| POST | `/api/setup-user` | Protected initial account creation |

## 12. Security Notes

- SQL uses prepared statements.
- Passwords are BCrypt hashes.
- Staff operations require a server session.
- Frontend validation is backed by server validation.
- Database credentials are not stored in frontend files.
- Setup account creation requires a server environment key.
- JSP does not contain database credentials or SQL.

For HTTPS production deployment, configure Tomcat behind HTTPS/reverse proxy and set secure cookies as appropriate for the deployment environment.

## 13. Queue Consistency

Token allocation locks the service row inside a MySQL transaction before incrementing the service counter and inserting the queue entry. This prevents two concurrent registrations for the same service from receiving the same sequence number.

Each service has its own counter:

```text
General Consultation → A001, A002...
Dental              → D001, D002...
Eye Care             → E001, E002...
```

Online and physical entries are stored together in `queue_entries` and ordered by their server-generated sequence.

## 14. Troubleshooting

### `Database configuration is missing`

Set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, or create `src/main/resources/db.properties` from the example.

### `Communications link failure`

Confirm MySQL is running and the URL/port are correct.

### `Access denied for user`

Check the MySQL username/password.

### `Unknown database 'queueless'`

Run `database/queueless.sql`.

### Login says invalid credentials

Create the first account using the protected setup endpoint.

### No services on registration page

This is expected on a fresh installation. Login as staff/admin and create the first service.

### Tomcat cannot deploy WAR

Confirm Tomcat 11 is being used and run:

```bash
mvn clean package
```

Then inspect the Tomcat `logs` directory for deployment errors.

## 15. Screenshots

Add project screenshots here after deployment:

```text
screenshots/
├── home.png
├── registration.png
├── token.png
├── queue-status.png
├── staff-dashboard.png
└── service-management.png
```

## 16. Viva Demonstration Checklist

1. Show the empty initial database.
2. Create the first service.
3. Register an online customer → A001.
4. Register a walk-in customer → A002.
5. Register another online customer → A003.
6. Demonstrate `CALL NEXT`.
7. Demonstrate `COMPLETE`.
8. Show live customer tracking.
9. Cancel A003 and register another customer → A004.
10. Create a second service and demonstrate its independent token sequence.
