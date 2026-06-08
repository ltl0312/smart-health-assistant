/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        serif: ['Georgia', 'serif'],
      },
      colors: {
        background: { light: '#FBFBFD', dark: '#0a0a0a' },
        surface: { light: '#FFFFFF', dark: '#171717' },
        medal: { gold: '#fef08a', silver: '#e2e8f0', bronze: '#ffedd5' },
      },
      boxShadow: {
        'premium': '0 4px 24px -4px rgba(0, 0, 0, 0.05)',
        'premium-dark': '0 4px 24px -4px rgba(0, 0, 0, 0.4)',
        'premium-hover': '0 12px 32px -4px rgba(0, 0, 0, 0.08)',
      },
      borderRadius: { '2xl': '1.25rem', '3xl': '1.75rem', '4xl': '2rem' },
      transitionTimingFunction: { 'apple': 'cubic-bezier(0.25, 0.1, 0.25, 1)' },
    },
  },
  plugins: [],
}
