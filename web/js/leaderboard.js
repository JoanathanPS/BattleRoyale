// Live leaderboard from the same Supabase table the game writes to (match_results).
// The anon key is a public, read-scoped key, as it was on the previous page.
(() => {
  const SUPABASE_URL = "https://ngssqooaxzaqobmowfrk.supabase.co";
  const SUPABASE_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5nc3Nxb29heHphcW9ibW93ZnJrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5ODY1ODEsImV4cCI6MjEwMjU2MjU4MX0.d3uagDjBQoSL8pv5RGZRakv8uimHnNyEqXTsIGhpnyE";

  const root = document.getElementById("lb");
  const body = document.getElementById("lb-body");
  const table = document.getElementById("lb-table");
  const state = document.getElementById("lb-state");
  const msg = document.getElementById("lb-msg");
  const retry = document.getElementById("lb-retry");
  const refresh = document.getElementById("lb-refresh");
  const updated = document.getElementById("lb-updated");
  const filters = [...document.querySelectorAll("[data-diff]")];
  if (!root) return;

  let diff = "";
  let controller = null;

  function show(kind, text) {
    // kind: "loading" | "empty" | "error" | null (data)
    root.setAttribute("aria-busy", kind === "loading" ? "true" : "false");
    if (!kind) {
      state.removeAttribute("data-show");
      table.hidden = false;
      return;
    }
    table.hidden = true;
    state.setAttribute("data-show", "");
    msg.textContent = text;
    retry.hidden = kind !== "error";
  }

  function td(cls, text, label) {
    const c = document.createElement("td");
    if (cls) c.className = cls;
    if (label) c.dataset.label = label;
    c.textContent = text;
    return c;
  }

  function render(rows) {
    body.replaceChildren();
    rows.forEach((r, i) => {
      const tr = document.createElement("tr");
      if (i < 3) tr.className = "top";
      tr.appendChild(td("lb-rank", String(i + 1)));

      const p = document.createElement("td");
      p.className = "lb-player";
      const name = document.createElement("b");
      name.textContent = r.player_name || "Survivor";
      p.appendChild(name);
      const when = r.created_at ? new Date(r.created_at) : null;
      if (when && !isNaN(when)) {
        const small = document.createElement("small");
        small.textContent = when.toLocaleDateString(undefined, { month: "short", day: "numeric" });
        p.appendChild(small);
      }
      tr.appendChild(p);

      tr.appendChild(td("lb-mode stat", cap(r.difficulty || "MEDIUM"), "Mode"));
      tr.appendChild(td("lb-kills stat num", String(r.kills || 0), "Kills"));
      const secs = r.survival_time_seconds || 0;
      tr.appendChild(td("lb-time stat num", Math.floor(secs / 60) + "m " + String(secs % 60).padStart(2, "0") + "s", "Survived"));
      tr.appendChild(td("lb-score stat num", (r.score || 0).toLocaleString(), "Score"));

      const res = document.createElement("td");
      res.className = "lb-res";
      const chip = document.createElement("span");
      const win = r.result === "WIN";
      chip.className = "chip" + (win ? " chip--win" : "");
      chip.textContent = win ? "Win" : "Loss";
      res.appendChild(chip);
      tr.appendChild(res);
      body.appendChild(tr);
    });
  }

  const cap = (s) => s.charAt(0) + s.slice(1).toLowerCase();

  async function load() {
    if (controller) controller.abort();
    controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);
    show("loading", "Loading results...");
    refresh.disabled = true;
    const cols = "player_name,difficulty,score,kills,survival_time_seconds,result,created_at";
    const q = `select=${cols}&order=score.desc&limit=10` + (diff ? `&difficulty=eq.${diff}` : "");
    try {
      const res = await fetch(`${SUPABASE_URL}/rest/v1/match_results?${q}`, {
        headers: { apikey: SUPABASE_KEY, Authorization: `Bearer ${SUPABASE_KEY}` },
        signal: controller.signal,
      });
      if (!res.ok) throw new Error("HTTP " + res.status);
      const rows = await res.json();
      if (!Array.isArray(rows) || rows.length === 0) {
        show("empty", diff ? `No ${diff.toLowerCase()} matches recorded yet.` : "No matches recorded yet.");
      } else {
        render(rows);
        show(null);
      }
      updated.textContent = "Updated " + new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    } catch (err) {
      show("error", "Could not load the results. Check your connection and try again.");
      updated.textContent = "";
    } finally {
      clearTimeout(timeout);
      refresh.disabled = false;
    }
  }

  filters.forEach((b) =>
    b.addEventListener("click", () => {
      diff = b.dataset.diff;
      filters.forEach((x) => x.setAttribute("aria-pressed", String(x === b)));
      load();
    })
  );
  refresh.addEventListener("click", load);
  retry.addEventListener("click", load);

  load();
  setInterval(() => {
    if (document.visibilityState === "visible") load();
  }, 60000);
})();
