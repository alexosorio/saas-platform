import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaUpload, FaDownload, FaFileCsv, FaCheckCircle, FaExclamationTriangle, FaArrowLeft } from 'react-icons/fa';
import productService from '../../services/productService';

const PLANTILLA = `nombre,sku,codigo_barras,tipo_producto,categoria,marca,proveedor,precio_compra,precio_venta,color,descripcion,imei1,imei2,bodega,stock_inicial,activo
iPhone 15 Pro 256GB,IP15P-256,194253000001,movil,Celulares,Apple,Distribuidora XYZ,3500000,4200000,Negro,Sellado,350000000000014,350000000000022,Principal,1,si
iPhone 15 Pro 256GB,IP15P-256,194253000001,movil,Celulares,Apple,Distribuidora XYZ,3550000,4250000,Azul titanio,Sellado,350000000000030,350000000000048,Principal,1,si
Cargador USB-C 20W,CAR-USBC20,7701234567890,accesorio,Accesorios,Genérico,Distribuidora XYZ,12000,25000,Blanco,Carga rápida,,,Principal,100,si`;

const ProductBulkImport = () => {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const descargarPlantilla = () => {
    const blob = new Blob([PLANTILLA], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'plantilla_productos.csv'; a.click();
    URL.revokeObjectURL(url);
  };

  const subir = async () => {
    if (!file) { setError('Selecciona un archivo CSV'); return; }
    setError(''); setResult(null); setLoading(true);
    try {
      const r = await productService.bulkUpload(file);
      setResult(r);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al procesar el archivo');
    } finally { setLoading(false); }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <button onClick={() => navigate('/catalog/products')} className="text-gray-500 mb-4 flex items-center gap-2"><FaArrowLeft /> Volver a productos</button>
      <h1 className="text-3xl font-bold mb-6 flex items-center gap-3"><FaFileCsv className="text-primary-600" /> Carga masiva de productos</h1>

      <div className="card mb-4">
        <h2 className="font-bold mb-2">1. Descarga la plantilla</h2>
        <p className="text-sm text-gray-600 mb-3">
          Usa este CSV como base. Columnas: nombre, sku, codigo_barras, tipo_producto (movil/accesorio), categoria, marca,
          proveedor, precio_compra, precio_venta, color, descripcion, imei1, imei2, bodega, stock_inicial, activo.
          El <strong>codigo_barras</strong> es el del empaque de la unidad (no del producto); en un lote de accesorios
          se asigna a todas las unidades de esa fila.
          Cada fila es una unidad (o un lote de accesorios): <strong>precio_compra</strong> (obligatorio),
          precio_venta y color se guardan en esa unidad, no en el producto. Para otras características usa
          columnas <code>carac_&lt;codigo&gt;</code>, por ejemplo <code>carac_almacenamiento</code>.
        </p>
        <button onClick={descargarPlantilla} className="btn-secondary flex items-center gap-2">
          <FaDownload /> Descargar plantilla CSV
        </button>
        <div className="mt-4 text-xs text-gray-500 space-y-1 bg-gray-50 p-3 rounded">
          <p><strong>Móvil:</strong> una fila por aparato (con sus IMEIs). stock_inicial = 1. Varias unidades del mismo modelo = varias filas con el mismo SKU.</p>
          <p><strong>Accesorio:</strong> una fila con stock_inicial = cantidad. Los IMEI van vacíos.</p>
          <p>La categoría debe existir previamente. Marca y proveedor se asocian por nombre si existen.</p>
        </div>
      </div>

      <div className="card mb-4">
        <h2 className="font-bold mb-2">2. Sube tu archivo</h2>
        <div className="flex items-center gap-3">
          <input type="file" accept=".csv,text/csv" onChange={e => { setFile(e.target.files?.[0] || null); setResult(null); }}
            className="flex-1" />
          <button onClick={subir} disabled={loading || !file} className="btn-primary flex items-center gap-2">
            <FaUpload /> {loading ? 'Procesando...' : 'Cargar'}
          </button>
        </div>
        {error && <div className="mt-3 bg-red-100 text-red-700 px-4 py-2 rounded">{error}</div>}
      </div>

      {result && (
        <div className="card">
          <h2 className="font-bold mb-3 flex items-center gap-2 text-green-700"><FaCheckCircle /> Resultado</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
            <div className="bg-green-50 p-3 rounded text-center">
              <div className="text-2xl font-bold text-green-700">{result.productosCreados}</div>
              <div className="text-xs text-gray-600">Productos creados</div>
            </div>
            <div className="bg-blue-50 p-3 rounded text-center">
              <div className="text-2xl font-bold text-primary-700">{result.productosExistentes}</div>
              <div className="text-xs text-gray-600">Ya existían (+ unidades)</div>
            </div>
            <div className="bg-purple-50 p-3 rounded text-center">
              <div className="text-2xl font-bold text-purple-700">{result.unidadesCreadas}</div>
              <div className="text-xs text-gray-600">Unidades creadas</div>
            </div>
            <div className={`p-3 rounded text-center ${result.filasConError > 0 ? 'bg-red-50' : 'bg-gray-50'}`}>
              <div className={`text-2xl font-bold ${result.filasConError > 0 ? 'text-red-700' : 'text-gray-400'}`}>{result.filasConError}</div>
              <div className="text-xs text-gray-600">Filas con error</div>
            </div>
          </div>
          {result.errores?.length > 0 && (
            <div className="bg-red-50 border border-red-200 rounded p-3">
              <p className="font-semibold text-red-700 mb-2 flex items-center gap-2"><FaExclamationTriangle /> Errores:</p>
              <ul className="text-sm text-red-700 space-y-1 max-h-48 overflow-y-auto">
                {result.errores.map((e, i) => <li key={i}>• {e}</li>)}
              </ul>
            </div>
          )}
          <button onClick={() => navigate('/catalog/products')} className="btn-primary mt-4">Ver productos</button>
        </div>
      )}
    </div>
  );
};

export default ProductBulkImport;
