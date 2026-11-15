import { Link } from 'react-router-dom';

const CatalogHome = () => {
  const cards = [
    { to: '/products', title: 'Productos', desc: 'Catálogo de productos y unidades' },
    { to: '/inventory', title: 'Inventario', desc: 'Stock por bodega y movimientos' },
    { to: '/orders', title: 'Órdenes', desc: 'Ventas, confirmaciones y pagos' },
    { to: '/clients', title: 'Clientes', desc: 'Mayoristas y minoristas' },
    { to: '/suppliers', title: 'Proveedores', desc: 'Distribuidores y mayoristas de origen' },
    { to: '/lists', title: 'Listas de Precios', desc: 'Precios diferenciados por tipo de cliente' },
    { to: '/categories', title: 'Categorías', desc: 'Clasificación de productos' },
    { to: '/brands', title: 'Marcas', desc: 'Marcas de fabricantes' },
    { to: '/characteristics', title: 'Características', desc: 'Atributos dinámicos' },
    { to: '/warehouses', title: 'Bodegas', desc: 'Bodegas de almacenamiento' },
    { to: '/branches', title: 'Sedes', desc: 'Sedes de la organización (agrupan bodegas)' },
  ];
  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Catálogo</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {cards.map(c => (
          <Link key={c.to} to={`/catalog${c.to}`} className="card hover:shadow-lg transition cursor-pointer">
            <h3 className="text-lg font-bold text-primary-600">{c.title}</h3>
            <p className="text-gray-600 text-sm mt-1">{c.desc}</p>
          </Link>
        ))}
      </div>
    </div>
  );
};
export default CatalogHome;
