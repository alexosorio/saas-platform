// Paleta de marca: #693a84 (principal) y #a871bd (acento). Fuente única para las 3 apps.
export const primary = {
  50: '#f6f1f8',
  100: '#eee3f2',
  200: '#dcc6e5',
  300: '#c29cd1',
  400: '#a871bd',
  500: '#8856a0',
  600: '#693a84',
  700: '#593170',
  800: '#4a295c',
  900: '#3a2049',
};

// Colores para usar fuera de Tailwind (gráficas, estilos en línea)
export const brand = {
  principal: primary[600],   // #693a84
  acento: primary[400],      // #a871bd
  suave: primary[100],
  oscuro: primary[800],
};
