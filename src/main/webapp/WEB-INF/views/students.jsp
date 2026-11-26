<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, com.studentanalytics.model.*, com.studentanalytics.util.ViewUtil" %>
<%
    String ctx = request.getContextPath();
    String navActive = "report";
    @SuppressWarnings("unchecked")
    List<Student> students = (List<Student>) request.getAttribute("students");
    @SuppressWarnings("unchecked")
    List<Student> toppers = (List<Student>) request.getAttribute("toppers");
    Student lowest = (Student) request.getAttribute("lowestScorer");
    Double classAverage = (Double) request.getAttribute("classAverage");
    Double passPercentage = (Double) request.getAttribute("passPercentage");
    String message = (String) request.getAttribute("message");
    @SuppressWarnings("unchecked")
    List<SchoolClass> sections = (List<SchoolClass>) request.getAttribute("classes");
    String filterLabel = (String) request.getAttribute("filterLabel");
    String filterId = (String) request.getAttribute("filterId");
    String addHref = ctx + "/students?action=new"
            + (filterId != null && !"none".equals(filterId) ? "&classId=" + filterId : "");
    if (sections == null) sections = new ArrayList<SchoolClass>();
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
      <h1><a href="<%= ctx %>/">Student Performance Analyser</a></h1>
      <p><%= filterLabel == null ? "Class report and rankings" : ViewUtil.esc(filterLabel) + " &middot; report and rankings" %></p>
    </div>
    <%@ include file="nav-teacher.jspf" %>
  </header>

  <main>
    <% if (message != null) { %>
      <div class="alert ok" role="status"><%= ViewUtil.esc(message) %></div>
    <% } %>

    <% if (!sections.isEmpty() || filterId != null) { %>
      <nav class="chips no-print" aria-label="Filter by section">
        <a class="chip" href="<%= ctx %>/students"<%= filterId == null ? " aria-current=\"true\"" : "" %>>All students</a>
        <% for (SchoolClass sc : sections) { %>
          <a class="chip" href="<%= ctx %>/students?classId=<%= sc.getId() %>"<%= String.valueOf(sc.getId()).equals(filterId) ? " aria-current=\"true\"" : "" %>><%= ViewUtil.esc(sc.getName()) %> <small><%= sc.getStudentCount() %></small></a>
        <% } %>
        <a class="chip" href="<%= ctx %>/students?classId=none"<%= "none".equals(filterId) ? " aria-current=\"true\"" : "" %>>Unassigned</a>
        <a class="chip" href="<%= ctx %>/classes">Manage sections</a>
      </nav>
    <% } %>

    <% if (students.isEmpty()) { %>
      <section class="sheet">
        <h2><%= filterLabel == null ? "No students yet" : "No students in this view" %></h2>
        <p class="empty">Add a student with marks in each subject to see rankings and class statistics.</p>
        <div class="actions">
          <a class="btn" href="<%= addHref %>">Add the first student</a>
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
          <div class="value"><%= ViewUtil.pct(classAverage == null ? 0 : classAverage) %></div>
        </div>
        <div class="stat">
          <div class="label">Passing every subject</div>
          <div class="value"><%= ViewUtil.pct(passPercentage == null ? 0 : passPercentage) %></div>
        </div>
        <div class="stat">
          <div class="label"><%= toppers.size() > 1 ? "Joint toppers" : "Topper" %></div>
          <div class="value">
            <% for (int i = 0; i < toppers.size(); i++) { %><%= i > 0 ? ", " : "" %><%= ViewUtil.esc(toppers.get(i).getName()) %><% } %>
          </div>
          <% if (!toppers.isEmpty()) { %><div class="detail"><%= ViewUtil.pct(toppers.get(0).getPercentage()) %></div><% } %>
        </div>
        <% if (lowest != null) { %>
        <div class="stat">
          <div class="label">Lowest overall score</div>
          <div class="value"><%= ViewUtil.esc(lowest.getName()) %></div>
          <div class="detail"><%= ViewUtil.pct(lowest.getPercentage()) %></div>
        </div>
        <% } %>
      </div>

      <section class="sheet" aria-labelledby="rankTitle">
        <h2 id="rankTitle">Student rankings<%= filterLabel == null ? "" : " &middot; " + ViewUtil.esc(filterLabel) %></h2>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Rank</th><th>ID</th><th>Name</th><th>Section</th>
                <th class="num">Total</th><th class="num">Percentage</th><th></th>
                <th>Grade</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <% for (Student s : students) { %>
                <tr>
                  <td class="rank"><%= s.getRank() %></td>
                  <td><%= ViewUtil.esc(s.getId()) %></td>
                  <td><a href="<%= ctx %>/students?action=view&amp;id=<%= java.net.URLEncoder.encode(s.getId(), "UTF-8") %>"><%= ViewUtil.esc(s.getName()) %></a></td>
                  <td><%= s.getClassName() == null ? "-" : ViewUtil.esc(s.getClassName()) %></td>
                  <td class="num"><%= ViewUtil.num(s.getTotalMarks()) %> / <%= ViewUtil.num(s.getMaxTotalMarks()) %></td>
                  <td class="num"><%= ViewUtil.pct(s.getPercentage()) %></td>
                  <td><div class="bar" aria-hidden="true"><span style="width:<%= String.format("%.0f", Math.min(100, s.getPercentage())) %>%"></span></div></td>
                  <td><% if (s.getSubjects().isEmpty()) { %>-<% } else { %><span class="grade <%= ViewUtil.gradeClass(s.getGrade()) %>"><%= ViewUtil.esc(s.getGrade()) %></span><% } %></td>
                  <td>
                    <div class="row-actions no-print">
                    <a class="btn small" href="<%= ctx %>/students?action=view&amp;id=<%= java.net.URLEncoder.encode(s.getId(), "UTF-8") %>">Edit</a>
                    <form class="inline-form no-print" method="post" action="<%= ctx %>/students"
                          onsubmit="return confirm('Remove <%= ViewUtil.esc(s.getName()).replace("'", "\\'") %>?');">
                      <input type="hidden" name="action" value="delete">
                      <input type="hidden" name="id" value="<%= ViewUtil.esc(s.getId()) %>">
                      <button type="submit" class="btn small danger">Remove</button>
                    </form>
                    </div>
                  </td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>
        <div class="actions no-print">
          <a class="btn" href="<%= addHref %>">Add student</a>
          <button type="button" class="btn secondary" onclick="window.print()">Print report</button>
        </div>
      </section>
    <% } %>
  </main>
  <script src="<%= ctx %>/js/session.js"></script>
</body>
</html>
