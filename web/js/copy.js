// Copy buttons for the run command and the checksum.
(() => {
  document.querySelectorAll("[data-copy]").forEach((btn) => {
    const target = document.querySelector(btn.dataset.copy);
    if (!target) return;
    const label = btn.textContent;
    btn.addEventListener("click", async () => {
      const text = target.textContent.trim();
      let ok = false;
      try {
        await navigator.clipboard.writeText(text);
        ok = true;
      } catch (e) {
        // Clipboard API blocked: select the text so Ctrl+C works.
        const r = document.createRange();
        r.selectNodeContents(target);
        const s = getSelection();
        s.removeAllRanges();
        s.addRange(r);
      }
      btn.textContent = ok ? "Copied" : "Press Ctrl+C";
      setTimeout(() => (btn.textContent = label), 2000);
    });
  });
})();
