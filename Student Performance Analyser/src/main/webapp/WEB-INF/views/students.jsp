<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="common.jspf" %>
<%
    String ctx = request.getContextPath();
    @SuppressWarnings("unchecked")
    List<Student> students = (List<Student>) request.getAttribute("students");
    @SuppressWarnings("unchecked")
    List<Student> toppers = (List<Student>) request.getAttribute("toppers");
    Student lowest = (Student) request.getAttribute("lowestScorer");
    Double classAverage = (Double) request.getAttribute("classAverage");
    Double passPercentage = (Double) request.getAttribute("passPercentage");
    String message = (String) request.getAttribute("message");
    if (students == null) students = new ArrayList<Student>();
    if (toppers == null) toppers = new ArrayList<Student>();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Class report | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <div>
      <h1>Student Performance Analyser</h1>
      <p>Class report and rankings</p>
    </div>
    <nav class="nav" aria-label="Main">
      <a href="<%= ctx %>/students" aria-current="page">Class report</a>
      <a href="<%= ctx %>/students?action=stats">Subject statistics</a>
      <a href="<%= ctx %>/students?action=new">Add student</a>
    </nav>
  </header>

  <main>
    <% if (message != null) { %>
      <div class="alert ok" role="status"><%= esc(message) %></div>
    <% } %>

    <% if (students.isEmpty()) { %>
      <section class="sheet">
        <h2>No students yet</h2>
        <p class="empty">Add a student with marks in each subject to see rankings and class statistics.</p>
        <div class="actions">
          <a class="btn" href="<%= ctx %>/students?action=new">Add the first student</a>
        </div>
      </section>
    <% } else { %>

      <div class="summary">
        <div class="stat">
          <div class="label">Students</div>
          <div class="value"><%= students.size() %></div>
        </div>
        <div class="stat">
          <div class="label">Class average</div>
          <div class="value"><%= pct(classAverage == null ? 0 : classAverage) %></div>
        </div>
        <div class="stat">
          <div class="label">Passing every subject</div>
          <div class="value"><%= pct(passPercentage == null ? 0 : passPercentage) %></div>
        </div>
        <div class="stat">
          <div class="label"><%= toppers.size() > 1 ? "Joint toppers" : "Topper" %></div>
          <div class="value">
            <% for (int i = 0; i < toppers.size(); i++) { %><%= i > 0 ? ", " : "" %><%= esc(toppers.get(i).getName()) %><% } %>
          </div>
          <% if (!toppers.isEmpty()) { %><div class="detail"><%= pct(toppers.get(0).getPercentage()) %></div><% } %>
        </div>
        <% if (lowest != null) { %>
        <div class="stat">
          <div class="label">Lowest overall score</div>
          <div class="value"><%= esc(lowest.getName()) %></div>
          <div class="detail"><%= pct(lowest.getPercentage()) %></div>
        </div>
        <% } %>
      </div>

      <section class="sheet" aria-labelledby="rankTitle">
        <h2 id="rankTitle">Student rankings</h2>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Rank</th><th>ID</th><th>Name</th>
                <th class="num">Total</th><th class="num">Percentage</th><th></th>
                <th>Grade</th><th></th>
              </tr>
            </thead>
            <tbody>
              <% for (Student s : students) { %>
                <tr>
                  <td class="rank"><%= s.getRank() %></td>
                  <td><%= esc(s.getId()) %></td>
                  <td><a href="<%= ctx %>/students?action=view&amp;id=<%= java.net.URLEncoder.encode(s.getId(), "UTF-8") %>"><%= esc(s.getName()) %></a></td>
                  <td class="num"><%= num(s.getTotalMarks()) %> / <%= num(s.getMaxTotalMarks()) %></td>
                  <td class="num"><%= pct(s.getPercentage()) %></td>
                  <td><div class="bar" aria-hidden="true"><span style="width:<%= String.format("%.0f", Math.min(100, s.getPercentage())) %>%"></span></div></td>
                  <td><% if (s.getSubjects().isEmpty()) { %>-<% } else { %><span class="grade <%= gradeClass(s.getGrade()) %>"><%= esc(s.getGrade()) %></span><% } %></td>
                  <td>
                    <form class="inline-form no-print" method="post" action="<%= ctx %>/students"
                          onsubmit="return confirm('Remove <%= esc(s.getName()).replace("'", "\\'") %>?');">
                      <input type="hidden" name="action" value="delete">
                      <input type="hidden" name="id" value="<%= esc(s.getId()) %>">
                      <button type="submit" class="btn small danger">Remove</button>
                    </form>
                  </td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>
        <div class="actions no-print">
          <a class="btn" href="<%= ctx %>/students?action=new">Add student</a>
          <button type="button" class="btn secondary" onclick="window.print()">Print report</button>
        </div>
      </section>
    <% } %>
  </main>
</body>
</html>
