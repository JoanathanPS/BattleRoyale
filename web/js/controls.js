// Controls diagram: keys light up when you press them. Purely a visual aid; it never blocks a key.
(() => {
  const root = document.getElementById("controls");
  if (!root) return;
  const caps = [...root.querySelectorAll("kbd[data-code]")];
  const byCode = new Map();
  caps.forEach((k) => k.dataset.code.split(" ").forEach((c) => byCode.set(c, k)));
  const wheel = root.querySelector("[data-wheel]");
  const click = root.querySelector('[data-mouse="0"]');
  const pointer = root.querySelector("[data-pointer]");

  document.addEventListener("keydown", (e) => {
    const k = byCode.get(e.code);
    if (k) k.classList.add("is-down");
  });
  document.addEventListener("keyup", (e) => {
    const k = byCode.get(e.code);
    if (k) k.classList.remove("is-down");
  });
  window.addEventListener("blur", () => caps.forEach((k) => k.classList.remove("is-down")));

  root.addEventListener("pointerdown", (e) => {
    if (e.button === 0 && click) click.classList.add("is-down");
  });
  document.addEventListener("pointerup", () => click && click.classList.remove("is-down"));

  let wheelTimer = 0;
  root.addEventListener(
    "wheel",
    () => {
      if (!wheel) return;
      wheel.classList.add("is-down");
      clearTimeout(wheelTimer);
      wheelTimer = setTimeout(() => wheel.classList.remove("is-down"), 220);
    },
    { passive: true }
  );

  let moveTimer = 0;
  root.addEventListener(
    "pointermove",
    () => {
      if (!pointer) return;
      pointer.classList.add("is-down");
      clearTimeout(moveTimer);
      moveTimer = setTimeout(() => pointer.classList.remove("is-down"), 160);
    },
    { passive: true }
  );
})();
