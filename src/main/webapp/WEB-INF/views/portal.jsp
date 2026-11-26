<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, com.studentanalytics.model.*, com.studentanalytics.util.ViewUtil" %>
<%
    String ctx = request.getContextPath();
    Student student = (Student) request.getAttribute("student");
    Subject highest = (Subject) request.getAttribute("highestSubject");
    Subject lowest = (Subject) request.getAttribute("lowestSubject");
    Integer groupSize = (Integer) request.getAttribute("groupSize");
    Double groupAverage = (Double) request.getAttribute("groupAverage");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>My marks | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <div>
      <h1><a href="<%= ctx %>/">Student Performance Analyser</a></h1>
      <p>Student portal &middot; my marks and details</p>
    </div>
    <nav class="nav" aria-label="Main">
      <a href="<%= ctx %>/portal" aria-current="page">My marks</a>
      <span class="who">Student: <%= ViewUtil.esc((String) session.getAttribute("authUserName")) %></span>
      <a href="<%= ctx %>/auth.html#signin" data-switch data-auth="<%= ctx %>/auth">Switch account</a>
      <a href="<%= ctx %>/" data-signout data-auth="<%= ctx %>/auth">Sign out</a>
    </nav>
  </header>

  <main>
    <% if (student == null) { %>
      <section class="sheet">
        <h2>No student record found</h2>
        <p class="empty">Your account is not linked to a student record. Your teacher may have removed it or
          changed the email. Ask your teacher to add you again with the email
          <strong><%= ViewUtil.esc((String) session.getAttribute("authUserEmail")) %></strong>.</p>
      </section>
    <% } else { %>

      <section class="sheet" aria-labelledby="studentName">
        <div class="detail-head">
          <div>
            <h2 id="studentName"><%= ViewUtil.esc(student.getName()) %></h2>
            <p class="meta">ID <%= ViewUtil.esc(student.getId()) %>
              &middot; <%= student.getClassName() == null ? "No section yet" : "Section " + ViewUtil.esc(student.getClassName()) %>
              <% if (student.getEmail() != null) { %> &middot; <%= ViewUtil.esc(student.getEmail()) %><% } %></p>
          </div>
          <% if (!student.getSubjects().isEmpty()) { %>
            <span class="grade <%= ViewUtil.gradeClass(student.getGrade()) %>" aria-label="Grade <%= ViewUtil.esc(student.getGrade()) %>"><%= ViewUtil.esc(student.getGrade()) %></span>
          <% } %>
        </div>
      </section>

      <% if (student.getSubjects().isEmpty()) { %>
        <section class="sheet">
          <h2>Marks</h2>
          <p class="empty">No marks have been entered for you yet. Check back after your teacher adds them.</p>
        </section>
      <% } else { %>
        <div class="summary">
          <div class="stat">
            <div class="label">Total marks</div>
            <div class="value"><%= ViewUtil.num(student.getTotalMarks()) %> / <%= ViewUtil.num(student.getMaxTotalMarks()) %></div>
          </div>
          <div class="stat">
            <div class="label">Percentage</div>
            <div class="value"><%= ViewUtil.pct(student.getPercentage()) %></div>
          </div>
          <% if (groupSize != null && groupSize > 0) { %>
          <div class="stat">
            <div class="label">Rank</div>
            <div class="value"><%= student.getRank() %> of <%= groupSize %></div>
            <% if (groupAverage != null) { %><div class="detail">Average of the group <%= ViewUtil.pct(groupAverage) %></div><% } %>
          </div>
          <% } %>
          <% if (highest != null) { %>
          <div class="stat">
            <div class="label">Strongest subject</div>
            <div class="value"><%= ViewUtil.esc(highest.getName()) %></div>
            <div class="detail"><%= ViewUtil.pct(highest.getPercentage()) %></div>
          </div>
          <% } %>
          <% if (lowest != null) { %>
          <div class="stat">
            <div class="label">Needs most work</div>
            <div class="value"><%= ViewUtil.esc(lowest.getName()) %></div>
            <div class="detail"><%= ViewUtil.pct(lowest.getPercentage()) %></div>
          </div>
          <% } %>
        </div>

        <section class="sheet" aria-labelledby="subjTitle">
          <h2 id="subjTitle">Subject marks</h2>
          <p class="readonly-note">This is a read-only view. Ask your teacher if something looks wrong.</p>
          <div class="table-wrap">
            <table>
              <thead>
                <tr><th>Subject</th><th class="num">Marks</th><th class="num">Out of</th><th class="num">Percentage</th><th></th><th>Result</th></tr>
              </thead>
              <tbody>
                <% for (Subject sub : student.getSubjects()) { %>
                  <tr>
                    <td><%= ViewUtil.esc(sub.getName()) %></td>
                    <td class="num"><%= ViewUtil.num(sub.getMarksObtained()) %></td>
                    <td class="num"><%= ViewUtil.num(sub.getMaxMarks()) %></td>
                    <td class="num <%= sub.isPassed() ? "" : "low" %>"><%= ViewUtil.pct(sub.getPercentage()) %></td>
                    <td><div class="bar" aria-hidden="true"><span style="width:<%= String.format("%.0f", Math.min(100, sub.getPercentage())) %>%"></span></div></td>
                    <td><span class="<%= sub.isPassed() ? "status-pass" : "status-fail" %>"><%= sub.isPassed() ? "Pass" : "Fail" %></span></td>
                  </tr>
                <% } %>
              </tbody>
            </table>
          </div>
          <div class="actions no-print">
            <button type="button" class="btn secondary" onclick="window.print()">Print my report</button>
          </div>
        </section>
      <% } %>
    <% } %>
  </main>
  <script src="<%= ctx %>/js/session.js"></script>
</body>
</html>
