import { initTheme } from "./theme.js";
initTheme();

import { getAccessToken } from "./auth.js";

const token = await getAccessToken();
window.location.href = token ? "/home.html" : "/login.html";
