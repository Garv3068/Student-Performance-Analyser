// Sign-in / sign-up page: tab switching, validation, strength meter and form submit.
// Talks to the AuthServlet at "auth" and sends signed-in users to the page for their role:
// teachers to their sections, students to their own marks.
(function () {
  var tabsEl = document.querySelector('.tabs');
  var tabs = { signin: document.getElementById('tabSignin'), signup: document.getElementById('tabSignup') };
  var panels = { signin: document.getElementById('panelSignin'), signup: document.getElementById('panelSignup') };
  var forms = { signin: document.getElementById('formSignin'), signup: document.getElementById('formSignup') };
  var EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;

  // ---------- Tabs (the #signin / #signup hash makes each mode linkable) ----------
  function show(mode, moveFocus) {
    ['signin', 'signup'].forEach(function (m) {
      var on = m === mode;
      tabs[m].setAttribute('aria-selected', String(on));
      tabs[m].tabIndex = on ? 0 : -1;
      panels[m].hidden = !on;
    });
    tabsEl.setAttribute('data-mode', mode);
    document.title = (mode === 'signin' ? 'Sign in' : 'Sign up') + ' | Student Performance Analyser';
    if (moveFocus) forms[mode].querySelector('input').focus();
  }
  function modeFromHash() { return location.hash === '#signup' ? 'signup' : 'signin'; }

  function go(mode) {
    if (history.replaceState) history.replaceState(null, '', '#' + mode);
    show(mode, true);
  }
  tabs.signin.addEventListener('click', function () { go('signin'); });
  tabs.signup.addEventListener('click', function () { go('signup'); });
  tabsEl.addEventListener('keydown', function (e) {
    if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return;
    var next = tabsEl.getAttribute('data-mode') === 'signin' ? 'signup' : 'signin';
    go(next); tabs[next].focus();
  });
  document.querySelectorAll('[data-go]').forEach(function (a) {
    a.addEventListener('click', function (e) { e.preventDefault(); go(a.getAttribute('data-go')); });
  });
  window.addEventListener('hashchange', function () { show(modeFromHash(), false); });
  show(modeFromHash(), false);

  // ---------- Teacher / Student chooser ----------
  var roleInputs = document.querySelectorAll('#roleChooser input[name="role"]');
  var siSub = document.getElementById('siSub');
  var suSub = document.getElementById('suSub');
  var suEmailHint = document.getElementById('suEmailHint');
  function currentRole() {
    var picked = document.querySelector('#roleChooser input[name="role"]:checked');
    return picked ? picked.value : 'TEACHER';
  }
  function applyRole() {
    var student = currentRole() === 'STUDENT';
    siSub.textContent = student ? 'Sign in to see your marks and details.'
                                : 'Sign in to manage your sections and students.';
    suSub.textContent = student ? 'Your teacher must have added you first.'
                                : 'Free, and it takes under a minute.';
    suEmailHint.hidden = !student;
    try { sessionStorage.setItem('sparRole', currentRole()); } catch (e) { /* private mode */ }
  }
  try {
    var saved = sessionStorage.getItem('sparRole');
    if (saved) {
      roleInputs.forEach(function (r) { r.checked = (r.value === saved); });
    }
  } catch (e) { /* ignore */ }
  roleInputs.forEach(function (r) { r.addEventListener('change', applyRole); });
  applyRole();

  // ---------- Show / hide password ----------
  document.querySelectorAll('.eye').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var input = btn.parentNode.querySelector('input');
      var reveal = input.type === 'password';
      input.type = reveal ? 'text' : 'password';
      btn.textContent = reveal ? 'Hide' : 'Show';
      btn.setAttribute('aria-pressed', String(reveal));
      btn.setAttribute('aria-label', reveal ? 'Hide password' : 'Show password');
    });
  });

  // ---------- Password strength meter ----------
  var pw = document.getElementById('suPassword');
  var meter = document.querySelector('.meter');
  var meterText = document.getElementById('meterText');
  var labels = ['At least 8 characters, with a letter and a number.', 'Weak: keep going', 'Fair: add more variety', 'Good', 'Strong'];
  pw.addEventListener('input', function () {
    var v = pw.value, s = 0;
    if (v.length >= 8) s++;
    if (v.length >= 12) s++;
    if (/[a-z]/.test(v) && /[A-Z]/.test(v)) s++;
    if (/\d/.test(v) && /[^A-Za-z0-9]/.test(v)) s++;
    if (v.length < 8 || !/[A-Za-z]/.test(v) || !/\d/.test(v)) s = Math.min(s, 1);
    if (!v) s = 0;
    meter.setAttribute('data-level', String(s));
    meterText.textContent = labels[s];
  });

  // ---------- Errors ----------
  var fieldIds = {
    signin: { email: 'siEmail', password: 'siPassword' },
    signup: { name: 'suName', email: 'suEmail', password: 'suPassword', confirm: 'suConfirm' }
  };
  function fieldError(mode, field, message) {
    var id = fieldIds[mode][field];
    var input = document.getElementById(id);
    var out = document.getElementById(id + 'Err');
    if (!input || !out) return false;
    input.setAttribute('aria-invalid', message ? 'true' : 'false');
    if (message) input.setAttribute('aria-describedby', id + 'Err'); else input.removeAttribute('aria-describedby');
    out.textContent = message || '';
    return true;
  }
  function clearErrors(mode) {
    Object.keys(fieldIds[mode]).forEach(function (f) { fieldError(mode, f, ''); });
    var box = forms[mode].querySelector('.alert');
    box.hidden = true; box.textContent = '';
  }
  function showAlert(mode, message) {
    var box = forms[mode].querySelector('.alert');
    box.textContent = message; box.hidden = false;
  }
  // Shows an error on its field if there is one, otherwise in the banner at the top.
  function fail(mode, field, message) {
    if (!(field && fieldError(mode, field, message))) showAlert(mode, message);
    var target = field && document.getElementById(fieldIds[mode][field]);
    if (target) target.focus();
  }

  // ---------- Client-side checks (the server repeats them) ----------
  function check(mode, v) {
    if (mode === 'signup') {
      if (v.name.trim().length < 2) return ['name', 'Enter your name.'];
    }
    if (!EMAIL.test(v.email.trim())) return ['email', 'Enter a valid email address.'];
    if (mode === 'signin') return v.password ? null : ['password', 'Enter your password.'];
    if (v.password.length < 8 || !/[A-Za-z]/.test(v.password) || !/\d/.test(v.password))
      return ['password', 'Use at least 8 characters with a letter and a number.'];
    if (v.password !== v.confirm) return ['confirm', 'The two passwords do not match.'];
    return null;
  }

  // ---------- Submit ----------
  function submit(mode, event) {
    event.preventDefault();
    clearErrors(mode);
    var f = forms[mode], v = {};
    Array.prototype.forEach.call(f.elements, function (el) { if (el.name) v[el.name] = el.value; });

    var problem = check(mode, v);
    if (problem) { fail(mode, problem[0], problem[1]); return; }

    var btn = f.querySelector('.submit');
    btn.disabled = true; btn.classList.add('busy');
    var body = new URLSearchParams(v);
    body.set('action', mode);
    body.set('role', currentRole());

    fetch('auth', {
      method: 'POST', credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'Accept': 'application/json' },
      body: body
    })
      .then(function (r) {
        return r.json().catch(function () { return { ok: false, error: 'Unexpected response from the server.' }; });
      })
      .then(function (data) {
        if (data.ok) {
          var box = f.querySelector('.alert');
          box.className = 'alert good'; box.textContent = 'Success. Taking you to your page...'; box.hidden = false;
          window.location.href = data.redirect || 'classes';
          return;
        }
        fail(mode, data.field, data.error || 'Something went wrong. Please try again.');
        btn.disabled = false; btn.classList.remove('busy');
      })
      .catch(function () {
        showAlert(mode, 'Could not reach the server. Open this page through Tomcat (http://localhost:8080/...), not as a file.');
        btn.disabled = false; btn.classList.remove('busy');
      });
  }
  forms.signin.addEventListener('submit', function (e) { submit('signin', e); });
  forms.signup.addEventListener('submit', function (e) { submit('signup', e); });

  // Clear a field's error as soon as the person edits it.
  Object.keys(fieldIds).forEach(function (mode) {
    Object.keys(fieldIds[mode]).forEach(function (f) {
      document.getElementById(fieldIds[mode][f]).addEventListener('input', function () { fieldError(mode, f, ''); });
    });
  });

  // Already signed in? Skip the form.
  fetch('auth?action=session', { credentials: 'same-origin' })
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (d) { if (d && d.authenticated) window.location.replace(d.redirect || 'classes'); })
    .catch(function () { /* stay on the form */ });
})();
