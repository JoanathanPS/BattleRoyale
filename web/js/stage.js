// Game stage: idle, loading, running, failed, plus fullscreen and restart.
// The game itself is web/game.html in an iframe. Nothing here touches it.
(() => {
  const stage = document.getElementById("stage");
  const frame = document.getElementById("game-frame");
  const status = document.getElementById("stage-status");
  const note = document.getElementById("stage-note");
  const launchBtn = document.getElementById("btn-launch");
  const desktopLink = document.getElementById("stage-desktop");
  const fsBtn = document.getElementById("btn-fs");
  const restartBtn = document.getElementById("btn-restart");
  const fileNotice = document.getElementById("file-notice");
  if (!stage || !frame) return;

  const GAME_URL = "game.html";
  const LOAD_TIMEOUT_MS = 30000;
  const isFile = location.protocol === "file:";
  const isTouch = window.matchMedia("(hover: none) and (pointer: coarse)").matches;
  let timer = 0;

  if (isFile && fileNotice) fileNotice.hidden = false;

  function setState(state) {
    stage.dataset.state = state;
    const running = state === "running";
    const busy = state === "loading" || running;
    fsBtn.disabled = !running;
    restartBtn.disabled = !busy;
    scrimUi(state);
  }

  function scrimUi(state) {
    const texts = {
      idle: ["Ready", "Click to load the game. It uses your keyboard and mouse.", "Drop in"],
      loading: ["Loading", "Loading the game. This can take a few seconds.", ""],
      running: ["Running", "", ""],
      failed: ["Did not load", "The game did not load. Check your connection and try again, or download the desktop build.", "Try again"],
    };
    const [s, n, b] = texts[state];
    status.textContent = s;
    note.textContent = n;
    launchBtn.hidden = !b;
    if (b) launchBtn.textContent = b;
    desktopLink.hidden = !(state === "failed" || isTouch || isFile);
  }

  function launch() {
    if (isTouch) return;
    if (isFile) {
      note.textContent = "The game cannot load from a file. Run Run-Local-Server.bat or use the hosted page.";
      status.textContent = "Needs a web server";
      return;
    }
    setState("loading");
    frame.hidden = false;
    frame.src = GAME_URL;
    clearTimeout(timer);
    timer = setTimeout(() => setState("failed"), LOAD_TIMEOUT_MS);
  }

  frame.addEventListener("load", () => {
    if (frame.getAttribute("src") !== GAME_URL) return;
    clearTimeout(timer);
    setState("running");
    frame.focus();
  });

  function restart() {
    clearTimeout(timer);
    frame.src = "about:blank";
    setState("loading");
    setTimeout(() => {
      frame.src = GAME_URL;
      timer = setTimeout(() => setState("failed"), LOAD_TIMEOUT_MS);
    }, 150);
  }

  function toggleFullscreen() {
    const fsEl = document.fullscreenElement || document.webkitFullscreenElement;
    if (fsEl) {
      (document.exitFullscreen || document.webkitExitFullscreen).call(document);
    } else {
      (stage.requestFullscreen || stage.webkitRequestFullscreen).call(stage);
    }
  }
  function syncFsLabel() {
    const on = document.fullscreenElement || document.webkitFullscreenElement;
    fsBtn.textContent = on ? "Exit fullscreen" : "Fullscreen";
  }
  document.addEventListener("fullscreenchange", syncFsLabel);
  document.addEventListener("webkitfullscreenchange", syncFsLabel);

  launchBtn.addEventListener("click", launch);
  restartBtn.addEventListener("click", restart);
  fsBtn.addEventListener("click", toggleFullscreen);
  document.querySelectorAll("[data-launch]").forEach((a) => a.addEventListener("click", () => {
    // The link scrolls to the stage; start the game as well.
    if (stage.dataset.state === "idle" || stage.dataset.state === "failed") launch();
  }));

  if (isTouch) {
    stage.dataset.input = "touch";
    note.textContent = "This game needs a keyboard and a mouse. Open this page on a computer, or get the desktop build.";
    launchBtn.hidden = true;
    desktopLink.hidden = false;
  } else {
    setState("idle");
  }
})();
