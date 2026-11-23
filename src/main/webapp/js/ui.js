"use strict";

// Keep navigation usable when the optional Bootstrap CDN cannot be reached.
const menuButton = document.querySelector('[aria-controls="navbar-menu"]');
const menu = document.querySelector("#navbar-menu");
if (menuButton && menu) {
  menuButton.addEventListener("click", () => {
    if (window.bootstrap?.Collapse) return;
    const expanded = menuButton.getAttribute("aria-expanded") !== "true";
    menuButton.setAttribute("aria-expanded", String(expanded));
    menu.classList.toggle("show", expanded);
  });

  menu.addEventListener("click", event => {
    if (!event.target.closest("a, #logout-button") || window.innerWidth >= 992) return;
    if (window.bootstrap?.Collapse) {
      window.bootstrap.Collapse.getInstance(menu)?.hide();
    } else {
      menu.classList.remove("show");
      menuButton.setAttribute("aria-expanded", "false");
    }
  });
}
