<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="common.jspf" %>
<%
    String ctx = request.getContextPath();
    @SuppressWarnings("unchecked")
    List<SubjectStats> stats = (List<SubjectStats>) request.getAttribute("subjectStats");
    SubjectStats best = (SubjectStats) request.getAttribute("bestSubject");
    SubjectStats weakest = (SubjectStats) request.getAttribute("weakestSubject");
    String message = (String) request.getAttribute("message");
    if (stats == null) stats = new ArrayList<SubjectStats>();
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Subject statistics | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <div>
      <h1>Student Performance Analyser</h1>
      <p>Subject-wise statistics</p>
    </div>
    <nav class="nav" aria-label="Main">
      <a href="<%= ctx %>/students">Class report</a>
      <a href="<%= ctx %>/students?action=stats" aria-current="page">Subject statistics</a>
      <a href="<%= ctx %>/students?action=new">Add student</a>
    </nav>
  </header>

  <main>
    <% if (message != null) { %>
      <div class="alert ok" role="status"><%= esc(message) %></div>
    <% } %>

    <% if (stats.isEmpty()) { %>
      <section class="sheet">
        <h2>No subject data yet</h2>
        <p class="empty">Statistics appear once at least one student has marks in a subject.</p>
        <div class="actions">
          <a class="btn" href="<%= ctx %>/students?action=new">Add a student</a>
        </div>
      </section>
    <% } else { %>

      <div class="summary">
        <% if (best != null) { %>
        <div class="stat">
          <div class="label">Strongest subject</div>
          <div class="value"><%= esc(best.getSubjectName()) %></div>
          <div class="detail">Class average <%= pct(best.getAveragePercentage()) %></div>
        </div>
        <% } %>
        <% if (weakest != null) { %>
        <div class="stat">
          <div class="label">Weakest subject</div>
          <div class="value"><%= esc(weakest.getSubjectName()) %></div>
          <div class="detail">Class average <%= pct(weakest.getAveragePercentage()) %></div>
        </div>
        <% } %>
      </div>

      <section class="sheet" aria-labelledby="statsTitle">
        <h2 id="statsTitle">Statistics by subject</h2>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Subject</th><th class="num">Students</th><th class="num">Average</th><th></th>
                <th>Highest</th><th>Lowest</th><th class="num">Passed</th>
              </tr>
            </thead>
            <tbody>
              <% for (SubjectStats st : stats) { %>
                <tr>
                  <td><%= esc(st.getSubjectName()) %></td>
                  <td class="num"><%= st.getStudentCount() %></td>
                  <td class="num"><%= pct(st.getAveragePercentage()) %></td>
                  <td><div class="bar" aria-hidden="true"><span style="width:<%= String.format("%.0f", Math.min(100, st.getAveragePercentage())) %>%"></span></div></td>
                  <td><%= pct(st.getHighestPercentage()) %> (<%= esc(st.getHighestScorer()) %>)</td>
                  <td class="low"><%= pct(st.getLowestPercentage()) %> (<%= esc(st.getLowestScorer()) %>)</td>
                  <td class="num"><%= st.getPassCount() %> of <%= st.getStudentCount() %> (<%= String.format("%.0f%%", st.getPassRate()) %>)</td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>
        <div class="actions no-print">
          <a class="btn secondary" href="<%= ctx %>/students">Back to class report</a>
          <button type="button" class="btn" onclick="window.print()">Print</button>
        </div>
      </section>
    <% } %>
  </main>
</body>
</html>
