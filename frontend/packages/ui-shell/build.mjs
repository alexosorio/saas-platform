import { build } from 'esbuild';
await build({
  entryPoints: ['src/index.js'],
  bundle: true,
  format: 'esm',
  outfile: 'dist/index.js',
  jsx: 'automatic',
  loader: { '.js': 'jsx', '.jsx': 'jsx' },
  external: ['react', 'react-dom', 'react-router-dom', 'react-icons', 'react-icons/fa', 'axios'],
  logLevel: 'info',
});
console.log('ui-shell built -> dist/index.js');
