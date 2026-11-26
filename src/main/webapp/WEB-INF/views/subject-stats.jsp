<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, com.studentanalytics.model.*, com.studentanalytics.util.ViewUtil" %>
<%
    String ctx = request.getContextPath();
    String navActive = "stats";
    @SuppressWarnings("unchecked")
    List<SubjectStats> stats = (List<SubjectStats>) request.getAttribute("subjectStats");
    SubjectStats best = (SubjectStats) request.getAttribute("bestSubject");
    SubjectStats weakest = (SubjectStats) request.getAttribute("weakestSubject");
    String message = (String) request.getAttribute("message");
    @SuppressWarnings("unchecked")
    List<SchoolClass> sections = (List<SchoolClass>) request.getAttribute("classes");
    String filterId = (String) request.getAttribute("filterId");
    if (sections == null) sections = new ArrayList<SchoolClass>();
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
      <h1><a href="<%= ctx %>/">Student Performance Analyser</a></h1>
      <p>Subject-wise statistics</p>
    </div>
    <%@ include file="nav-teacher.jspf" %>
  </header>

  <main>
    <% if (message != null) { %>
      <div class="alert ok" role="status"><%= ViewUtil.esc(message) %></div>
    <% } %>

    <% if (!sections.isEmpty() || filterId != null) { %>
      <nav class="chips no-print" aria-label="Filter by section">
        <a class="chip" href="<%= ctx %>/students?action=stats"<%= filterId == null ? " aria-current=\"true\"" : "" %>>All students</a>
        <% for (SchoolClass sc : sections) { %>
          <a class="chip" href="<%= ctx %>/students?action=stats&amp;classId=<%= sc.getId() %>"<%= String.valueOf(sc.getId()).equals(filterId) ? " aria-current=\"true\"" : "" %>><%= ViewUtil.esc(sc.getName()) %></a>
        <% } %>
        <a class="chip" href="<%= ctx %>/students?action=stats&amp;classId=none"<%= "none".equals(filterId) ? " aria-current=\"true\"" : "" %>>Unassigned</a>
      </nav>
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
          <div class="value"><%= ViewUtil.esc(best.getSubjectName()) %></div>
          <div class="detail">Class average <%= ViewUtil.pct(best.getAveragePercentage()) %></div>
        </div>
        <% } %>
        <% if (weakest != null) { %>
        <div class="stat">
          <div class="label">Weakest subject</div>
          <div class="value"><%= ViewUtil.esc(weakest.getSubjectName()) %></div>
          <div class="detail">Class average <%= ViewUtil.pct(weakest.getAveragePercentage()) %></div>
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
                  <td><%= ViewUtil.esc(st.getSubjectName()) %></td>
                  <td class="num"><%= st.getStudentCount() %></td>
                  <td class="num"><%= ViewUtil.pct(st.getAveragePercentage()) %></td>
                  <td><div class="bar" aria-hidden="true"><span style="width:<%= String.format("%.0f", Math.min(100, st.getAveragePercentage())) %>%"></span></div></td>
                  <td><%= ViewUtil.pct(st.getHighestPercentage()) %> (<%= ViewUtil.esc(st.getHighestScorer()) %>)</td>
                  <td class="low"><%= ViewUtil.pct(st.getLowestPercentage()) %> (<%= ViewUtil.esc(st.getLowestScorer()) %>)</td>
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
  <script src="<%= ctx %>/js/session.js"></script>
</body>
</html>
