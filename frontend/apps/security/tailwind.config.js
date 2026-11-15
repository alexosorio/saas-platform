import { primary } from '../../packages/ui-shell/theme.js';
/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
    "../../packages/ui-shell/src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary,
      },
    },
  },
  plugins: [],
}
