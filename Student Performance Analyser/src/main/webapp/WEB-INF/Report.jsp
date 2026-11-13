<%@ page contentType="text/html;charset=UTF-8" language="java"
    import="java.util.*, com.studentanalytics.model.*" %>
<%!
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static String pct(double v) {
        return String.format("%.2f%%", v);
    }
    private static String gradeClass(String g) {
        return g == null ? "" : "g-" + g.replace("+", "p");
    }
%>
<%
    String ctx = request.getContextPath();
    @SuppressWarnings("unchecked")
    List<Student> ranked = (List<Student>) request.getAttribute("rankedStudents");
    @SuppressWarnings("unchecked")
    List<SubjectStats> stats = (List<SubjectStats>) request.getAttribute("subjectStats");
    Double classAverage = (Double) request.getAttribute("classAverage");
    Double passPercentage = (Double) request.getAttribute("passPercentage");
    Student topper = (Student) request.getAttribute("topper");
    Student lowestScorer = (Student) request.getAttribute("lowestScorer");
    SubjectStats best = (SubjectStats) request.getAttribute("bestSubject");
    SubjectStats weakest = (SubjectStats) request.getAttribute("weakestSubject");

    if (ranked == null || ranked.isEmpty()) {
        response.sendRedirect(ctx + "/index.jsp");
        return;
    }
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
    <h1>Class performance report</h1>
    <p><%= ranked.size() %> students ranked by percentage</p>
  </header>

  <main>
    <div class="summary">
      <div class="stat">
        <div class="label">Class average</div>
        <div class="value"><%= pct(classAverage == null ? 0 : classAverage) %></div>
      </div>
      <div class="stat">
        <div class="label">Students passing all subjects</div>
        <div class="value"><%= pct(passPercentage == null ? 0 : passPercentage) %></div>
      </div>
      <% if (topper != null) { %>
      <div class="stat">
        <div class="label">Topper</div>
        <div class="value"><%= esc(topper.getName()) %></div>
        <div class="detail"><%= pct(topper.getPercentage()) %></div>
      </div>
      <% } %>
      <% if (lowestScorer != null) { %>
      <div class="stat">
        <div class="label">Lowest overall score</div>
        <div class="value"><%= esc(lowestScorer.getName()) %></div>
        <div class="detail"><%= pct(lowestScorer.getPercentage()) %></div>
      </div>
      <% } %>
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

    <section class="sheet" aria-labelledby="rankTitle">
      <h2 id="rankTitle">Student rankings</h2>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Rank</th><th>ID</th><th>Name</th>
              <th class="num">Total</th><th class="num">Percentage</th><th></th>
              <th>Grade</th><th>Best subject</th><th>Weakest subject</th>
            </tr>
          </thead>
          <tbody>
            <% for (Student s : ranked) {
                 Subject hi = s.getHighestSubject();
                 Subject lo = s.getLowestSubject(); %>
              <tr>
                <td class="rank"><%= s.getRank() %></td>
                <td><%= esc(s.getId()) %></td>
                <td><%= esc(s.getName()) %></td>
                <td class="num"><%= String.format("%.1f / %.1f", s.getTotalMarks(), s.getMaxTotalMarks()) %></td>
                <td class="num"><%= pct(s.getPercentage()) %></td>
                <td><div class="bar" aria-hidden="true"><span style="width:<%= String.format("%.0f", s.getPercentage()) %>%"></span></div></td>
                <td><span class="grade <%= gradeClass(s.getGrade()) %>"><%= esc(s.getGrade()) %></span></td>
                <td><%= hi == null ? "-" : esc(hi.getName()) + " (" + pct(hi.getPercentage()) + ")" %></td>
                <td class="low"><%= lo == null ? "-" : esc(lo.getName()) + " (" + pct(lo.getPercentage()) + ")" %></td>
              </tr>
            <% } %>
          </tbody>
        </table>
      </div>
    </section>

    <section class="sheet" aria-labelledby="subjTitle">
      <h2 id="subjTitle">Subject-wise statistics</h2>
      <% if (stats == null || stats.isEmpty()) { %>
        <p class="empty">No subject data available.</p>
      <% } else { %>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Subject</th><th class="num">Students</th><th class="num">Average</th>
                <th>Highest</th><th>Lowest</th><th class="num">Pass rate</th>
              </tr>
            </thead>
            <tbody>
              <% for (SubjectStats st : stats) { %>
                <tr>
                  <td><%= esc(st.getSubjectName()) %></td>
                  <td class="num"><%= st.getStudentCount() %></td>
                  <td class="num"><%= pct(st.getAveragePercentage()) %></td>
                  <td><%= pct(st.getHighestPercentage()) %> (<%= esc(st.getHighestScorer()) %>)</td>
                  <td class="low"><%= pct(st.getLowestPercentage()) %> (<%= esc(st.getLowestScorer()) %>)</td>
                  <td class="num"><%= st.getPassCount() %> of <%= st.getStudentCount() %> (<%= String.format("%.0f%%", st.getPassRate()) %>)</td>
                </tr>
              <% } %>
            </tbody>
          </table>
        </div>
      <% } %>
    </section>

    <div class="actions no-print">
      <a class="btn secondary" href="<%= ctx %>/index.jsp">Back to marks entry</a>
      <button type="button" class="btn" onclick="window.print()">Print report</button>
    </div>
  </main>
</body>
</html>
