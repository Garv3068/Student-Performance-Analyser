# 🎓 Student Performance Analyser

A Java web application for recording, analysing, and reporting student academic performance. It helps teachers and administrators turn raw marks into meaningful insights such as totals, averages, grades, and class-level trends.

> Built with Java (Servlets/JSP) and deployed on Apache Tomcat.

---

## ✨ Features

> Edit this list to match what your app actually does.

- Add, view, update, and delete student records
- Enter marks for multiple subjects
- Automatic calculation of total, percentage, and average
- Grade assignment based on performance
- Identify top performers and students who need support
- Subject-wise and class-wise performance summary

---

## 🛠️ Tech Stack

| Layer            | Technology                          |
| ---------------- | ----------------------------------- |
| Language         | Java 21 (JavaSE-21)                 |
| Web Layer        | Java Servlets / JSP (Jakarta EE)    |
| Server           | Apache Tomcat 10.1                  |
| IDE              | Eclipse (Dynamic Web Project)       |
| Base Package     | `com.studentanalytics`              |

<!-- Add your database / frontend (e.g. MySQL, JDBC, HTML/CSS/JS) here if used -->

---

## 📁 Project Structure

```
Student-Performance-Analyser/
├── src/
│   └── main/
│       ├── java/                  # Java source code (com.studentanalytics)
│       └── webapp/                # JSP pages, static assets, WEB-INF
├── build/
│   └── classes/                   # Compiled classes (Eclipse output)
├── .classpath                     # Eclipse classpath config
├── .project                       # Eclipse project config
└── README.md
```

---

## ✅ Prerequisites

- **JDK 21** or later
- **Apache Tomcat 10.1** (Jakarta EE namespace)
- **Eclipse IDE for Enterprise Java and Web Developers** (recommended)
<!-- - MySQL / other database, if applicable -->

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Garv3068/Student-Performance-Analyser.git
cd Student-Performance-Analyser
```

### 2. Import into Eclipse

1. Open Eclipse and go to **File → Import → Existing Projects into Workspace**.
2. Select the cloned folder and click **Finish**.
3. Confirm the project uses **JavaSE-21** under *Project → Properties → Java Build Path*.

### 3. Configure Tomcat

1. Go to **Window → Preferences → Server → Runtime Environments**.
2. Add **Apache Tomcat v10.1** and point it to your Tomcat installation directory.
3. Make sure the project's targeted runtime is set to Tomcat 10.1.

### 4. Run the application

1. Right-click the project and choose **Run As → Run on Server**.
2. Select **Tomcat v10.1** and click **Finish**.
3. Open your browser at:

```
http://localhost:8080/Student-Performance-Analyser/
```

> The context path may differ depending on your project/server settings.

---

## 📖 Usage

1. Open the application in your browser.
2. Add students and enter their marks for each subject.
3. View the generated results, grades, and performance summary.
4. Use the analysis views to compare students and spot trends.

<!-- Add screenshots here:
![Home Page](docs/screenshots/home.png)
-->

---

## 🔮 Future Improvements

- Charts and visual dashboards for performance trends
- Export reports to PDF / Excel
- Role-based login (admin / teacher / student)
- Attendance tracking and correlation with performance
- REST API support

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

## 👤 Author

**Garv** — [@Garv3068](https://github.com/Garv3068)
