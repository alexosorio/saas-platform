// Módulo expuesto vía Module Federation. Rutas RELATIVAS (montadas por el host
// bajo /catalog/*). No incluye BrowserRouter ni AppShell: eso lo aporta el host.
import { Routes, Route, Navigate } from 'react-router-dom';
import '../index.css';

import CatalogHome from '../components/CatalogHome';
import ProductList from '../components/products/ProductList';
import ProductForm from '../components/products/ProductForm';
import ProductDetail from '../components/products/ProductDetail';
import ProductBulkImport from '../components/products/ProductBulkImport';
import PriceHistory from '../components/products/PriceHistory';
import UnitForm from '../components/units/UnitForm';
import SupplierList from '../components/suppliers/SupplierList';
import SupplierForm from '../components/suppliers/SupplierForm';
import BranchList from '../components/branches/BranchList';
import BranchForm from '../components/branches/BranchForm';
import CategoryList from '../components/categories/CategoryList';
import CategoryForm from '../components/categories/CategoryForm';
import BrandList from '../components/brands/BrandList';
import BrandForm from '../components/brands/BrandForm';
import CharacteristicList from '../components/characteristics/CharacteristicList';
import CharacteristicForm from '../components/characteristics/CharacteristicForm';
import WarehouseList from '../components/warehouses/WarehouseList';
import WarehouseForm from '../components/warehouses/WarehouseForm';
import InventoryList from '../components/inventory/InventoryList';
import InventoryAdjust from '../components/inventory/InventoryAdjust';
import InventoryMovements from '../components/inventory/InventoryMovements';
import ClientList from '../components/clients/ClientList';
import ClientForm from '../components/clients/ClientForm';
import ListPriceList from '../components/lists/ListPriceList';
import ListPriceForm from '../components/lists/ListPriceForm';
import ListPrices from '../components/lists/ListPrices';
import OrderList from '../components/orders/OrderList';
import OrderForm from '../components/orders/OrderForm';
import OrderDetail from '../components/orders/OrderDetail';
import CuentasPorPagar from '../components/cuentas/CuentasPorPagar';
import DevolucionesList from '../components/devoluciones/DevolucionesList';
import NuevaDevolucion from '../components/devoluciones/NuevaDevolucion';
import DevolucionDetalle from '../components/devoluciones/DevolucionDetalle';
import CuentasPorCobrar from '../components/cuentas/CuentasPorCobrar';

export default function CatalogRoutes() {
  return (
    <Routes>
      <Route index element={<CatalogHome />} />
      <Route path="products" element={<ProductList />} />
      <Route path="products/new" element={<ProductForm />} />
      <Route path="products/bulk" element={<ProductBulkImport />} />
      <Route path="products/:id" element={<ProductDetail />} />
      <Route path="products/:id/edit" element={<ProductForm />} />
      <Route path="products/:id/price-history" element={<PriceHistory />} />
      <Route path="products/:id/units/new" element={<UnitForm />} />

      <Route path="suppliers" element={<SupplierList />} />
      <Route path="suppliers/new" element={<SupplierForm />} />
      <Route path="suppliers/:id/edit" element={<SupplierForm />} />

      <Route path="branches" element={<BranchList />} />
      <Route path="branches/new" element={<BranchForm />} />
      <Route path="branches/:id/edit" element={<BranchForm />} />

      <Route path="categories" element={<CategoryList />} />
      <Route path="categories/new" element={<CategoryForm />} />
      <Route path="categories/:id/edit" element={<CategoryForm />} />

      <Route path="brands" element={<BrandList />} />
      <Route path="brands/new" element={<BrandForm />} />
      <Route path="brands/:id/edit" element={<BrandForm />} />

      <Route path="characteristics" element={<CharacteristicList />} />
      <Route path="characteristics/new" element={<CharacteristicForm />} />
      <Route path="characteristics/:id/edit" element={<CharacteristicForm />} />

      <Route path="warehouses" element={<WarehouseList />} />
      <Route path="warehouses/new" element={<WarehouseForm />} />
      <Route path="warehouses/:id/edit" element={<WarehouseForm />} />

      <Route path="inventory" element={<InventoryList />} />
      <Route path="inventory/adjust" element={<InventoryAdjust />} />
      <Route path="inventory/movements/:inventarioId" element={<InventoryMovements />} />

      <Route path="clients" element={<ClientList />} />
      <Route path="clients/new" element={<ClientForm />} />
      <Route path="clients/:id/edit" element={<ClientForm />} />

      <Route path="lists" element={<ListPriceList />} />
      <Route path="lists/new" element={<ListPriceForm />} />
      <Route path="lists/:id/edit" element={<ListPriceForm />} />
      <Route path="lists/:id/prices" element={<ListPrices />} />

      <Route path="orders" element={<OrderList />} />
      <Route path="orders/new" element={<OrderForm />} />
      <Route path="orders/:id" element={<OrderDetail />} />

      <Route path="devoluciones" element={<DevolucionesList />} />
      <Route path="devoluciones/nueva" element={<NuevaDevolucion />} />
      <Route path="devoluciones/:id" element={<DevolucionDetalle />} />
      <Route path="cuentas-por-pagar" element={<CuentasPorPagar />} />
      <Route path="cuentas-por-cobrar" element={<CuentasPorCobrar />} />

      <Route path="*" element={<Navigate to="/catalog" replace />} />
    </Routes>
  );
}
