# 🎓 Student Performance Analyser

A Java web application that turns raw marks into grades, class rankings and subject-wise analytics. Teachers manage sections, students and marks; students sign in to see their own results. All data is stored in **MySQL** through **JDBC**.

> Built with Java 21, Servlets/JSP and MySQL, deployed on Apache Tomcat 10.1, for the **Programming in Java** course (B.Tech CSE AI-ML, Chitkara University).

---

## ✨ Features

### 👩‍🏫 For teachers
- Sign up / sign in with a **teacher account**
- Create, rename and delete **sections** (e.g. G1, G2)
- Add, edit and delete **students** (ID, name, email, section)
- Add, update and remove **subject marks**, with custom maximum marks per subject
- **Class report**: automatic total, percentage, grade and **rank** for every student
  - Ties share a rank (competition ranking: 1, 1, 3)
- **Class analytics**: class average, pass percentage, topper(s), lowest scorer
- **Subject statistics**: average, highest and lowest scorer, pass rate, strongest and weakest subject
- Printable reports

### 🧑‍🎓 For students
- Sign up with the email the teacher registered, then sign in
- **Student portal**: own marks, percentage, grade, rank in the group, strongest and weakest subject

### 📊 Grading rules

| Percentage | Grade |
| --- | --- |
| above 80 | O |
| above 70 | A+ |
| above 60 | A |
| 50 and above | B+ |
| 40 and above | B |
| any subject below 40% | **F** |

---

## 🛠️ Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 (JavaSE-21) |
| Web layer | Jakarta Servlets, JSP, Servlet Filter |
| Server | Apache Tomcat 10.1 |
| Database | MySQL |
| DB access | JDBC with MySQL Connector/J |
| Security | PBKDF2-HMAC-SHA256 password hashing with salt, HTTP sessions, role-based access |
| Frontend | HTML5, CSS3, vanilla JavaScript |
| Architecture | MVC + DAO |
| IDE / tools | Eclipse (Dynamic Web Project), Git, GitHub |
| Base package | `com.studentanalytics` |

---

## 🏗️ Architecture

```
Browser
   |
   v
AuthFilter             -> not signed in? redirect to sign-in. Wrong role? redirect to own home page
   |
   v
Servlets (controllers)  StudentServlet, ClassServlet, PortalServlet, AuthServlet
   |            \
   v             v
Services          DAO layer (JDBC)
GradeCalculator   StudentDao (interface) -> JdbcStudentDao
RankingService    ClassDao, UserDao, Database
AnalyticsService       |
   |                   v
   v                 MySQL
JSP views -> Browser
```

---

## 📁 Project Structure

```
Student-Performance-Analyser/
├── src/main/java/com/studentanalytics/
│   ├── model/     Student, Subject, SubjectStats, SchoolClass, User, Role (enum)
│   ├── dao/       Database, StudentDao, JdbcStudentDao, ClassDao, UserDao, DataAccessException
│   ├── service/   RankingService, AnalyticsService
│   ├── util/      GradeCalculator, StudentComparators, PasswordHasher, ViewUtil
│   └── servlet/   AuthServlet, AuthFilter, StudentServlet, ClassServlet, PortalServlet
├── src/main/webapp/
│   ├── index.html, auth.html    landing page, sign-in / sign-up
│   ├── css/, js/                styles and scripts
│   └── WEB-INF/
│       ├── views/               JSP pages
│       ├── lib/                 mysql-connector-j jar
│       └── web.xml
├── .classpath, .project         Eclipse project config
└── README.md
```

---

## 🗄️ Database

The database `student_analytics` and its tables are **created automatically** on first run (`Database.initSchema()`).

| Table | Purpose | Key |
| --- | --- | --- |
| `users` | Teacher and student accounts (password stored as a hash) | `id` |
| `classes` | Sections owned by a teacher | `id`, FK `user_id` → users |
| `students` | Student records | composite PK `(user_id, id)`, FK `class_id` → classes |
| `subjects` | One row per subject mark | `id`, FK `(user_id, student_id)` → students |

- Deleting a student deletes their marks (`ON DELETE CASCADE`).
- Deleting a section leaves its students unassigned (`ON DELETE SET NULL`).
- Every query is filtered by the signed-in teacher's `user_id`, so teachers only ever see their own data.

