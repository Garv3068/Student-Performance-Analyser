// Add-student form: add/remove subject rows and validate before submit.
// Rows already rendered by the server (after an error) are kept as they are.
(function () {
  var rows = document.getElementById('subjectRows');
  var form = document.getElementById('studentForm');
  if (!rows || !form) return;

  var errorBox = document.getElementById('formError');
  var defaults = ['Mathematics', 'Science', 'English'];

  function showError(message) {
    errorBox.textContent = message;
    errorBox.hidden = !message;
  }

  function addRow(subjectName) {
    var row = document.createElement('div');
    row.className = 'subject-row';
    row.innerHTML =
      '<input type="text" name="subjectName" aria-label="Subject name" maxlength="40">' +
      '<input type="number" name="marks" aria-label="Marks obtained" min="0" step="0.5">' +
      '<input type="number" name="maxMarks" aria-label="Maximum marks" value="100" min="1" step="0.5">' +
      '<button type="button" class="btn small" data-remove>Remove</button>';
    row.querySelector('input[name="subjectName"]').value = subjectName || '';
    rows.appendChild(row);
    return row;
  }

  rows.addEventListener('click', function (event) {
    if (!event.target.hasAttribute('data-remove')) return;
    if (rows.children.length > 1) {
      rows.removeChild(event.target.parentNode);
      showError('');
    } else {
      showError('Keep at least one row. Empty rows are ignored when you save.');
    }
  });

  document.getElementById('addSubject').addEventListener('click', function () {
    addRow('').querySelector('input').focus();
  });

  form.addEventListener('submit', function (event) {
    var seen = {};
    for (var i = 0; i < rows.children.length; i++) {
      var r = rows.children[i];
      var nameInput = r.querySelector('[name="subjectName"]');
      var name = nameInput.value.trim();
      var marksText = r.querySelector('[name="marks"]').value;
      if (!name && !marksText) continue;               // empty row: server skips it
      var marks = parseFloat(marksText);
      var max = parseFloat(r.querySelector('[name="maxMarks"]').value);
      var problem = null;
      if (!name) problem = 'Enter a subject name in row ' + (i + 1) + '.';
      else if (isNaN(marks)) problem = 'Enter the marks for ' + name + '.';
      else if (seen[name.toLowerCase()]) problem = name + ' is listed twice. Use each subject once.';
      else if (marks > max) problem = 'Marks for ' + name + ' cannot be higher than ' + max + '.';
      if (problem) { showError(problem); event.preventDefault(); return; }
      seen[name.toLowerCase()] = true;
    }
    showError('');
  });

  if (rows.children.length === 0) defaults.forEach(addRow);
})();
