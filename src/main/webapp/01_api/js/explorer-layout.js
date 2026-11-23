"use strict";

const explorerTabs = [...document.querySelectorAll('.explorer-tabs [role="tab"]')];

function selectExplorerTab(selected) {
  for (const tab of explorerTabs) {
    const active = tab === selected;
    tab.setAttribute("aria-selected", String(active));
    tab.tabIndex = active ? 0 : -1;
    document.getElementById(tab.getAttribute("aria-controls")).hidden = !active;
  }
}

for (const tab of explorerTabs) {
  tab.addEventListener("click", () => selectExplorerTab(tab));
  tab.addEventListener("keydown", event => {
    const index = explorerTabs.indexOf(tab);
    const next = {
      ArrowRight: explorerTabs[(index + 1) % explorerTabs.length],
      ArrowLeft: explorerTabs[(index + explorerTabs.length - 1) % explorerTabs.length],
      Home: explorerTabs[0],
      End: explorerTabs.at(-1),
    }[event.key];
    if (!next) return;
    event.preventDefault();
    selectExplorerTab(next);
    next.focus();
  });
}

// Fit the frame to its layout; long desktop tool lists scroll inside their panels.
if (window.frameElement) {
  document.body.classList.add("embedded-map");

  // Content changes and stacked phone layouts must not leave a clipped frame.
  const frame = window.frameElement;
  const explorer = document.querySelector(".explorer");
  let resizeFrame = 0;
  const syncFrameHeight = () => {
    cancelAnimationFrame(resizeFrame);
    resizeFrame = requestAnimationFrame(() => {
      const height = Math.ceil(explorer.getBoundingClientRect().height);
      if (height > 0) frame.style.height = `${height + 2}px`;
    });
  };
  new ResizeObserver(syncFrameHeight).observe(explorer);
  window.addEventListener("resize", syncFrameHeight);
  syncFrameHeight();
}
