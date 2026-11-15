import { useRef, useState } from 'react';
import { FaImage, FaTrash, FaUpload } from 'react-icons/fa';

const MAX_ANCHO = 480;
const MAX_ALTO = 240;
const MAX_BYTES = 280 * 1024;          // el backend acepta hasta 300 KB
const MAX_ARCHIVO = 3 * 1024 * 1024;   // archivo de entrada
const ACEPTA = 'image/svg+xml,image/png,image/jpeg,image/webp';

// Bytes reales de un data URL base64
const pesoDataUrl = (d) => Math.floor((d.length - d.indexOf(',') - 1) * 3 / 4);

const leerComo = (file, modo) => new Promise((ok, mal) => {
  const r = new FileReader();
  r.onload = () => ok(r.result);
  r.onerror = () => mal(new Error('No se pudo leer el archivo'));
  modo === 'texto' ? r.readAsText(file) : r.readAsDataURL(file);
});

const cargarImagen = (src) => new Promise((ok, mal) => {
  const img = new Image();
  img.onload = () => ok(img);
  img.onerror = () => mal(new Error('No se pudo abrir la imagen'));
  img.src = src;
});

/** Un SVG sin width/height no se dibuja bien en canvas: se le ponen a partir del viewBox. */
const svgConTamano = (texto) => {
  const doc = new DOMParser().parseFromString(texto, 'image/svg+xml');
  const svg = doc.documentElement;
  if (!svg || svg.nodeName.toLowerCase() !== 'svg') throw new Error('El archivo SVG no es válido');
  const vb = (svg.getAttribute('viewBox') || '').split(/[\s,]+/).map(Number);
  const w = parseFloat(svg.getAttribute('width')) || (vb.length === 4 ? vb[2] : 0) || MAX_ANCHO;
  const h = parseFloat(svg.getAttribute('height')) || (vb.length === 4 ? vb[3] : 0) || MAX_ALTO;
  svg.setAttribute('width', w);
  svg.setAttribute('height', h);
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(new XMLSerializer().serializeToString(svg));
};

/**
 * Convierte cualquier logo (SVG, PNG, JPG, WEBP) a PNG con fondo transparente, máximo 480×240 y
 * menos de 300 KB. PNG/JPEG es lo único que todos los clientes de correo muestran (SVG no).
 */
export async function normalizarLogo(file) {
  if (!file) throw new Error('No se seleccionó ningún archivo');
  if (!ACEPTA.split(',').includes(file.type)) throw new Error('Usa un logo en SVG, PNG, JPG o WEBP');
  if (file.size > MAX_ARCHIVO) throw new Error('El archivo es muy grande (máximo 3 MB)');

  const src = file.type === 'image/svg+xml' ? svgConTamano(await leerComo(file, 'texto')) : await leerComo(file, 'dataurl');
  const img = await cargarImagen(src);
  const w0 = img.naturalWidth || img.width, h0 = img.naturalHeight || img.height;
  if (!w0 || !h0) throw new Error('No se pudo determinar el tamaño de la imagen');

  // SVG es vectorial: se rasteriza al tamaño máximo; las imágenes no se agrandan
  let escala = Math.min(MAX_ANCHO / w0, MAX_ALTO / h0, file.type === 'image/svg+xml' ? Infinity : 1);
  for (let intento = 0; intento < 6; intento++) {
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(w0 * escala));
    canvas.height = Math.max(1, Math.round(h0 * escala));
    const ctx = canvas.getContext('2d');
    ctx.imageSmoothingQuality = 'high';
    ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
    const png = canvas.toDataURL('image/png');
    if (pesoDataUrl(png) <= MAX_BYTES) return png;
    escala *= 0.75;
  }
  throw new Error('No se pudo reducir el logo a menos de 300 KB; prueba con una imagen más simple');
}

/**
 * Selector de logo con vista previa. value: data URL (o null). onChange(dataUrl | null).
 * El logo es opcional: "Quitar" lo deja vacío.
 */
export default function LogoUploader({ value, onChange, disabled = false, ayuda }) {
  const input = useRef(null);
  const [error, setError] = useState('');
  const [procesando, setProcesando] = useState(false);

  const elegir = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    setError(''); setProcesando(true);
    try { onChange(await normalizarLogo(file)); }
    catch (err) { setError(err.message); }
    finally { setProcesando(false); }
  };

  return (
    <div>
      <div className="flex items-center gap-4">
        <div className="w-40 h-20 shrink-0 rounded-lg border border-dashed border-gray-300 bg-white flex items-center justify-center overflow-hidden">
          {value
            ? <img src={value} alt="Logo" className="max-w-full max-h-full object-contain p-1" />
            : <FaImage className="text-gray-300" size={28} />}
        </div>
        <div className="flex flex-col gap-2">
          <button type="button" disabled={disabled || procesando} onClick={() => input.current?.click()}
            className="btn-secondary text-sm flex items-center gap-2 disabled:opacity-60">
            <FaUpload /> {procesando ? 'Procesando…' : value ? 'Cambiar logo' : 'Subir logo'}
          </button>
          {value && (
            <button type="button" disabled={disabled || procesando} onClick={() => { setError(''); onChange(null); }}
              className="text-sm text-red-600 hover:text-red-800 flex items-center gap-1.5 disabled:opacity-60">
              <FaTrash size={12} /> Quitar
            </button>
          )}
        </div>
        <input ref={input} type="file" accept={ACEPTA} className="hidden" onChange={elegir} />
      </div>
      <p className="text-xs text-gray-500 mt-2">
        {ayuda || 'SVG, PNG, JPG o WEBP. Se ajusta automáticamente; mejor si es horizontal y con fondo transparente.'}
      </p>
      {error && <p className="text-xs text-red-600 mt-1">{error}</p>}
    </div>
  );
}
