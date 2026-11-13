<%@ page contentType="text/html;charset=UTF-8" language="java"
    import="java.util.*, com.studentanalytics.model.*" %>
<%!
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
%>
<%
    String ctx = request.getContextPath();
    @SuppressWarnings("unchecked")
    List<Student> students = (List<Student>) session.getAttribute("students");
    if (students == null) students = new ArrayList<Student>();
    String error = (String) request.getAttribute("error");
    String message = (String) request.getAttribute("message");
    String idValue = request.getParameter("studentId");
    String nameValue = request.getParameter("studentName");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Enter marks | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <h1>Student Performance Analyser</h1>
    <p>Enter marks, then generate the class report.</p>
  </header>

  <main>
    <section class="sheet" aria-labelledby="addTitle">
      <h2 id="addTitle">Add a student</h2>

      <% if (error != null) { %>
        <div class="alert" role="alert"><%= esc(error) %></div>
      <% } %>
      <% if (message != null) { %>
        <div class="alert ok" role="status"><%= esc(message) %></div>
      <% } %>
      <div id="formError" class="alert" role="alert" hidden></div>

      <form id="studentForm" method="post" action="<%= ctx %>/student">
        <div class="field-row">
          <div>
            <label for="studentId">Student ID</label>
            <input id="studentId" name="studentId" maxlength="20" required value="<%= esc(idValue) %>">
          </div>
          <div>
            <label for="studentName">Student name</label>
            <input id="studentName" name="studentName" maxlength="60" required value="<%= esc(nameValue) %>">
          </div>
        </div>

        <div class="subject-head" aria-hidden="true">
          <span>Subject</span><span>Marks</span><span>Out of</span><span></span>
        </div>
        <div id="subjectRows"></div>

        <div class="actions">
          <button type="button" id="addSubject" class="btn secondary">Add subject</button>
          <button type="submit" class="btn">Save student</button>
        </div>
      </form>
    </section>

    <section class="sheet" aria-labelledby="listTitle">
      <h2 id="listTitle">Students added (<%= students.size() %>)</h2>

      <% if (students.isEmpty()) { %>
        <p class="empty">No students yet. Save at least one student to generate a report.</p>
      <% } else { %>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>ID</th><th>Name</th><th class="num">Subjects</th><th class="num">Total</th><th class="num">Percentage</th></tr>
            </thead>
            <tbody>
              <% for (Student s : students) { %>
                <tr>
                  <td><%= esc(s.getId()) %></td>
                  <td><%= esc(s.getName()) %></td>
                  <td class="num"><%= s.getSubjects().size() %></td>
                  <td class="num"><%= String.format("%.1f / %.1f", s.getTotalMarks(), s.getMaxTotalMarks()) %></td>
                  <td class="num"><%= String.format("%.2f%%", s.getPercentage()) %></td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>
      <% } %>

      <div class="actions">
        <a class="btn" href="<%= ctx %>/report" <%= students.isEmpty() ? "aria-disabled=\"true\"" : "" %>>Generate report</a>
      </div>
    </section>
  </main>

  <script src="<%= ctx %>/js/app.js"></script>
</body>
</html>
