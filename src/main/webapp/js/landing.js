// Landing page behaviour: nav glass effect, mobile menu, reveal animations,
// card spotlight, and a live class snapshot read from the /students page.
(function () {
  var nav = document.getElementById('nav');
  var toggle = document.getElementById('navToggle');
  var links = document.getElementById('navLinks');

  // Nav stays fully transparent at the top and turns to light glass once scrolled.
  function onScroll() { nav.classList.toggle('scrolled', window.scrollY > 20); }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  // Mobile menu
  function setMenu(open) {
    links.classList.toggle('open', open);
    toggle.setAttribute('aria-expanded', String(open));
    toggle.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
  }
  toggle.addEventListener('click', function () {
    setMenu(toggle.getAttribute('aria-expanded') !== 'true');
  });
  links.addEventListener('click', function (e) { if (e.target.tagName === 'A') setMenu(false); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') setMenu(false); });

  // Reveal on scroll
  var reveals = document.querySelectorAll('.reveal');
  if ('IntersectionObserver' in window) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); }
      });
    }, { threshold: 0.15 });
    reveals.forEach(function (el) { io.observe(el); });
  } else {
    reveals.forEach(function (el) { el.classList.add('in'); });
  }

  // Sample report card: animate the bars in
  window.addEventListener('load', function () {
    setTimeout(function () {
      document.querySelectorAll('.bar i[data-w]').forEach(function (b) {
        b.style.width = b.getAttribute('data-w') + '%';
      });
    }, 250);
  });

  // Soft spotlight that follows the pointer over each card
  document.querySelectorAll('.card').forEach(function (card) {
    card.addEventListener('pointermove', function (e) {
      var r = card.getBoundingClientRect();
      card.style.setProperty('--mx', (e.clientX - r.left) + 'px');
      card.style.setProperty('--my', (e.clientY - r.top) + 'px');
    });
  });

  // Live snapshot: read the summary cards from the class report page.
  // If the server is not reachable (e.g. file opened directly) the dashes stay.
  function setLive(key, text) {
    var el = document.querySelector('[data-live="' + key + '"]');
    if (el) el.textContent = text;
  }
  function countUp(el, target) {
    if (isNaN(target) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      el.textContent = target; return;
    }
    var start = null;
    (function step(ts) {
      if (!start) start = ts;
      var p = Math.min((ts - start) / 800, 1);
      el.textContent = Math.round(target * p);
      if (p < 1) requestAnimationFrame(step);
    })(performance.now());
  }

  fetch('students', { credentials: 'same-origin' })
    .then(function (r) { if (!r.ok) throw new Error(r.status); return r.text(); })
    .then(function (html) {
      var doc = new DOMParser().parseFromString(html, 'text/html');
      var found = {};
      doc.querySelectorAll('.summary .stat').forEach(function (s) {
        var label = s.querySelector('.label'), value = s.querySelector('.value');
        if (label && value) found[label.textContent.trim()] = value.textContent.trim();
      });
      var count = found['Students'] !== undefined ? parseInt(found['Students'], 10) : 0;
      countUp(document.querySelector('[data-live="students"]'), count);
      setLive('average', found['Class average'] || '\u2014');
      setLive('pass', found['Passing every subject'] || '\u2014');
    })
    .catch(function () { /* keep the dashes */ });
})();