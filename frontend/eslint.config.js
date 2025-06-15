import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'
import oxlint from 'eslint-plugin-oxlint'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

export default [
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,jsx,vue}'],
  },

  {
    name: 'app/files-to-ignore',
    ignores: ['**/dist/**', '**/dist-ssr/**', '**/coverage/**', '**/node_modules/**', '**/*.config.{js,cjs,mjs}', '**/tailwind.config.cjs', '**/postcss.config.cjs'],
  },

  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  ...oxlint.configs['flat/recommended'],

  // Custom rules for unused imports and code quality
  {
    rules: {
      'no-unused-vars': ['error', {
        argsIgnorePattern: '^_',
        varsIgnorePattern: '^_',
        caughtErrorsIgnorePattern: '^_'
      }],
      'vue/no-unused-vars': ['error', {
        ignorePattern: '^_'
      }],
      'vue/multi-word-component-names': 'off', // Allow single-word component names for Vietnamese business terms
      'vue/no-reserved-component-names': 'off', // Allow reserved names when needed
      'no-case-declarations': 'error',
      'prefer-const': 'error',
      'no-var': 'error',
    },
  },

  skipFormatting,
]
