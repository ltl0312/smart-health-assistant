/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Noto Sans SC"', '"Microsoft YaHei"', 'system-ui', 'sans-serif'],
        serif: ['"Noto Serif SC"', 'Georgia', 'serif'],
      },
      colors: {
        background: { light: '#F6FAF8', dark: '#07110d' },
        surface: { light: '#FFFFFF', dark: '#111a16' },
        medal: { gold: '#fef08a', silver: '#e2e8f0', bronze: '#ffedd5' },
      },
      boxShadow: {
        'premium': '0 18px 50px -30px rgba(15, 118, 110, 0.35)',
        'premium-dark': '0 18px 50px -30px rgba(20, 184, 166, 0.35)',
        'premium-hover': '0 22px 60px -32px rgba(15, 118, 110, 0.42)',
      },
      borderRadius: { '2xl': '1.25rem', '3xl': '1.75rem', '4xl': '2rem' },
      transitionTimingFunction: { 'apple': 'cubic-bezier(0.25, 0.1, 0.25, 1)' },
    },
  },
  plugins: [],
}