---

## ✅ Prerequisites

- **JDK 21** or later
- **Apache Tomcat 10.1** (Jakarta EE namespace)
- **MySQL 8**, running locally
- **Eclipse IDE for Enterprise Java and Web Developers** (recommended)

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Garv3068/Student-Performance-Analyser.git
cd Student-Performance-Analyser
```

### 2. Configure the database connection

Set these **environment variables** (recommended):

```
DB_URL=jdbc:mysql://localhost:3306/student_analytics?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
DB_USER=your_mysql_user
DB_PASSWORD=your_mysql_password
```

Or create `src/main/webapp/WEB-INF/classes/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/student_analytics?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
db.user=your_mysql_user
db.password=your_mysql_password
```

> ⚠️ Never commit `db.properties` with a real password. Add it to `.gitignore`.

### 3. Import into Eclipse

1. **File → Import → Existing Projects into Workspace**, select the cloned folder, click **Finish**.
2. Confirm the project uses **JavaSE-21** under *Project → Properties → Java Build Path*.

### 4. Configure Tomcat

1. **Window → Preferences → Server → Runtime Environments**.
2. Add **Apache Tomcat v10.1** and point it to your Tomcat installation directory.
3. Make sure the project's targeted runtime is Tomcat 10.1.

### 5. Run the application

1. Make sure **MySQL is running**.
2. Right-click the project → **Run As → Run on Server** → **Tomcat v10.1** → **Finish**.
3. Open your browser at:

```
http://localhost:8080/Student-Performance-Analyser/
```

> The context path may differ depending on your project/server settings.

Or deploy the WAR: copy `Student_Performance_Analyser_v2.war` into Tomcat's `webapps/` folder and start Tomcat.

---

## 📖 Usage

1. Open the app and **sign up as a teacher**.
2. Create a section, then add students with their email and marks.
3. View the **class report** (ranks and grades) and **subject statistics**.
4. A student can **sign up as a student** with the same email the teacher used, then sign in to see their portal.

<!-- Add screenshots here:
![Sign in](docs/screenshots/auth.png)
![Class report](docs/screenshots/class-report.png)
![Student portal](docs/screenshots/portal.png)
-->

---

## 🔒 Security

- Passwords are hashed with **PBKDF2-HMAC-SHA256**, a random 16-byte salt and 210,000 iterations; plain passwords are never stored.
- All SQL uses **PreparedStatement** (protects against SQL injection).
- A new session is created on sign-in (prevents session fixation).
- Repeated failed sign-ins are temporarily blocked.
- Protected pages send `Cache-Control: no-store`.
- User input is HTML-escaped before display.

---

## ☕ Java Concepts Used

OOP (encapsulation, inheritance, polymorphism, abstraction) · interfaces and implementation (`StudentDao` / `JdbcStudentDao`) · enum (`Role`) · method overloading and overriding · custom exceptions (`DataAccessException`) · checked and unchecked exceptions · Collections (`ArrayList`, `LinkedHashMap`, `ConcurrentHashMap`) · `Comparator` chaining · generics · lambdas and streams · JDBC (PreparedStatement, ResultSet, transactions, batch insert) · try-with-resources · static blocks · regex validation · thread safety (`ConcurrentHashMap`, `synchronized`) · Servlets, JSP and Filters

---

## 🔮 Future Improvements

- Charts and visual dashboards for performance trends
- Export reports to PDF / Excel
- Attendance tracking and correlation with performance
- Email notifications for results
- REST API support
- JUnit tests for grading and ranking

---

## 🤝 Contributing

Contributions are welcome!

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

No license has been specified yet. Consider adding one (e.g. [MIT](https://choosealicense.com/licenses/mit/)) via a `LICENSE` file.

---

## 👥 Team

| Name | GitHub | Contribution |
| --- | --- | --- |
| Garv Goel | [@Garv3068](https://github.com/Garv3068) | |
| Kartik Thakur | [@Kartikthakur2208](https://github.com/Kartikthakur2208) | |
| Ryan Sharma | [@Ryan140-cloud](https://github.com/Ryan140-cloud) | |
| Ridhi Jain | | |
