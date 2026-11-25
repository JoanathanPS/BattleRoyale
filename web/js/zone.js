// Header minimap: the safe-zone ring shrinks one step per section, one step per section.
// State-driven (the section in view), so the ring matches what you are reading. Also sets nav aria-current.
(() => {
  const ids = ["drop", "play", "loadout", "results", "download"];
  const labels = [
    ["Drop", "Island overview"],
    ["Zone 1/4", "Play"],
    ["Zone 2/4", "Loadout"],
    ["Zone 3/4", "Results"],
    ["Zone 4/4", "Download"],
  ];
  const sections = ids.map((id) => document.getElementById(id)).filter(Boolean);
  const bar = document.getElementById("top");
  const zoneEl = document.getElementById("hud-zone");
  const nameEl = document.getElementById("hud-name");
  const links = [...document.querySelectorAll('.bar nav a[href^="#"]')];
  if (!sections.length || !bar) return;

  let current = -1;
  function set(i) {
    if (i === current) return;
    current = i;
    bar.dataset.zone = String(i);
    if (zoneEl) zoneEl.textContent = labels[i][0];
    if (nameEl) nameEl.textContent = labels[i][1];
    links.forEach((a) => {
      if (a.getAttribute("href") === "#" + ids[i]) a.setAttribute("aria-current", "true");
      else a.removeAttribute("aria-current");
    });
  }

  // The section crossing the middle of the viewport is the current one.
  function pick() {
    const mid = window.innerHeight * 0.45;
    let idx = 0;
    sections.forEach((s, i) => {
      if (s.getBoundingClientRect().top <= mid) idx = i;
    });
    if (window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 4) {
      idx = sections.length - 1;
    }
    set(idx);
  }

  let ticking = false;
  window.addEventListener(
    "scroll",
    () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        ticking = false;
        pick();
      });
    },
    { passive: true }
  );
  window.addEventListener("resize", pick);
  pick();
})();
