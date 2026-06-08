/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // All colors use CSS variables for theme switching
        canvas: 'var(--color-canvas)',
        'surface-1': 'var(--color-surface-1)',
        'surface-2': 'var(--color-surface-2)',
        'surface-3': 'var(--color-surface-3)',
        'surface-4': 'var(--color-surface-4)',
        hairline: 'var(--color-hairline)',
        'hairline-strong': 'var(--color-hairline-strong)',
        ink: 'var(--color-ink)',
        'ink-muted': 'var(--color-ink-muted)',
        'ink-subtle': 'var(--color-ink-subtle)',
        'ink-tertiary': 'var(--color-ink-tertiary)',
        primary: 'var(--color-primary)',
        'primary-hover': 'var(--color-primary-hover)',
        'primary-focus': 'var(--color-primary-focus)',
        success: 'var(--color-success)',
        'inverse-canvas': 'var(--color-inverse-canvas)',
        'inverse-ink': 'var(--color-inverse-ink)',
      },
      fontFamily: {
        sans: ['Inter', 'SF Pro Display', '-apple-system', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        'xs': '4px', 'sm': '6px', 'md': '8px',
        'lg': '12px', 'xl': '16px', 'xxl': '24px', 'pill': '9999px',
      },
      spacing: { 'section': '96px' },
      letterSpacing: { 'tighter': '-0.05em', 'tight': '-0.02em' },
    },
  },
  plugins: [],
}
