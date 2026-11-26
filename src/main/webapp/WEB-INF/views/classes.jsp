<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, com.studentanalytics.model.*, com.studentanalytics.util.ViewUtil" %>
<%
    String ctx = request.getContextPath();
    String navActive = "classes";
    @SuppressWarnings("unchecked")
    List<SchoolClass> classes = (List<SchoolClass>) request.getAttribute("classes");
    Integer totalStudents = (Integer) request.getAttribute("totalStudents");
    Integer unassigned = (Integer) request.getAttribute("unassignedCount");
    String message = (String) request.getAttribute("message");
    if (classes == null) classes = new ArrayList<SchoolClass>();
    if (totalStudents == null) totalStudents = 0;
    if (unassigned == null) unassigned = 0;
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Sections | Student Performance Analyser</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Public+Sans:wght@400;600;700&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
  <header class="topbar">
    <div>
      <h1><a href="<%= ctx %>/">Student Performance Analyser</a></h1>
      <p>Teacher dashboard &middot; sections</p>
    </div>
    <%@ include file="nav-teacher.jspf" %>
  </header>

  <main>
    <% if (message != null) { %>
      <div class="alert ok" role="status"><%= ViewUtil.esc(message) %></div>
    <% } %>

    <div class="summary">
      <div class="stat"><div class="label">Sections</div><div class="value"><%= classes.size() %></div></div>
      <div class="stat"><div class="label">Students</div><div class="value"><%= totalStudents %></div></div>
      <div class="stat"><div class="label">Not in a section</div><div class="value"><%= unassigned %></div></div>
    </div>

    <section class="sheet" aria-labelledby="newSecTitle">
      <h2 id="newSecTitle">Create a section</h2>
      <form method="post" action="<%= ctx %>/classes" class="create-class">
        <input type="hidden" name="action" value="create">
        <div>
          <label for="newSection">Section name</label>
          <input id="newSection" name="name" maxlength="40" required placeholder="e.g. 10-A or Physics Batch 2">
        </div>
        <button type="submit" class="btn">Create section</button>
      </form>
    </section>

    <section class="sheet" aria-labelledby="secTitle">
      <h2 id="secTitle">Your sections</h2>
      <% if (classes.isEmpty()) { %>
        <p class="empty">No sections yet. Create one above, then add students to it.</p>
      <% } else { %>
        <div class="class-grid">
          <% for (SchoolClass sc : classes) { %>
            <article class="class-card">
              <div>
                <h3><%= ViewUtil.esc(sc.getName()) %></h3>
                <p class="count"><%= sc.getStudentCount() %> student<%= sc.getStudentCount() == 1 ? "" : "s" %></p>
              </div>
              <div class="card-actions">
                <a class="btn small" href="<%= ctx %>/students?classId=<%= sc.getId() %>">View students</a>
                <a class="btn small" href="<%= ctx %>/students?action=new&amp;classId=<%= sc.getId() %>">Add student</a>
              </div>
              <form method="post" action="<%= ctx %>/classes" class="rename">
                <input type="hidden" name="action" value="rename">
                <input type="hidden" name="classId" value="<%= sc.getId() %>">
                <input name="name" maxlength="40" required value="<%= ViewUtil.esc(sc.getName()) %>" aria-label="Rename section <%= ViewUtil.esc(sc.getName()) %>">
                <button type="submit" class="btn small">Rename</button>
              </form>
              <form method="post" action="<%= ctx %>/classes"
                    onsubmit="return confirm('Delete section <%= ViewUtil.esc(sc.getName()).replace("'", "\\'") %>? Its students are kept and become unassigned.');">
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="classId" value="<%= sc.getId() %>">
                <button type="submit" class="btn small danger">Delete section</button>
              </form>
            </article>
          <% } %>
        </div>
      <% } %>
      <div class="actions">
        <a class="btn secondary" href="<%= ctx %>/students">All students (<%= totalStudents %>)</a>
        <% if (unassigned > 0) { %><a class="btn secondary" href="<%= ctx %>/students?classId=none">Unassigned (<%= unassigned %>)</a><% } %>
        <a class="btn" href="<%= ctx %>/students?action=new">Add student</a>
      </div>
    </section>
  </main>
  <script src="<%= ctx %>/js/session.js"></script>
</body>
</html>
