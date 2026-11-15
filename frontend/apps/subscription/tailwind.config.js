import { primary } from '../../packages/ui-shell/theme.js';
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    '../../packages/ui-shell/src/**/*.{js,jsx}',"./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        // Paleta de marca central (#693a84 / #a871bd): packages/ui-shell/theme.js
        primary,
      },
    },
  },
  plugins: [],
}
