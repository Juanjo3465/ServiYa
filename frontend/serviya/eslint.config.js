import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    // M49: el frontend quedó 0 warnings; subimos exhaustive-deps a ERROR para blindar contra regresiones
    // (un fetch-en-efecto con deps incompletas vuelve a romper el gate de lint).
    rules: {
      'react-hooks/exhaustive-deps': 'error',
    },
  },
])
