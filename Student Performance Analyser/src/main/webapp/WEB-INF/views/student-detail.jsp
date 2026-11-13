<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="common.jspf" %>
<%
    String ctx = request.getContextPath();
    Student student = (Student) request.getAttribute("student");
    Subject highest = (Subject) request.getAttribute("highestSubject");
    Subject lowest = (Subject) request.getAttribute("lowestSubject");
    Integer classSize = (Integer) request.getAttribute("classSize");
    String message = (String) request.getAttribute("message");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><%= esc(student.getName()) %> | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <div>
      <h1>Student Performance Analyser</h1>
      <p>Student details</p>
    </div>
    <nav class="nav" aria-label="Main">
      <a href="<%= ctx %>/students">Class report</a>
      <a href="<%= ctx %>/students?action=stats">Subject statistics</a>
      <a href="<%= ctx %>/students?action=new">Add student</a>
    </nav>
  </header>

  <main>
    <% if (message != null) { %>
      <div class="alert ok" role="status"><%= esc(message) %></div>
    <% } %>

    <section class="sheet" aria-labelledby="studentName">
      <div class="detail-head">
        <div>
          <h2 id="studentName"><%= esc(student.getName()) %></h2>
          <p class="meta">ID <%= esc(student.getId()) %>
            <% if (classSize != null) { %> &middot; Rank <%= student.getRank() %> of <%= classSize %><% } %></p>
        </div>
        <% if (!student.getSubjects().isEmpty()) { %>
          <span class="grade <%= gradeClass(student.getGrade()) %>" aria-label="Grade <%= esc(student.getGrade()) %>"><%= esc(student.getGrade()) %></span>
        <% } %>
      </div>
    </section>

    <div class="summary">
      <div class="stat">
        <div class="label">Total marks</div>
        <div class="value"><%= num(student.getTotalMarks()) %> / <%= num(student.getMaxTotalMarks()) %></div>
      </div>
      <div class="stat">
        <div class="label">Percentage</div>
        <div class="value"><%= pct(student.getPercentage()) %></div>
      </div>
      <% if (highest != null) { %>
      <div class="stat">
        <div class="label">Strongest subject</div>
        <div class="value"><%= esc(highest.getName()) %></div>
        <div class="detail"><%= pct(highest.getPercentage()) %></div>
      </div>
      <% } %>
      <% if (lowest != null) { %>
      <div class="stat">
        <div class="label">Weakest subject</div>
        <div class="value"><%= esc(lowest.getName()) %></div>
        <div class="detail"><%= pct(lowest.getPercentage()) %></div>
      </div>
      <% } %>
    </div>

    <section class="sheet" aria-labelledby="subjTitle">
      <h2 id="subjTitle">Subject marks</h2>
      <% if (student.getSubjects().isEmpty()) { %>
        <p class="empty">No subjects yet. Add the first one below.</p>
      <% } else { %>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Subject</th><th class="num">Marks</th><th class="num">Out of</th><th class="num">Percentage</th><th>Result</th></tr>
            </thead>
            <tbody>
              <% for (Subject sub : student.getSubjects()) { %>
                <tr>
                  <td><%= esc(sub.getName()) %></td>
                  <td class="num"><%= num(sub.getMarksObtained()) %></td>
                  <td class="num"><%= num(sub.getMaxMarks()) %></td>
                  <td class="num <%= sub.isPassed() ? "" : "low" %>"><%= pct(sub.getPercentage()) %></td>
                  <td><span class="<%= sub.isPassed() ? "status-pass" : "status-fail" %>"><%= sub.isPassed() ? "Pass" : "Fail" %></span></td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>
      <% } %>
    </section>

    <section class="sheet" aria-labelledby="addSubjTitle">
      <h2 id="addSubjTitle">Add a subject</h2>
      <form method="post" action="<%= ctx %>/students" class="add-subject">
        <input type="hidden" name="action" value="addSubject">
        <input type="hidden" name="id" value="<%= esc(student.getId()) %>">
        <div>
          <label for="subjectName">Subject</label>
          <input id="subjectName" name="subjectName" maxlength="40" required>
        </div>
        <div>
          <label for="marks">Marks</label>
          <input id="marks" name="marks" type="number" min="0" step="0.5" required>
        </div>
        <div>
          <label for="maxMarks">Out of</label>
          <input id="maxMarks" name="maxMarks" type="number" min="1" step="0.5" value="100">
        </div>
        <button type="submit" class="btn">Add subject</button>
      </form>
    </section>

    <div class="actions">
      <a class="btn secondary" href="<%= ctx %>/students">Back to class report</a>
      <form class="inline-form" method="post" action="<%= ctx %>/students"
            onsubmit="return confirm('Remove <%= esc(student.getName()).replace("'", "\\'") %>?');">
        <input type="hidden" name="action" value="delete">
        <input type="hidden" name="id" value="<%= esc(student.getId()) %>">
        <button type="submit" class="btn danger">Remove student</button>
      </form>
    </div>
  </main>
</body>
</html>
