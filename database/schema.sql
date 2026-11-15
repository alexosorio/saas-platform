-- ============================================================================
-- SaaS Platform · Modelo de datos consolidado (esquema: gets_shared)
-- Incluye el esquema base + migraciones: precios (fórmula base+margen),
-- pendiente_pago, habeas data, stock reservado y código de barras.
-- Requiere PostgreSQL 12+. La base de datos 'saas_platform' debe existir.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS gets_shared;

-- Hace que gets_shared sea el esquema por defecto para TODA conexión a esta BD
-- (así los 3 servicios encuentran las tablas sin configurar currentSchema).
ALTER DATABASE saas_platform SET search_path TO gets_shared, public;

SET search_path TO gets_shared;

-- ─── TIPOS (ENUMS) ───────────────────────────────────────────────────────
-- =====================================================
-- TIPOS ENUMERADOS - PostgreSQL
-- =====================================================


-- Tipos para suscripciones
CREATE TYPE estado_suscripcion AS ENUM ('activa', 'vencida', 'cancelada', 'periodo_prueba', 'suspendida', 'pendiente_pago');
CREATE TYPE periodo_suscripcion AS ENUM ('mensual', 'anual');

-- Tipos para historial de suscripciones
CREATE TYPE accion_suscripcion AS ENUM ('creacion', 'upgrade', 'downgrade', 'cancelacion', 'renovacion', 'reactivacion');

-- Tipos para métodos de pago
CREATE TYPE tipo_metodo_pago AS ENUM ('tarjeta_credito', 'tarjeta_debito', 'transferencia', 'paypal', 'stripe');

-- Tipos para pagos
CREATE TYPE estado_pago AS ENUM ('pendiente', 'completado', 'fallido', 'reembolsado', 'cancelado');
CREATE TYPE tipo_pago AS ENUM ('suscripcion', 'upgrade', 'adicional');

-- Tipos para productos
CREATE TYPE tipo_dato_caracteristica AS ENUM ('texto', 'numero', 'fecha', 'booleano', 'lista');
CREATE TYPE aplicable_caracteristica AS ENUM ('moviles', 'accesorios', 'todos');

-- Tipos para unidades de productos
CREATE TYPE condicion_producto AS ENUM ('nuevo', 'usado');
CREATE TYPE estado_unidad AS ENUM ('disponible', 'vendido', 'reservado', 'dañado', 'en_reparacion');

-- Tipos para clientes
CREATE TYPE tipo_cliente AS ENUM ('mayorista', 'minorista');

-- Tipos para órdenes
CREATE TYPE estado_orden AS ENUM ('borrador', 'confirmada', 'en_proceso', 'completada', 'cancelada');

-- Tipos para pagos de orden
CREATE TYPE metodo_pago_orden AS ENUM ('efectivo', 'tarjeta', 'transferencia', 'credito');

-- Tipos para movimientos de inventario
CREATE TYPE tipo_movimiento_inventario AS ENUM ('entrada', 'salida', 'ajuste', 'transferencia');

-- Tipos para invitaciones
CREATE TYPE estado_invitacion AS ENUM ('pendiente', 'aceptada', 'rechazada', 'expirada');

-- Tipos para dispositivos
CREATE TYPE tipo_dispositivo AS ENUM ('mobile', 'tablet', 'desktop', 'unknown');

COMMENT ON TYPE estado_suscripcion IS 'Estados posibles de una suscripción';
COMMENT ON TYPE tipo_cliente IS 'Tipo de cliente: mayorista o minorista';

-- ─── USUARIOS Y AUTENTICACIÓN ─────────────────────────────────────────────────
-- =====================================================
-- MÓDULO DE USUARIOS Y AUTENTICACIÓN - PostgreSQL
-- =====================================================


-- Tabla de usuarios
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    email_verificado BOOLEAN DEFAULT FALSE,
    codigo_verificacion VARCHAR(255),
    codigo_verificacion_expira TIMESTAMP,
    token_recuperacion VARCHAR(255),
    token_recuperacion_expira TIMESTAMP,
    intentos_fallidos INTEGER DEFAULT 0,
    bloqueado_hasta TIMESTAMP,
    ultimo_acceso TIMESTAMP,
    ultimo_cambio_password TIMESTAMP,
    organizacion_id BIGINT,
    rol_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);
CREATE INDEX idx_usuarios_organizacion ON usuarios(organizacion_id);

COMMENT ON TABLE usuarios IS 'Usuarios del sistema';
COMMENT ON COLUMN usuarios.password_hash IS 'Hash BCrypt de la contraseña';

-- Tabla de perfiles
CREATE TABLE perfiles (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    nombre VARCHAR(100),
    apellido VARCHAR(100),
    telefono VARCHAR(20),
    avatar_url VARCHAR(500),
    idioma VARCHAR(10) DEFAULT 'es',
    zona_horaria VARCHAR(50) DEFAULT 'America/Bogota',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX idx_perfiles_usuario ON perfiles(usuario_id);

COMMENT ON TABLE perfiles IS 'Información adicional de los usuarios';

-- Tabla de organizaciones
CREATE TABLE organizaciones (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    telefono VARCHAR(20),
    logo_url VARCHAR(500),
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_organizaciones_slug ON organizaciones(slug);
CREATE INDEX idx_organizaciones_activo ON organizaciones(activo);

COMMENT ON TABLE organizaciones IS 'Organizaciones multi-tenant';

-- Tabla de roles
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    nivel INTEGER DEFAULT 1,
    es_sistema BOOLEAN DEFAULT FALSE NOT NULL,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_roles_nombre ON roles(nombre);
CREATE INDEX idx_roles_activo ON roles(activo);
CREATE INDEX idx_roles_nivel ON roles(nivel);

-- FK usuarios.rol_id -> roles(id) (se agrega aquí, ya creada la tabla roles)
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles(id);

COMMENT ON TABLE roles IS 'Roles del sistema';
COMMENT ON COLUMN roles.nivel IS 'Nivel jerárquico del rol (1=bajo, 5=alto)';

-- Tabla de relación usuarios-organizaciones
CREATE TABLE usuarios_organizaciones (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    organizacion_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    es_propietario BOOLEAN DEFAULT FALSE,
    unido_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT,
    UNIQUE (usuario_id, organizacion_id)
);

CREATE INDEX idx_usuarios_org_usuario ON usuarios_organizaciones(usuario_id);
CREATE INDEX idx_usuarios_org_organizacion ON usuarios_organizaciones(organizacion_id);
CREATE INDEX idx_usuarios_org_propietario ON usuarios_organizaciones(es_propietario);

COMMENT ON TABLE usuarios_organizaciones IS 'Relación muchos a muchos entre usuarios y organizaciones';

-- Tabla de invitaciones
CREATE TABLE invitaciones (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    rol_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    estado estado_invitacion DEFAULT 'pendiente' NOT NULL,
    invitado_por BIGINT NOT NULL,
    mensaje TEXT,
    expira_en TIMESTAMP NOT NULL,
    aceptada_en TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT,
    FOREIGN KEY (invitado_por) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX idx_invitaciones_organizacion ON invitaciones(organizacion_id);
CREATE INDEX idx_invitaciones_email ON invitaciones(email);
CREATE INDEX idx_invitaciones_token ON invitaciones(token);
CREATE INDEX idx_invitaciones_estado ON invitaciones(estado);

COMMENT ON TABLE invitaciones IS 'Invitaciones a organizaciones';

-- ─── SUSCRIPCIONES ─────────────────────────────────────────────────
-- =====================================================
-- MÓDULO DE SUSCRIPCIONES Y PLANES - PostgreSQL
-- =====================================================


-- Tabla de planes
CREATE TABLE planes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    precio_mensual DECIMAL(10, 2) NOT NULL,
    precio_anual DECIMAL(10, 2),
    max_usuarios INTEGER DEFAULT 1,
    almacenamiento_gb INTEGER DEFAULT 5,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    orden INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_planes_nombre ON planes(nombre);
CREATE INDEX idx_planes_activo ON planes(activo);
CREATE INDEX idx_planes_orden ON planes(orden);

COMMENT ON TABLE planes IS 'Planes de suscripción disponibles';
COMMENT ON COLUMN planes.max_usuarios IS '-1 indica usuarios ilimitados';

-- Tabla de suscripciones
CREATE TABLE suscripciones (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    estado estado_suscripcion DEFAULT 'periodo_prueba' NOT NULL,
    periodo periodo_suscripcion DEFAULT 'mensual' NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    auto_renovacion BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES planes(id) ON DELETE RESTRICT
);

CREATE INDEX idx_suscripciones_organizacion ON suscripciones(organizacion_id);
CREATE INDEX idx_suscripciones_plan ON suscripciones(plan_id);
CREATE INDEX idx_suscripciones_estado ON suscripciones(estado);
CREATE INDEX idx_suscripciones_fechas ON suscripciones(fecha_inicio, fecha_fin);

COMMENT ON TABLE suscripciones IS 'Suscripciones de las organizaciones';

-- Tabla de historial de suscripciones
CREATE TABLE historial_suscripciones (
    id BIGSERIAL PRIMARY KEY,
    suscripcion_id BIGINT NOT NULL,
    plan_anterior_id BIGINT,
    plan_nuevo_id BIGINT,
    accion accion_suscripcion NOT NULL,
    motivo TEXT,
    usuario_id BIGINT,
    monto_cambio DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (suscripcion_id) REFERENCES suscripciones(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_anterior_id) REFERENCES planes(id) ON DELETE SET NULL,
    FOREIGN KEY (plan_nuevo_id) REFERENCES planes(id) ON DELETE SET NULL,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL
);

CREATE INDEX idx_historial_suscripcion ON historial_suscripciones(suscripcion_id);
CREATE INDEX idx_historial_accion ON historial_suscripciones(accion);
CREATE INDEX idx_historial_fecha ON historial_suscripciones(created_at);

COMMENT ON TABLE historial_suscripciones IS 'Auditoría de cambios en suscripciones';

-- Tabla de métodos de pago
CREATE TABLE metodos_pago (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    tipo tipo_metodo_pago NOT NULL,
    proveedor VARCHAR(50),
    ultimos_digitos VARCHAR(4),
    token VARCHAR(255),
    nombre_titular VARCHAR(200),
    fecha_expiracion DATE,
    predeterminado BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE
);

CREATE INDEX idx_metodos_pago_organizacion ON metodos_pago(organizacion_id);
CREATE INDEX idx_metodos_pago_predeterminado ON metodos_pago(predeterminado);
CREATE INDEX idx_metodos_pago_activo ON metodos_pago(activo);

COMMENT ON TABLE metodos_pago IS 'Métodos de pago de las organizaciones';

-- Tabla de pagos
CREATE TABLE pagos (
    id BIGSERIAL PRIMARY KEY,
    suscripcion_id BIGINT NOT NULL,
    metodo_pago_id BIGINT,
    monto DECIMAL(10, 2) NOT NULL,
    moneda VARCHAR(3) DEFAULT 'USD',
    estado estado_pago DEFAULT 'pendiente' NOT NULL,
    tipo tipo_pago DEFAULT 'suscripcion' NOT NULL,
    descripcion TEXT,
    referencia_externa VARCHAR(255),
    referencia_interna VARCHAR(100) UNIQUE,
    fecha_pago DATE,
    fecha_vencimiento DATE,
    datos_adicionales JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (suscripcion_id) REFERENCES suscripciones(id) ON DELETE RESTRICT,
    FOREIGN KEY (metodo_pago_id) REFERENCES metodos_pago(id) ON DELETE SET NULL
);

CREATE INDEX idx_pagos_suscripcion ON pagos(suscripcion_id);
CREATE INDEX idx_pagos_estado ON pagos(estado);
CREATE INDEX idx_pagos_referencia_externa ON pagos(referencia_externa);
CREATE INDEX idx_pagos_fecha_pago ON pagos(fecha_pago);
CREATE INDEX idx_pagos_datos_adicionales ON pagos USING GIN (datos_adicionales);

COMMENT ON TABLE pagos IS 'Registro de pagos y transacciones';

-- ─── AUTORIZACIÓN (ROLES / PERMISOS) ─────────────────────────────────────────────────
-- =====================================================
-- MÓDULO DE AUTORIZACIÓN Y PERMISOS - PostgreSQL
-- =====================================================


-- Tabla de módulos del sistema
CREATE TABLE modulos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    ruta VARCHAR(255),
    icono VARCHAR(100),
    orden INTEGER DEFAULT 0,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_modulos_codigo ON modulos(codigo);
CREATE INDEX idx_modulos_activo ON modulos(activo);
CREATE INDEX idx_modulos_orden ON modulos(orden);

COMMENT ON TABLE modulos IS 'Módulos funcionales del sistema';

-- Tabla de relación planes-módulos
CREATE TABLE planes_modulos (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    modulo_id BIGINT NOT NULL,
    FOREIGN KEY (plan_id) REFERENCES planes(id) ON DELETE CASCADE,
    FOREIGN KEY (modulo_id) REFERENCES modulos(id) ON DELETE CASCADE,
    UNIQUE (plan_id, modulo_id)
);

CREATE INDEX idx_planes_modulos_plan ON planes_modulos(plan_id);
CREATE INDEX idx_planes_modulos_modulo ON planes_modulos(modulo_id);

COMMENT ON TABLE planes_modulos IS 'Módulos incluidos en cada plan';

-- Tabla de permisos por rol y módulo
CREATE TABLE roles_modulos (
    id BIGSERIAL PRIMARY KEY,
    rol_id BIGINT NOT NULL,
    modulo_id BIGINT NOT NULL,
    crear BOOLEAN DEFAULT FALSE,
    leer BOOLEAN DEFAULT TRUE,
    actualizar BOOLEAN DEFAULT FALSE,
    eliminar BOOLEAN DEFAULT FALSE,
    modulo_codigo VARCHAR(50),
    modulo_nombre VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (modulo_id) REFERENCES modulos(id) ON DELETE CASCADE,
    UNIQUE (rol_id, modulo_id)
);

CREATE INDEX idx_roles_modulos_rol ON roles_modulos(rol_id);
CREATE INDEX idx_roles_modulos_modulo ON roles_modulos(modulo_id);

COMMENT ON TABLE roles_modulos IS 'Permisos CRUD por rol y módulo';

-- ─── INVENTARIO ─────────────────────────────────────────────────
-- =====================================================
-- MÓDULO DE INVENTARIO Y PRODUCTOS - PostgreSQL
-- =====================================================


-- Tabla de ciudades
CREATE TABLE ciudades (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(20) UNIQUE,
    pais VARCHAR(100) DEFAULT 'Colombia',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_ciudades_codigo ON ciudades(codigo);
CREATE INDEX idx_ciudades_nombre ON ciudades(nombre);

COMMENT ON TABLE ciudades IS 'Ciudades donde opera el sistema';

-- Tabla de relación organizaciones-ciudades
CREATE TABLE ciudades_organizaciones (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    ciudad_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (ciudad_id) REFERENCES ciudades(id) ON DELETE CASCADE,
    UNIQUE (organizacion_id, ciudad_id)
);

CREATE INDEX idx_ciudades_org_organizacion ON ciudades_organizaciones(organizacion_id);
CREATE INDEX idx_ciudades_org_ciudad ON ciudades_organizaciones(ciudad_id);

-- Tabla de sedes
CREATE TABLE sedes (
    id BIGSERIAL PRIMARY KEY,
    ciudad_organizacion_id BIGINT NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    direccion TEXT,
    telefono VARCHAR(20),
    email VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (ciudad_organizacion_id) REFERENCES ciudades_organizaciones(id) ON DELETE CASCADE
);

CREATE INDEX idx_sedes_activo ON sedes(activo);

COMMENT ON TABLE sedes IS 'Sedes físicas de las organizaciones';

-- Tabla de bodegas
CREATE TABLE bodegas (
    id BIGSERIAL PRIMARY KEY,
    sede_id BIGINT NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    descripcion TEXT,
    capacidad_m2 DECIMAL(10, 2),
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (sede_id) REFERENCES sedes(id) ON DELETE CASCADE,
    UNIQUE (sede_id, codigo)
);

CREATE INDEX idx_bodegas_codigo ON bodegas(codigo);
CREATE INDEX idx_bodegas_activo ON bodegas(activo);

COMMENT ON TABLE bodegas IS 'Bodegas de almacenamiento por sede';

-- Tabla de categorías de productos
CREATE TABLE categorias_productos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_categorias_codigo ON categorias_productos(codigo);
CREATE INDEX idx_categorias_activo ON categorias_productos(activo);

-- Tabla de marcas
CREATE TABLE marcas (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    logo_url VARCHAR(500),
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_marcas_codigo ON marcas(codigo);
CREATE INDEX idx_marcas_nombre ON marcas(nombre);
CREATE INDEX idx_marcas_activo ON marcas(activo);

-- Tabla de productos
CREATE TABLE productos (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    marca_id BIGINT,
    nombre VARCHAR(255) NOT NULL,
    sku VARCHAR(100),
    descripcion TEXT,
    imagen_url VARCHAR(500),
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (categoria_id) REFERENCES categorias_productos(id) ON DELETE RESTRICT,
    FOREIGN KEY (marca_id) REFERENCES marcas(id) ON DELETE SET NULL,
    UNIQUE (organizacion_id, sku)
);

CREATE INDEX idx_productos_categoria ON productos(categoria_id);
CREATE INDEX idx_productos_marca ON productos(marca_id);
CREATE INDEX idx_productos_activo ON productos(activo);

COMMENT ON TABLE productos IS 'Catálogo de productos';

-- Tabla de características de productos
CREATE TABLE caracteristicas_productos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo_dato tipo_dato_caracteristica DEFAULT 'texto' NOT NULL,
    opciones_lista TEXT,
    obligatorio BOOLEAN DEFAULT FALSE,
    aplicable_a aplicable_caracteristica DEFAULT 'todos' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_caracteristicas_codigo ON caracteristicas_productos(codigo);
CREATE INDEX idx_caracteristicas_aplicable ON caracteristicas_productos(aplicable_a);

COMMENT ON TABLE caracteristicas_productos IS 'Características dinámicas de productos';

-- Tabla de unidades individuales
CREATE TABLE unidades (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    codigo_unico VARCHAR(100) NOT NULL UNIQUE,
    imei1 VARCHAR(20),
    imei2 VARCHAR(20),
    condicion condicion_producto DEFAULT 'nuevo' NOT NULL,
    nivel_bateria INTEGER CHECK (nivel_bateria IS NULL OR (nivel_bateria >= 0 AND nivel_bateria <= 100)),
    precio_compra DECIMAL(12, 2) NOT NULL,
    estado estado_unidad DEFAULT 'disponible' NOT NULL,
    notas TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE RESTRICT
);

CREATE INDEX idx_unidades_codigo_unico ON unidades(codigo_unico);
CREATE INDEX idx_unidades_imei1 ON unidades(imei1);
CREATE INDEX idx_unidades_imei2 ON unidades(imei2);
CREATE INDEX idx_unidades_estado ON unidades(estado);
CREATE INDEX idx_unidades_condicion ON unidades(condicion);

COMMENT ON TABLE unidades IS 'Unidades individuales de productos';

-- Tabla de valores de características (EAV)
CREATE TABLE valores_caracteristicas (
    id BIGSERIAL PRIMARY KEY,
    unidad_id BIGINT NOT NULL,
    caracteristica_id BIGINT NOT NULL,
    valor TEXT,
    FOREIGN KEY (unidad_id) REFERENCES unidades(id) ON DELETE CASCADE,
    FOREIGN KEY (caracteristica_id) REFERENCES caracteristicas_productos(id) ON DELETE CASCADE,
    UNIQUE (unidad_id, caracteristica_id)
);

CREATE INDEX idx_valores_unidad ON valores_caracteristicas(unidad_id);
CREATE INDEX idx_valores_caracteristica ON valores_caracteristicas(caracteristica_id);

-- Tabla de inventario
CREATE TABLE inventario (
    id BIGSERIAL PRIMARY KEY,
    bodega_id BIGINT NOT NULL,
    unidad_id BIGINT NOT NULL,
    cantidad INTEGER DEFAULT 1 CHECK (cantidad >= 0),
    ubicacion VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (bodega_id) REFERENCES bodegas(id) ON DELETE RESTRICT,
    FOREIGN KEY (unidad_id) REFERENCES unidades(id) ON DELETE RESTRICT,
    UNIQUE (bodega_id, unidad_id)
);

CREATE INDEX idx_inventario_bodega ON inventario(bodega_id);
CREATE INDEX idx_inventario_unidad ON inventario(unidad_id);

COMMENT ON TABLE inventario IS 'Stock de productos por bodega';

-- Tabla de movimientos de inventario
CREATE TABLE movimientos_inventario (
    id BIGSERIAL PRIMARY KEY,
    inventario_id BIGINT NOT NULL,
    orden_id BIGINT,
    tipo tipo_movimiento_inventario NOT NULL,
    cantidad INTEGER NOT NULL,
    cantidad_anterior INTEGER NOT NULL,
    cantidad_nueva INTEGER NOT NULL,
    motivo VARCHAR(255),
    usuario_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (inventario_id) REFERENCES inventario(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE INDEX idx_movimientos_inventario ON movimientos_inventario(inventario_id);
CREATE INDEX idx_movimientos_tipo ON movimientos_inventario(tipo);
CREATE INDEX idx_movimientos_fecha ON movimientos_inventario(created_at);

COMMENT ON TABLE movimientos_inventario IS 'Auditoría de movimientos de inventario';

-- ─── VENTAS / CATÁLOGO ─────────────────────────────────────────────────
-- =====================================================
-- MÓDULO DE VENTAS Y LISTAS DE PRECIOS - PostgreSQL
-- =====================================================


-- Tabla de listas de precios
CREATE TABLE listas_precios (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    tipo_cliente tipo_cliente NOT NULL,
    descripcion TEXT,
    activa BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE
);

CREATE INDEX idx_listas_organizacion ON listas_precios(organizacion_id);
CREATE INDEX idx_listas_tipo ON listas_precios(tipo_cliente);
CREATE INDEX idx_listas_activa ON listas_precios(activa);

COMMENT ON TABLE listas_precios IS 'Listas de precios diferenciadas por tipo de cliente';

-- Tabla de precios
CREATE TABLE precios (
    id BIGSERIAL PRIMARY KEY,
    lista_precio_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    precio DECIMAL(12, 2) NOT NULL,
    margen_porcentaje DECIMAL(5, 2),
    vigente_desde TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vigente_hasta TIMESTAMP,
    FOREIGN KEY (lista_precio_id) REFERENCES listas_precios(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

CREATE INDEX idx_precios_lista ON precios(lista_precio_id);
CREATE INDEX idx_precios_producto ON precios(producto_id);
CREATE INDEX idx_precios_vigencia ON precios(vigente_desde, vigente_hasta);

COMMENT ON TABLE precios IS 'Precios por producto y lista';

-- Tabla de enlaces públicos para listas de precios
CREATE TABLE enlaces_publicos_listas (
    id BIGSERIAL PRIMARY KEY,
    lista_precio_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    nombre_enlace VARCHAR(200),
    descripcion TEXT,
    
    -- Control de acceso
    requiere_password BOOLEAN DEFAULT FALSE,
    password_hash VARCHAR(255),
    
    -- Vigencia
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_expiracion TIMESTAMP,
    
    -- Límites de uso
    max_vistas INTEGER,
    vistas_actuales INTEGER DEFAULT 0,
    
    -- Configuración de visualización
    mostrar_precios BOOLEAN DEFAULT TRUE,
    mostrar_stock BOOLEAN DEFAULT FALSE,
    permitir_busqueda BOOLEAN DEFAULT TRUE,
    
    -- Filtros opcionales
    categorias_visibles JSONB,
    marcas_visibles JSONB,
    
    -- Metadata
    usuario_creador BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (lista_precio_id) REFERENCES listas_precios(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_creador) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE INDEX idx_enlaces_token ON enlaces_publicos_listas(token);
CREATE INDEX idx_enlaces_lista ON enlaces_publicos_listas(lista_precio_id);
CREATE INDEX idx_enlaces_activo ON enlaces_publicos_listas(activo);
CREATE INDEX idx_enlaces_expiracion ON enlaces_publicos_listas(fecha_expiracion);

COMMENT ON TABLE enlaces_publicos_listas IS 'URLs públicas compartibles de listas de precios';

-- Tabla de accesos a enlaces públicos
CREATE TABLE accesos_enlace_publico (
    id BIGSERIAL PRIMARY KEY,
    enlace_id BIGINT NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    referrer VARCHAR(500),
    pais VARCHAR(100),
    ciudad VARCHAR(100),
    dispositivo tipo_dispositivo DEFAULT 'unknown',
    
    -- Datos de sesión
    session_id VARCHAR(100),
    duracion_segundos INTEGER,
    productos_vistos INTEGER DEFAULT 0,
    
    fecha_acceso TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (enlace_id) REFERENCES enlaces_publicos_listas(id) ON DELETE CASCADE
);

CREATE INDEX idx_accesos_enlace ON accesos_enlace_publico(enlace_id);
CREATE INDEX idx_accesos_fecha ON accesos_enlace_publico(fecha_acceso);
CREATE INDEX idx_accesos_session ON accesos_enlace_publico(session_id);

COMMENT ON TABLE accesos_enlace_publico IS 'Analytics de accesos a enlaces públicos';

-- Tabla de productos vistos en enlaces
CREATE TABLE productos_vistos_enlace (
    id BIGSERIAL PRIMARY KEY,
    acceso_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    tiempo_visualizacion_segundos INTEGER DEFAULT 0,
    fecha_vista TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (acceso_id) REFERENCES accesos_enlace_publico(id) ON DELETE CASCADE,
    FOREIGN KEY (producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

CREATE INDEX idx_productos_vistos_acceso ON productos_vistos_enlace(acceso_id);
CREATE INDEX idx_productos_vistos_producto ON productos_vistos_enlace(producto_id);

-- Tabla de clientes
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    tipo tipo_cliente DEFAULT 'minorista' NOT NULL,
    nombre VARCHAR(255) NOT NULL,
    documento VARCHAR(50),
    email VARCHAR(255),
    telefono VARCHAR(20),
    direccion TEXT,
    ciudad_id BIGINT,
    limite_credito DECIMAL(12, 2) DEFAULT 0,
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE CASCADE,
    FOREIGN KEY (ciudad_id) REFERENCES ciudades(id) ON DELETE SET NULL
);

CREATE INDEX idx_clientes_organizacion ON clientes(organizacion_id);
CREATE INDEX idx_clientes_tipo ON clientes(tipo);
CREATE INDEX idx_clientes_documento ON clientes(documento);
CREATE INDEX idx_clientes_activo ON clientes(activo);

COMMENT ON TABLE clientes IS 'Clientes de la organización';

-- Tabla de órdenes de venta
CREATE TABLE ordenes (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    sede_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    numero_orden VARCHAR(50) NOT NULL UNIQUE,
    fecha_orden DATE NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    descuento DECIMAL(12, 2) DEFAULT 0,
    impuestos DECIMAL(12, 2) DEFAULT 0,
    total DECIMAL(12, 2) NOT NULL,
    saldo_pendiente DECIMAL(12, 2) NOT NULL,
    estado estado_orden DEFAULT 'borrador' NOT NULL,
    notas TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (organizacion_id) REFERENCES organizaciones(id) ON DELETE RESTRICT,
    FOREIGN KEY (sede_id) REFERENCES sedes(id) ON DELETE RESTRICT,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE INDEX idx_ordenes_organizacion ON ordenes(organizacion_id);
CREATE INDEX idx_ordenes_sede ON ordenes(sede_id);
CREATE INDEX idx_ordenes_cliente ON ordenes(cliente_id);
CREATE INDEX idx_ordenes_numero ON ordenes(numero_orden);
CREATE INDEX idx_ordenes_estado ON ordenes(estado);
CREATE INDEX idx_ordenes_fecha ON ordenes(fecha_orden);

COMMENT ON TABLE ordenes IS 'Órdenes de venta';

-- Tabla de detalles de orden
CREATE TABLE detalles_orden (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    unidad_id BIGINT NOT NULL,
    cantidad INTEGER DEFAULT 1,
    precio_unitario DECIMAL(12, 2) NOT NULL,
    descuento_linea DECIMAL(12, 2) DEFAULT 0,
    subtotal DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (orden_id) REFERENCES ordenes(id) ON DELETE CASCADE,
    FOREIGN KEY (unidad_id) REFERENCES unidades(id) ON DELETE RESTRICT
);

CREATE INDEX idx_detalles_orden ON detalles_orden(orden_id);
CREATE INDEX idx_detalles_unidad ON detalles_orden(unidad_id);

-- Tabla de pagos de orden
CREATE TABLE pagos_orden (
    id BIGSERIAL PRIMARY KEY,
    orden_id BIGINT NOT NULL,
    monto DECIMAL(12, 2) NOT NULL,
    metodo metodo_pago_orden NOT NULL,
    referencia VARCHAR(100),
    observaciones TEXT,
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    usuario_registro BIGINT NOT NULL,
    FOREIGN KEY (orden_id) REFERENCES ordenes(id) ON DELETE RESTRICT,
    FOREIGN KEY (usuario_registro) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE INDEX idx_pagos_orden_orden ON pagos_orden(orden_id);
CREATE INDEX idx_pagos_orden_metodo ON pagos_orden(metodo);
CREATE INDEX idx_pagos_orden_fecha ON pagos_orden(fecha_pago);

COMMENT ON TABLE pagos_orden IS 'Pagos recibidos por orden (parciales o totales)';

-- Agregar foreign key de movimientos_inventario a ordenes (faltó en el archivo anterior)
ALTER TABLE movimientos_inventario ADD CONSTRAINT fk_movimientos_orden 
    FOREIGN KEY (orden_id) REFERENCES ordenes(id) ON DELETE SET NULL;

-- ─── TRIGGERS Y FUNCIONES ─────────────────────────────────────────────────
-- =====================================================
-- TRIGGERS Y FUNCIONES - PostgreSQL
-- =====================================================


-- =====================================================
-- FUNCIÓN: Actualizar timestamp updated_at
-- =====================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar trigger a todas las tablas con updated_at
CREATE TRIGGER update_usuarios_updated_at BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_perfiles_updated_at BEFORE UPDATE ON perfiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_organizaciones_updated_at BEFORE UPDATE ON organizaciones
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_suscripciones_updated_at BEFORE UPDATE ON suscripciones
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_metodos_pago_updated_at BEFORE UPDATE ON metodos_pago
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pagos_updated_at BEFORE UPDATE ON pagos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sedes_updated_at BEFORE UPDATE ON sedes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bodegas_updated_at BEFORE UPDATE ON bodegas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_productos_updated_at BEFORE UPDATE ON productos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_unidades_updated_at BEFORE UPDATE ON unidades
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_listas_precios_updated_at BEFORE UPDATE ON listas_precios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_clientes_updated_at BEFORE UPDATE ON clientes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ordenes_updated_at BEFORE UPDATE ON ordenes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_enlaces_updated_at BEFORE UPDATE ON enlaces_publicos_listas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =====================================================
-- FUNCIÓN: Actualizar saldo pendiente después de pago
-- =====================================================
CREATE OR REPLACE FUNCTION actualizar_saldo_orden()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE ordenes 
    SET saldo_pendiente = total - (
        SELECT COALESCE(SUM(monto), 0) 
        FROM pagos_orden 
        WHERE orden_id = NEW.orden_id
    )
    WHERE id = NEW.orden_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_pago_orden_insert
    AFTER INSERT ON pagos_orden
    FOR EACH ROW EXECUTE FUNCTION actualizar_saldo_orden();

-- =====================================================
-- FUNCIÓN: Afectar inventario al confirmar orden
-- =====================================================
CREATE OR REPLACE FUNCTION afectar_inventario_orden()
RETURNS TRIGGER AS $$
BEGIN
    -- Solo si cambia de borrador a confirmada
    IF NEW.estado = 'confirmada' AND OLD.estado = 'borrador' THEN
        
        -- Reducir inventario por cada detalle
        UPDATE inventario i
        SET cantidad = cantidad - d.cantidad
        FROM detalles_orden d
        WHERE i.unidad_id = d.unidad_id
          AND d.orden_id = NEW.id;
        
        -- Registrar movimientos de inventario
        INSERT INTO movimientos_inventario (
            inventario_id, orden_id, tipo, cantidad, 
            cantidad_anterior, cantidad_nueva, motivo, usuario_id
        )
        SELECT 
            i.id,
            NEW.id,
            'salida',
            d.cantidad,
            i.cantidad + d.cantidad,
            i.cantidad,
            'Venta orden ' || NEW.numero_orden,
            NEW.usuario_id
        FROM inventario i
        INNER JOIN detalles_orden d ON i.unidad_id = d.unidad_id
        WHERE d.orden_id = NEW.id;
        
        -- Marcar unidades como vendidas
        UPDATE unidades u
        SET estado = 'vendido'
        FROM detalles_orden d
        WHERE u.id = d.unidad_id
          AND d.orden_id = NEW.id;
    
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_orden_confirmada
    AFTER UPDATE ON ordenes
    FOR EACH ROW EXECUTE FUNCTION afectar_inventario_orden();

-- =====================================================
-- FUNCIÓN: Incrementar contador de vistas en enlaces
-- =====================================================
CREATE OR REPLACE FUNCTION incrementar_vistas_enlace()
RETURNS TRIGGER AS $$
BEGIN
    -- Incrementar contador
    UPDATE enlaces_publicos_listas
    SET vistas_actuales = vistas_actuales + 1
    WHERE id = NEW.enlace_id;
    
    -- Desactivar si alcanzó el límite
    UPDATE enlaces_publicos_listas
    SET activo = FALSE
    WHERE id = NEW.enlace_id
      AND max_vistas IS NOT NULL
      AND vistas_actuales >= max_vistas;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_acceso_enlace_insert
    AFTER INSERT ON accesos_enlace_publico
    FOR EACH ROW EXECUTE FUNCTION incrementar_vistas_enlace();

-- =====================================================
-- FUNCIÓN: Actualizar timestamp de inventario
-- =====================================================
CREATE OR REPLACE FUNCTION update_inventario_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_inventario_updated_at
    BEFORE UPDATE ON inventario
    FOR EACH ROW EXECUTE FUNCTION update_inventario_timestamp();

COMMENT ON FUNCTION update_updated_at_column() IS 'Actualiza automáticamente el campo updated_at';
COMMENT ON FUNCTION actualizar_saldo_orden() IS 'Calcula el saldo pendiente después de registrar un pago';
COMMENT ON FUNCTION afectar_inventario_orden() IS 'Reduce inventario y registra movimientos al confirmar orden';
COMMENT ON FUNCTION incrementar_vistas_enlace() IS 'Incrementa contador de vistas y desactiva enlace si alcanza límite';

-- ─── MIGRACIONES APLICADAS (foldeadas) ──────────────────────────────────
-- Modelo de precios (proveedores, historial, columnas de fórmula)
-- ═══════════════════════════════════════════════════════════════════════
--  MIGRACION V2 — Modelo de precios centrado en el producto
--  + listas de precios como FORMULAS + auditoria de cambios + proveedores
--  Schema: gets_shared   |   PostgreSQL
-- ═══════════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────────
-- 1. PROVEEDORES (tabla nueva)
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS proveedores (
    id              BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    nombre          VARCHAR(200) NOT NULL,
    identificacion  VARCHAR(50),            -- NIT / RUT
    telefono        VARCHAR(50),
    email           VARCHAR(150),
    direccion       VARCHAR(300),
    contacto        VARCHAR(150),           -- persona de contacto
    notas           TEXT,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_proveedores_org ON proveedores(organizacion_id);

-- ─────────────────────────────────────────────────────────────────────
-- 2. PRODUCTOS: precio_venta, precio_compra, proveedor_id, tipo, requiere_imei
-- ─────────────────────────────────────────────────────────────────────
ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS precio_venta   NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS precio_compra  NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS proveedor_id   BIGINT,
    ADD COLUMN IF NOT EXISTS tipo_producto  VARCHAR(50) DEFAULT 'accesorio',
    ADD COLUMN IF NOT EXISTS requiere_imei  BOOLEAN NOT NULL DEFAULT FALSE;

-- requiere_imei se deriva de tipo_producto: 'movil' => TRUE.
-- Lo guardamos explicito para que el formulario de unidades lo lea directo.

DO $$ BEGIN
    ALTER TABLE productos
        ADD CONSTRAINT fk_productos_proveedor
        FOREIGN KEY (proveedor_id) REFERENCES proveedores(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- ─────────────────────────────────────────────────────────────────────
-- 3. AUDITORIA DE CAMBIOS DE PRECIO (tabla nueva)
--    Una fila por cada cambio de precio_venta y/o precio_compra.
-- ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS historial_precios_producto (
    id                     BIGSERIAL PRIMARY KEY,
    producto_id            BIGINT NOT NULL,
    precio_venta_anterior  NUMERIC(15,2),
    precio_venta_nuevo     NUMERIC(15,2),
    precio_compra_anterior NUMERIC(15,2),
    precio_compra_nuevo    NUMERIC(15,2),
    usuario_id             BIGINT,          -- quien hizo el cambio (del JWT)
    motivo                 VARCHAR(300),
    fecha_cambio           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_histprecio_producto FOREIGN KEY (producto_id)
        REFERENCES productos(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_histprecio_producto ON historial_precios_producto(producto_id);
CREATE INDEX IF NOT EXISTS idx_histprecio_fecha    ON historial_precios_producto(fecha_cambio);

-- ─────────────────────────────────────────────────────────────────────
-- 4. LISTAS_PRECIOS: ahora son FORMULAS
--    base_calculo: sobre que precio del producto se calcula
--    tipo_ajuste:  margen porcentual o valor fijo
--    valor_ajuste: el numero (porcentaje o valor)
-- ─────────────────────────────────────────────────────────────────────
DO $$ BEGIN
    CREATE TYPE base_calculo_lista AS ENUM ('precio_venta', 'precio_compra');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE tipo_ajuste_lista AS ENUM ('margen_porcentaje', 'valor_fijo');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE listas_precios
    ADD COLUMN IF NOT EXISTS base_calculo base_calculo_lista NOT NULL DEFAULT 'precio_venta',
    ADD COLUMN IF NOT EXISTS tipo_ajuste  tipo_ajuste_lista  NOT NULL DEFAULT 'margen_porcentaje',
    ADD COLUMN IF NOT EXISTS valor_ajuste NUMERIC(15,4)      NOT NULL DEFAULT 0;

-- ─────────────────────────────────────────────────────────────────────
-- 5. PRECIOS (tabla existente) -> se reutiliza como OVERRIDE opcional.
--    Semantica nueva: si hay fila en `precios` para (lista, producto),
--    ese valor MANDA sobre la formula. Si no hay, se usa la formula.
--    No requiere cambios estructurales.
--    Si quieres modelo 100% formula, puedes vaciarla:  TRUNCATE precios;
-- ─────────────────────────────────────────────────────────────────────

-- ═══════════════════════════════════════════════════════════════════════
--  REGLA DE CALCULO DEL PRECIO DE UN PRODUCTO EN UNA LISTA
--  ───────────────────────────────────────────────────────────────────
--  precio(lista, producto) =
--     SI existe override en `precios`  -> ese valor
--     SINO:
--       base = (lista.base_calculo = 'precio_venta')
--                 ? producto.precio_venta
--                 : producto.precio_compra
--       SI lista.tipo_ajuste = 'margen_porcentaje'
--                 -> base * (1 + lista.valor_ajuste/100)
--       SI lista.tipo_ajuste = 'valor_fijo'
--                 -> lista.valor_ajuste
-- ═══════════════════════════════════════════════════════════════════════

-- Habeas data en métodos de pago
-- ============================================================================
-- V3: Gating de pago (suscripción pendiente hasta pagar) + Habeas Data en pagos
-- Esquema de subscription-service (tablas en 'public').
-- ============================================================================

-- 1) Nuevo estado para suscripciones creadas sin pago confirmado.

-- 2) Registro de autorización de tratamiento de datos (Ley 1581/2012 - Habeas Data)
ALTER TABLE metodos_pago
    ADD COLUMN IF NOT EXISTS autorizacion_datos boolean,
    ADD COLUMN IF NOT EXISTS fecha_autorizacion timestamp,
    ADD COLUMN IF NOT EXISTS version_politica varchar(20);

-- Stock reservado (bloqueo por pago en proceso)
-- V4: stock reservado (bloqueo por pago en proceso). Esquema del catálogo.
ALTER TABLE inventario ADD COLUMN IF NOT EXISTS cantidad_reservada integer NOT NULL DEFAULT 0;

-- Código de barras del fabricante (EAN/UPC)
-- V5: código de barras del fabricante (EAN/UPC/GTIN) en productos.
ALTER TABLE productos ADD COLUMN IF NOT EXISTS codigo_barras varchar(64);
CREATE INDEX IF NOT EXISTS idx_productos_codigo_barras ON productos (organizacion_id, codigo_barras);

-- V8: pagos manuales


-- ─── V8: campos de pago manual (transferencia/tarjeta) + soporte ─────────
ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS metodo            varchar(50),
    ADD COLUMN IF NOT EXISTS banco             varchar(100),
    ADD COLUMN IF NOT EXISTS comprobante_url   varchar(500),
    ADD COLUMN IF NOT EXISTS ultimos_digitos   varchar(4),
    ADD COLUMN IF NOT EXISTS fecha_transaccion date;
ALTER TABLE pagos_orden
    ADD COLUMN IF NOT EXISTS banco             varchar(100),
    ADD COLUMN IF NOT EXISTS fecha_transaccion date,
    ADD COLUMN IF NOT EXISTS comprobante_url   varchar(500),
    ADD COLUMN IF NOT EXISTS ultimos_digitos   varchar(4);



-- ─── V9: proveedor pasa de producto a unidad ────────────────────────────
-- V9: proveedor en unidad
ALTER TABLE unidades ADD COLUMN IF NOT EXISTS proveedor_id bigint;
DO $$ BEGIN
    ALTER TABLE unidades ADD CONSTRAINT fk_unidades_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id);
EXCEPTION WHEN duplicate_object THEN NULL; WHEN undefined_table THEN NULL; END $$;
CREATE INDEX IF NOT EXISTS idx_unidades_proveedor ON unidades(proveedor_id);
UPDATE unidades u SET proveedor_id = p.proveedor_id FROM productos p
  WHERE u.producto_id = p.id AND p.proveedor_id IS NOT NULL AND u.proveedor_id IS NULL;
ALTER TABLE productos DROP COLUMN IF EXISTS proveedor_id;


-- ─── V10: cuentas por pagar / por cobrar ────────────────────────────────


-- Cuentas por pagar (una por unidad ingresada, por su precio de compra)
CREATE TABLE IF NOT EXISTS cuentas_por_pagar (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    unidad_id BIGINT REFERENCES unidades(id),
    proveedor_id BIGINT REFERENCES proveedores(id),
    monto NUMERIC(14,2) NOT NULL,
    saldo NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    descripcion TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cxp_org ON cuentas_por_pagar(organizacion_id);
CREATE INDEX IF NOT EXISTS idx_cxp_estado ON cuentas_por_pagar(organizacion_id, estado);

-- Pagos a proveedor (con soporte de la transferencia)
CREATE TABLE IF NOT EXISTS pagos_proveedor (
    id BIGSERIAL PRIMARY KEY,
    cuenta_por_pagar_id BIGINT NOT NULL REFERENCES cuentas_por_pagar(id) ON DELETE CASCADE,
    monto NUMERIC(14,2) NOT NULL,
    metodo VARCHAR(50),
    banco VARCHAR(100),
    comprobante_url VARCHAR(500),
    ultimos_digitos VARCHAR(4),
    fecha_transaccion DATE,
    referencia VARCHAR(255),
    observaciones TEXT,
    usuario_registro BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_pagoprov_cuenta ON pagos_proveedor(cuenta_por_pagar_id);

-- Cuentas por cobrar (una por orden con saldo pendiente)
CREATE TABLE IF NOT EXISTS cuentas_por_cobrar (
    id BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    orden_id BIGINT REFERENCES ordenes(id),
    cliente_id BIGINT REFERENCES clientes(id),
    monto NUMERIC(14,2) NOT NULL,
    saldo NUMERIC(14,2) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cxc_org ON cuentas_por_cobrar(organizacion_id);
CREATE INDEX IF NOT EXISTS idx_cxc_estado ON cuentas_por_cobrar(organizacion_id, estado);


-- ─── DATOS INICIALES ────────────────────────────────────────────────────
-- =====================================================
-- DATOS INICIALES DEL SISTEMA - PostgreSQL
-- =====================================================


BEGIN;

-- =====================================================
-- MÓDULOS DEL SISTEMA
-- =====================================================
INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo) VALUES
('Dashboard', 'dashboard', 'Panel principal del sistema', '/dashboard', 'home', 1, TRUE),
('Usuarios', 'usuarios', 'Gestión de usuarios y equipos', '/usuarios', 'users', 2, TRUE),
('Reportes Básicos', 'reportes_basicos', 'Reportes estándar del sistema', '/reportes/basicos', 'chart-line', 3, TRUE),
('Reportes Avanzados', 'reportes_avanzados', 'Analytics y reportes avanzados', '/reportes/avanzados', 'chart-bar', 4, TRUE),
('API Access', 'api_access', 'Acceso a la API REST', '/api', 'code', 5, TRUE),
('Integraciones', 'integraciones', 'Integraciones con servicios externos', '/integraciones', 'plug', 6, TRUE),
('Facturación', 'facturacion', 'Gestión de facturación y pagos', '/facturacion', 'credit-card', 7, TRUE),
('Inventario', 'inventario', 'Gestión de inventario de productos', '/inventario', 'package', 8, TRUE),
('Ventas', 'ventas', 'Gestión de órdenes y ventas', '/ventas', 'shopping-cart', 9, TRUE),
('Configuración', 'configuracion', 'Configuración del sistema', '/configuracion', 'settings', 10, TRUE);

-- =====================================================
-- PLANES DE SUSCRIPCIÓN
-- =====================================================
INSERT INTO planes (nombre, descripcion, precio_mensual, precio_anual, max_usuarios, almacenamiento_gb, orden) VALUES
('Free', 'Plan gratuito para probar el servicio', 0.00, 0.00, 1, 2, 1),
('Starter', 'Ideal para equipos pequeños', 29.99, 299.99, 5, 10, 2),
('Professional', 'Para equipos en crecimiento', 99.99, 999.99, 20, 50, 3),
('Enterprise', 'Solución empresarial completa', 299.99, 2999.99, -1, 500, 4);

-- =====================================================
-- ASIGNAR MÓDULOS A PLANES
-- =====================================================

-- Plan Free: solo dashboard y reportes básicos
INSERT INTO planes_modulos (plan_id, modulo_id)
SELECT p.id, m.id FROM planes p CROSS JOIN modulos m
WHERE p.nombre = 'Free' AND m.codigo IN ('dashboard', 'reportes_basicos');

-- Plan Starter
INSERT INTO planes_modulos (plan_id, modulo_id)
SELECT p.id, m.id FROM planes p CROSS JOIN modulos m
WHERE p.nombre = 'Starter' AND m.codigo IN ('dashboard', 'usuarios', 'reportes_basicos', 'facturacion', 'configuracion');

-- Plan Professional
INSERT INTO planes_modulos (plan_id, modulo_id)
SELECT p.id, m.id FROM planes p CROSS JOIN modulos m
WHERE p.nombre = 'Professional' AND m.codigo IN ('dashboard', 'usuarios', 'reportes_basicos', 'reportes_avanzados', 'api_access', 'inventario', 'ventas', 'facturacion', 'configuracion');

-- Plan Enterprise: todos los módulos
INSERT INTO planes_modulos (plan_id, modulo_id)
SELECT p.id, m.id FROM planes p CROSS JOIN modulos m
WHERE p.nombre = 'Enterprise';

-- =====================================================
-- ROLES
-- =====================================================
INSERT INTO roles (nombre, descripcion, nivel, activo) VALUES
('Propietario', 'Propietario de la organización con acceso total', 5, TRUE),
('Administrador', 'Administrador con acceso completo', 4, TRUE),
('Editor', 'Puede crear, leer y actualizar', 3, TRUE),
('Miembro', 'Miembro regular del equipo', 2, TRUE),
('Visualizador', 'Solo puede ver información', 1, TRUE);

-- =====================================================
-- ASIGNAR PERMISOS A ROLES
-- =====================================================

-- Propietario y Administrador: permisos completos
INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, m.codigo, m.nombre, TRUE, TRUE, TRUE, TRUE FROM roles r CROSS JOIN modulos m
WHERE r.nombre IN ('Propietario', 'Administrador');

-- Editor: crear, leer, actualizar
INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, m.codigo, m.nombre, TRUE, TRUE, TRUE, FALSE FROM roles r CROSS JOIN modulos m
WHERE r.nombre = 'Editor';

-- Miembro: crear y leer en algunos módulos
INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, m.codigo, m.nombre, 
    CASE WHEN m.codigo IN ('dashboard', 'reportes_basicos', 'ventas') THEN TRUE ELSE FALSE END,
    TRUE,
    CASE WHEN m.codigo IN ('dashboard') THEN TRUE ELSE FALSE END,
    FALSE 
FROM roles r CROSS JOIN modulos m
WHERE r.nombre = 'Miembro';

-- Visualizador: solo leer
INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, m.codigo, m.nombre, FALSE, TRUE, FALSE, FALSE FROM roles r CROSS JOIN modulos m
WHERE r.nombre = 'Visualizador';

-- =====================================================
-- CIUDADES DE COLOMBIA
-- =====================================================
INSERT INTO ciudades (nombre, codigo, pais) VALUES
('Medellín', 'MDE', 'Colombia'),
('Bogotá', 'BOG', 'Colombia'),
('Cali', 'CLO', 'Colombia'),
('Barranquilla', 'BAQ', 'Colombia'),
('Cartagena', 'CTG', 'Colombia'),
('Bucaramanga', 'BGA', 'Colombia'),
('Pereira', 'PEI', 'Colombia'),
('Manizales', 'MZL', 'Colombia'),
('Santa Marta', 'SMR', 'Colombia'),
('Cúcuta', 'CUC', 'Colombia');

-- =====================================================
-- CATEGORÍAS DE PRODUCTOS
-- =====================================================
INSERT INTO categorias_productos (nombre, codigo, descripcion) VALUES
('Móviles', 'MOVILES', 'Teléfonos móviles y smartphones'),
('Accesorios', 'ACCESORIOS', 'Accesorios para móviles'),
('Repuestos', 'REPUESTOS', 'Repuestos y partes');

-- =====================================================
-- MARCAS
-- =====================================================
INSERT INTO marcas (nombre, codigo) VALUES
('Apple', 'APPLE'),
('Samsung', 'SAMSUNG'),
('Xiaomi', 'XIAOMI'),
('Huawei', 'HUAWEI'),
('Motorola', 'MOTOROLA'),
('Realme', 'REALME'),
('Oppo', 'OPPO'),
('OnePlus', 'ONEPLUS'),
('Vivo', 'VIVO'),
('Genérico', 'GENERICO');

-- =====================================================
-- CARACTERÍSTICAS DINÁMICAS DE PRODUCTOS
-- =====================================================
INSERT INTO caracteristicas_productos (nombre, codigo, tipo_dato, obligatorio, aplicable_a) VALUES
('Color', 'color', 'texto', TRUE, 'moviles'),
('Memoria RAM (GB)', 'ram', 'numero', FALSE, 'moviles'),
('Almacenamiento (GB)', 'storage', 'numero', FALSE, 'moviles'),
('Tamaño Pantalla (pulgadas)', 'screen_size', 'numero', FALSE, 'moviles'),
('Sistema Operativo', 'os', 'texto', FALSE, 'moviles'),
('Procesador', 'processor', 'texto', FALSE, 'moviles'),
('Cámara Principal (MP)', 'main_camera', 'numero', FALSE, 'moviles'),
('Cámara Frontal (MP)', 'front_camera', 'numero', FALSE, 'moviles'),
('Batería (mAh)', 'battery', 'numero', FALSE, 'moviles'),
('Material', 'material', 'texto', FALSE, 'accesorios'),
('Compatibilidad', 'compatibilidad', 'texto', FALSE, 'accesorios');

COMMIT;

SELECT 'Datos iniciales insertados correctamente' as mensaje;

-- ─── Permiso comodín para el rol Administrador (autorización data-driven) ─
INSERT INTO modulos (nombre, codigo, descripcion, orden, activo)
VALUES ('Todos los módulos', '*', 'Comodín: acceso total a todos los módulos', 0, TRUE)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, '*', 'Todos los módulos', TRUE, TRUE, TRUE, TRUE
FROM roles r JOIN modulos m ON m.codigo = '*'
WHERE r.nombre = 'Administrador'
ON CONFLICT (rol_id, modulo_id) DO NOTHING;

-- ─── V11: módulo 'roles' ─────────────────────────────────────────────────
INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Roles y permisos', 'roles', 'Administración de roles y sus permisos por módulo', '/roles', 'user-shield', 3, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ─── V12: vencimiento de cuentas ─────────────────────────────────────────
ALTER TABLE cuentas_por_pagar  ADD COLUMN IF NOT EXISTS fecha_vencimiento date;
ALTER TABLE cuentas_por_cobrar ADD COLUMN IF NOT EXISTS fecha_vencimiento date;
CREATE INDEX IF NOT EXISTS idx_cxp_vence ON cuentas_por_pagar(organizacion_id, fecha_vencimiento);
CREATE INDEX IF NOT EXISTS idx_cxc_vence ON cuentas_por_cobrar(organizacion_id, fecha_vencimiento);

-- ─── V13: precios en unidad ──────────────────────────────────────────────
ALTER TABLE unidades ADD COLUMN IF NOT EXISTS precio_venta NUMERIC(12,2);
COMMENT ON COLUMN productos.precio_venta  IS 'OBSOLETO desde V13: el precio de venta vive en unidades.precio_venta';
COMMENT ON COLUMN productos.precio_compra IS 'OBSOLETO desde V13: el costo vive en unidades.precio_compra';

-- ─── V15: administración de suscripciones ───────────────────────────────


ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS motivo_rechazo   text,
    ADD COLUMN IF NOT EXISTS validado_por     bigint,
    ADD COLUMN IF NOT EXISTS fecha_validacion timestamp;

ALTER TYPE accion_suscripcion ADD VALUE IF NOT EXISTS 'suspension';

-- Módulo de plataforma: el comodín '*' de los administradores de cada organización NO lo concede
INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Administración de suscripciones', 'plataforma_suscripciones',
        'Suscripciones, validación de pagos y facturación de TODAS las organizaciones (solo plataforma)',
        '/subscription/admin', 'crown', 90, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- Rol de la plataforma: acceso total ('*') + administración de suscripciones
INSERT INTO roles (nombre, descripcion, nivel, es_sistema, activo)
VALUES ('Superadministrador', 'Administrador de la plataforma (todas las organizaciones)', 10, TRUE, TRUE)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, m.codigo, m.nombre, TRUE, TRUE, TRUE, TRUE
FROM roles r JOIN modulos m ON m.codigo IN ('plataforma_suscripciones', '*')
WHERE r.nombre = 'Superadministrador'
ON CONFLICT (rol_id, modulo_id) DO NOTHING;

-- Asigna el rol a la persona que administra la plataforma (ajusta el correo).
-- Conserva todos sus permisos actuales, porque el rol incluye el comodín '*'.
-- UPDATE usuarios SET rol_id = (SELECT id FROM roles WHERE nombre = 'Superadministrador')
--  WHERE email = 'tu-correo@dominio.com';

-- ─── V16: roles por organización ─────────────────────────────────────────


ALTER TABLE roles ADD COLUMN IF NOT EXISTS organizacion_id bigint;

-- 1) Los roles base son del sistema
UPDATE roles SET es_sistema = TRUE
 WHERE organizacion_id IS NULL
   AND nombre IN ('Propietario', 'Administrador', 'Editor', 'Miembro', 'Invitado', 'Superadministrador');

-- 2) Roles personalizados existentes: se asignan a la organización de sus usuarios cuando no hay ambigüedad
UPDATE roles r
   SET organizacion_id = x.org
  FROM (SELECT rol_id, MIN(organizacion_id) AS org
          FROM usuarios
         WHERE rol_id IS NOT NULL AND organizacion_id IS NOT NULL
         GROUP BY rol_id
        HAVING COUNT(DISTINCT organizacion_id) = 1) x
 WHERE r.id = x.rol_id
   AND r.organizacion_id IS NULL
   AND COALESCE(r.es_sistema, FALSE) = FALSE;

-- 3) Nombre único dentro de cada organización (antes: único en toda la plataforma)
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_nombre_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_org_nombre ON roles (COALESCE(organizacion_id, 0), nombre);
CREATE INDEX IF NOT EXISTS idx_roles_organizacion ON roles (organizacion_id);

-- ─── V17: parámetros configurables ──────────────────────────────────────


CREATE TABLE IF NOT EXISTS parametros (
    id              BIGSERIAL PRIMARY KEY,
    clave           VARCHAR(80)  NOT NULL,
    organizacion_id BIGINT,
    valor           VARCHAR(500) NOT NULL,
    tipo            VARCHAR(20)  NOT NULL DEFAULT 'entero',
    alcance         VARCHAR(20)  NOT NULL DEFAULT 'organizacion',
    categoria       VARCHAR(50),
    nombre          VARCHAR(120),
    descripcion     TEXT,
    minimo          NUMERIC,
    maximo          NUMERIC,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_parametros_clave_org ON parametros (clave, COALESCE(organizacion_id, 0));

INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion, minimo, maximo) VALUES
 ('suscripcion.dias_prueba',     '15', 'entero', 'plataforma',   'Suscripción', 'Días de prueba gratuita',
  'Duración de la prueba gratuita al registrarse una organización nueva.', 0, 90),
 ('suscripcion.dias_gracia',     '3',  'entero', 'plataforma',   'Suscripción', 'Días de gracia por no pago',
  'Días después del vencimiento antes de suspender automáticamente una suscripción sin pago.', 0, 60),
 ('sesion.minutos_inactividad',  '30', 'entero', 'organizacion', 'Seguridad',   'Cierre de sesión por inactividad (minutos)',
  'Minutos sin actividad tras los cuales se cierra la sesión automáticamente.', 5, 480),
 ('cuentas.plazo_pagar_dias',    '30', 'entero', 'organizacion', 'Finanzas',    'Plazo de cuentas por pagar (días)',
  'Días para el vencimiento de cada cuenta por pagar desde que ingresa la unidad.', 0, 365),
 ('cuentas.plazo_cobrar_dias',   '30', 'entero', 'organizacion', 'Finanzas',    'Plazo de cuentas por cobrar (días)',
  'Días para el vencimiento de cada cuenta por cobrar desde que se confirma la orden.', 0, 365),
 ('inventario.umbral_stock_bajo','5',  'entero', 'organizacion', 'Inventario',  'Umbral de stock bajo',
  'Cantidad disponible a partir de la cual un producto se considera con stock bajo en el tablero.', 0, 1000)
ON CONFLICT DO NOTHING;

INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Configuración', 'configuracion', 'Parámetros configurables de la organización', '/configuracion', 'cog', 4, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ─── V18: venta por bodega, súper organización y auditoría ──────────────


-- ── 1. Bodegas asignadas a cada usuario ─────────────────────────────────
CREATE TABLE IF NOT EXISTS usuarios_bodegas (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    bodega_id  BIGINT NOT NULL REFERENCES bodegas(id)  ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, bodega_id)
);

INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo) VALUES
 ('Vender desde otras bodegas', 'ventas_otra_bodega',
  'Permite incluir en órdenes unidades de bodegas distintas a las asignadas al usuario', '/catalog/orders', 'warehouse', 20, TRUE),
 ('Auditoría', 'auditoria', 'Consulta del registro de auditoría de operaciones críticas', '/auditoria', 'history', 5, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ── 2. Súper organización (dueña de la plataforma) ──────────────────────
ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS es_plataforma BOOLEAN NOT NULL DEFAULT FALSE;
-- Solo puede existir una
CREATE UNIQUE INDEX IF NOT EXISTS ux_organizacion_plataforma ON organizaciones (es_plataforma) WHERE es_plataforma;

INSERT INTO organizaciones (nombre, slug, email, activo, es_plataforma)
SELECT 'Administración de la plataforma', 'plataforma', NULL, TRUE, TRUE
WHERE NOT EXISTS (SELECT 1 FROM organizaciones WHERE es_plataforma);

-- Los usuarios con rol Superadministrador pasan a la súper organización
UPDATE usuarios SET organizacion_id = (SELECT id FROM organizaciones WHERE es_plataforma)
 WHERE rol_id = (SELECT id FROM roles WHERE nombre = 'Superadministrador' AND organizacion_id IS NULL);

-- ── 3. Auditoría de operaciones críticas (solo inserción) ────────────────
CREATE TABLE IF NOT EXISTS auditoria (
    id              BIGSERIAL PRIMARY KEY,
    fecha           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    organizacion_id BIGINT,
    usuario_id      BIGINT,
    usuario_email   VARCHAR(255),
    servicio        VARCHAR(40)  NOT NULL,
    accion          VARCHAR(60)  NOT NULL,
    entidad         VARCHAR(60)  NOT NULL,
    entidad_id      VARCHAR(60),
    detalle         TEXT,
    ip              VARCHAR(64),
    user_agent      VARCHAR(300)
);
CREATE INDEX IF NOT EXISTS idx_auditoria_org_fecha ON auditoria (organizacion_id, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_entidad   ON auditoria (entidad, entidad_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_accion    ON auditoria (accion);

-- Inmutable: ni la aplicación ni un acceso directo pueden modificar o borrar registros
CREATE OR REPLACE FUNCTION auditoria_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'La auditoría es de solo inserción: no se puede modificar ni eliminar';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auditoria_inmutable ON auditoria;
CREATE TRIGGER trg_auditoria_inmutable BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria_inmutable();

-- ─── V19: baja de unidades y notas crédito ──────────────────────────────


ALTER TYPE estado_unidad ADD VALUE IF NOT EXISTS 'dado_de_baja';

ALTER TABLE unidades
    ADD COLUMN IF NOT EXISTS tipo_baja    VARCHAR(30),
    ADD COLUMN IF NOT EXISTS motivo_baja  TEXT,
    ADD COLUMN IF NOT EXISTS fecha_baja   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_baja BIGINT;

-- Notas sobre cuentas por pagar (crédito = reduce lo que se le debe al proveedor)
CREATE TABLE IF NOT EXISTS notas_cuenta_pagar (
    id                  BIGSERIAL PRIMARY KEY,
    cuenta_por_pagar_id BIGINT NOT NULL REFERENCES cuentas_por_pagar(id),
    unidad_id           BIGINT REFERENCES unidades(id),
    tipo                VARCHAR(20)  NOT NULL DEFAULT 'credito',
    monto               NUMERIC(14,2) NOT NULL,
    motivo              TEXT NOT NULL,
    usuario_id          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notas_cxp_cuenta ON notas_cuenta_pagar (cuenta_por_pagar_id);

-- ─── V20: devoluciones de venta ─────────────────────────────────────────


ALTER TABLE ordenes ADD COLUMN IF NOT EXISTS monto_devuelto NUMERIC(14,2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS devoluciones_venta (
    id                    BIGSERIAL PRIMARY KEY,
    organizacion_id       BIGINT NOT NULL,
    orden_id              BIGINT NOT NULL REFERENCES ordenes(id),
    cliente_id            BIGINT REFERENCES clientes(id),
    numero                VARCHAR(40) NOT NULL,
    tipo_motivo           VARCHAR(30) NOT NULL,      -- garantia | cambio | error_venta | desistimiento | otro
    observacion           TEXT NOT NULL,
    monto_total           NUMERIC(14,2) NOT NULL,    -- valor acreditado por lo devuelto
    monto_aplicado_saldo  NUMERIC(14,2) NOT NULL DEFAULT 0,  -- parte que redujo lo que el cliente debía
    monto_nota_credito    NUMERIC(14,2) NOT NULL DEFAULT 0,  -- excedente ya pagado -> nota crédito al cliente
    tratamiento           VARCHAR(20),               -- reembolso | saldo_a_favor (si hubo excedente)
    metodo_reembolso      VARCHAR(30),
    referencia_reembolso  VARCHAR(100),
    usuario_id            BIGINT,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_devoluciones_orden ON devoluciones_venta (orden_id);

CREATE TABLE IF NOT EXISTS detalles_devolucion (
    id               BIGSERIAL PRIMARY KEY,
    devolucion_id    BIGINT NOT NULL REFERENCES devoluciones_venta(id),
    detalle_orden_id BIGINT NOT NULL REFERENCES detalles_orden(id),
    unidad_id        BIGINT NOT NULL REFERENCES unidades(id),
    cantidad         INTEGER NOT NULL,
    monto            NUMERIC(14,2) NOT NULL,
    destino          VARCHAR(20) NOT NULL,           -- disponible | revision | baja
    bodega_id        BIGINT REFERENCES bodegas(id)
);
CREATE INDEX IF NOT EXISTS idx_det_devolucion_detalle ON detalles_devolucion (detalle_orden_id);

CREATE TABLE IF NOT EXISTS notas_credito_cliente (
    id               BIGSERIAL PRIMARY KEY,
    organizacion_id  BIGINT NOT NULL,
    cliente_id       BIGINT REFERENCES clientes(id),
    devolucion_id    BIGINT REFERENCES devoluciones_venta(id),
    monto            NUMERIC(14,2) NOT NULL,
    saldo_disponible NUMERIC(14,2) NOT NULL DEFAULT 0,
    tratamiento      VARCHAR(20) NOT NULL,           -- reembolso | saldo_a_favor
    estado           VARCHAR(20) NOT NULL,           -- reembolsada | disponible | aplicada
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_nc_cliente ON notas_credito_cliente (cliente_id, estado);

-- Nueva forma de pago: usar el saldo a favor del cliente
ALTER TYPE metodo_pago_orden ADD VALUE IF NOT EXISTS 'nota_credito';
INSERT INTO modulos (nombre, codigo, descripcion, ruta, icono, orden, activo)
VALUES ('Devoluciones de venta', 'devoluciones', 'Registrar devoluciones de órdenes confirmadas (reembolsos y saldos a favor)', '/catalog/orders', 'undo', 21, TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- ─── V21: devoluciones y contabilidad ───────────────────────────────────


CREATE TABLE IF NOT EXISTS asientos_contables (
    id              BIGSERIAL PRIMARY KEY,
    organizacion_id BIGINT NOT NULL,
    fecha           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo            VARCHAR(40) NOT NULL,          -- devolucion_venta, ...
    referencia      VARCHAR(60) NOT NULL,          -- p. ej. número de la devolución
    documento_id    BIGINT,
    descripcion     TEXT,
    total_debe      NUMERIC(16,2) NOT NULL,
    total_haber     NUMERIC(16,2) NOT NULL,
    usuario_id      BIGINT,
    CONSTRAINT asiento_cuadrado CHECK (total_debe = total_haber)
);
CREATE INDEX IF NOT EXISTS idx_asientos_org_fecha ON asientos_contables (organizacion_id, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_asientos_doc ON asientos_contables (tipo, documento_id);

CREATE TABLE IF NOT EXISTS movimientos_contables (
    id            BIGSERIAL PRIMARY KEY,
    asiento_id    BIGINT NOT NULL REFERENCES asientos_contables(id),
    cuenta_codigo VARCHAR(20)  NOT NULL,
    cuenta_nombre VARCHAR(150),
    debe          NUMERIC(16,2) NOT NULL DEFAULT 0,
    haber         NUMERIC(16,2) NOT NULL DEFAULT 0,
    tercero       VARCHAR(200),
    detalle       VARCHAR(300),
    CONSTRAINT movimiento_un_lado CHECK ((debe > 0 AND haber = 0) OR (haber > 0 AND debe = 0))
);
CREATE INDEX IF NOT EXISTS idx_movcont_asiento ON movimientos_contables (asiento_id);

-- Inmutables: un asiento registrado no se modifica ni se borra (se corrige con otro asiento)
CREATE OR REPLACE FUNCTION contabilidad_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Los asientos contables no se modifican ni eliminan: registra un asiento de corrección';
END;
$$ LANGUAGE plpgsql;
DROP TRIGGER IF EXISTS trg_asientos_inmutables ON asientos_contables;
CREATE TRIGGER trg_asientos_inmutables BEFORE UPDATE OR DELETE ON asientos_contables
    FOR EACH ROW EXECUTE FUNCTION contabilidad_inmutable();
DROP TRIGGER IF EXISTS trg_movcont_inmutables ON movimientos_contables;
CREATE TRIGGER trg_movcont_inmutables BEFORE UPDATE OR DELETE ON movimientos_contables
    FOR EACH ROW EXECUTE FUNCTION contabilidad_inmutable();

-- Cuentas configurables (PUC – Decreto 2650 de 1993; ajústalas a tu plan de cuentas)
INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion) VALUES
 ('contab.devoluciones_ventas',  '4175 Devoluciones en ventas',                  'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: devoluciones en ventas',  'Débito por el valor neto devuelto (sin IVA).'),
 ('contab.iva_por_pagar',        '2408 Impuesto sobre las ventas por pagar',     'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: IVA por pagar',          'Débito por el IVA de lo devuelto.'),
 ('contab.clientes',             '1305 Clientes',                                'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: clientes',               'Crédito por lo que el cliente dejó de deber.'),
 ('contab.caja',                 '1105 Caja',                                    'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: caja',                   'Crédito por reembolsos en efectivo.'),
 ('contab.bancos',               '1110 Bancos',                                  'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: bancos',                 'Crédito por reembolsos por transferencia o tarjeta.'),
 ('contab.anticipos_clientes',   '2805 Anticipos y avances recibidos',           'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: saldo a favor de clientes', 'Crédito cuando el excedente queda como saldo a favor.'),
 ('contab.inventario',           '1435 Mercancías no fabricadas por la empresa', 'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: inventario',             'Débito por el costo de las unidades que reingresan.'),
 ('contab.costo_ventas',         '6135 Comercio al por mayor y al por menor',    'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: costo de ventas',        'Crédito por el costo de las unidades devueltas.'),
 ('contab.perdida_inventario',   '5315 Gastos extraordinarios',                  'cuenta', 'organizacion', 'Contabilidad', 'Cuenta: pérdida de inventario',  'Débito por el costo de unidades devueltas inservibles.')
ON CONFLICT DO NOTHING;


-- ─── V22: Propietario con comodín + enlace de verificación de correo ─────
-- V22: el Propietario administra toda su organización + enlace de verificación de correo

-- ─── 1) Propietario con acceso total (comodín '*') ──────────────────────
-- Antes solo tenía los módulos que existían al sembrar la base: le faltaban los que
-- se agregaron después (roles, configuración, auditoría, devoluciones…) y, si sus
-- permisos se editaron, podía quedar sin 'usuarios'. Con '*' cubre también los futuros.
-- Los permisos de PLATAFORMA (plataforma_*) no se conceden con '*'.
INSERT INTO modulos (nombre, codigo, descripcion, orden, activo)
VALUES ('Todos los módulos', '*', 'Comodín: acceso total a todos los módulos', 0, TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO roles_modulos (rol_id, modulo_id, modulo_codigo, modulo_nombre, crear, leer, actualizar, eliminar)
SELECT r.id, m.id, '*', 'Todos los módulos', TRUE, TRUE, TRUE, TRUE
  FROM roles r JOIN modulos m ON m.codigo = '*'
 WHERE r.nombre = 'Propietario' AND r.organizacion_id IS NULL
ON CONFLICT (rol_id, modulo_id) DO UPDATE
   SET crear = TRUE, leer = TRUE, actualizar = TRUE, eliminar = TRUE,
       modulo_codigo = '*', modulo_nombre = 'Todos los módulos';

-- ─── 2) Enlace de verificación de correo ────────────────────────────────
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_verificacion        VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_verificacion_expira TIMESTAMP;
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS verificacion_enviada_en   TIMESTAMP;
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_token_verificacion
    ON usuarios (token_verificacion) WHERE token_verificacion IS NOT NULL;

INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion, minimo, maximo) VALUES
 ('seguridad.horas_enlace_verificacion', '48', 'entero', 'plataforma', 'Seguridad',
  'Vigencia del enlace de verificación (horas)',
  'Horas durante las que sirve el enlace de verificación de correo enviado al registrarse o al reenviarlo.', 1, 168)
ON CONFLICT DO NOTHING;

-- Verificación: el Propietario debe tener el comodín (1 fila)
SELECT r.id, r.nombre, rm.modulo_codigo, rm.leer
  FROM roles r JOIN roles_modulos rm ON rm.rol_id = r.id
 WHERE r.nombre = 'Propietario' AND r.organizacion_id IS NULL AND rm.modulo_codigo = '*';


-- ─── V23: logo de la organización ─────
-- V23: logo propio de cada organización (opcional)
-- Se guarda normalizado a PNG/JPEG (el frontend convierte SVG a PNG) para que se vea en
-- todos los clientes de correo: Gmail y Outlook no muestran SVG.

ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS logo             BYTEA;
ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS logo_tipo        VARCHAR(30);
ALTER TABLE organizaciones ADD COLUMN IF NOT EXISTS logo_actualizado TIMESTAMP;

COMMENT ON COLUMN organizaciones.logo IS 'Logo de la organización (PNG o JPEG, máx. 300 KB). Se usa en correos, menú y páginas públicas.';


-- ─── V24: código de barras en la unidad ─────
-- V24: el código de barras pertenece a la UNIDAD, no al producto general
-- Cada unidad trae el suyo en el empaque. Si es el EAN del modelo, varias unidades pueden compartirlo;
-- al escanearlo en una venta se toma una unidad disponible (y se pregunta el color si hay varios).

ALTER TABLE unidades ADD COLUMN IF NOT EXISTS codigo_barras VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_unidades_codigo_barras ON unidades (codigo_barras) WHERE codigo_barras IS NOT NULL;

-- Las unidades existentes heredan el código que tenía su producto (si lo tenía)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema = 'gets_shared' AND table_name = 'productos' AND column_name = 'codigo_barras') THEN
    UPDATE unidades u
       SET codigo_barras = p.codigo_barras
      FROM productos p
     WHERE u.producto_id = p.id
       AND u.codigo_barras IS NULL
       AND p.codigo_barras IS NOT NULL AND btrim(p.codigo_barras) <> '';
  END IF;
END $$;

-- El producto general ya no lleva código de barras
DROP INDEX IF EXISTS idx_productos_codigo_barras;
ALTER TABLE productos DROP COLUMN IF EXISTS codigo_barras;


-- ─── V25: avisos de vencimiento y bloqueo inmediato ─────
-- V25: avisos antes del vencimiento de la suscripción y bloqueo inmediato al vencer

-- Registro de avisos enviados: evita repetir el mismo aviso para el mismo vencimiento
CREATE TABLE IF NOT EXISTS avisos_suscripcion (
    id              BIGSERIAL PRIMARY KEY,
    suscripcion_id  BIGINT      NOT NULL REFERENCES suscripciones(id),
    organizacion_id BIGINT      NOT NULL,
    tipo            VARCHAR(20) NOT NULL,          -- previo | suspendida
    fecha_fin       DATE        NOT NULL,          -- vencimiento al que se refiere el aviso
    dias_antes      INTEGER     NOT NULL DEFAULT 0,
    destinatarios   TEXT,
    enviado_en      TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT ux_aviso_suscripcion UNIQUE (suscripcion_id, tipo, fecha_fin, dias_antes)
);
CREATE INDEX IF NOT EXISTS idx_avisos_suscripcion_org ON avisos_suscripcion (organizacion_id, enviado_en DESC);

-- Días antes del vencimiento en que se avisa (lista separada por comas)
INSERT INTO parametros (clave, valor, tipo, alcance, categoria, nombre, descripcion) VALUES
 ('suscripcion.dias_aviso_vencimiento', '7,3,1,0', 'texto', 'plataforma', 'Suscripción',
  'Avisos antes del vencimiento (días)',
  'Días antes del vencimiento en que se envía un correo a los administradores de la organización. Separados por comas; 0 = el mismo día.')
ON CONFLICT DO NOTHING;

-- Bloqueo inmediato: sin días de gracia (la suscripción se suspende el día siguiente a su fecha de fin)
UPDATE parametros
   SET valor = '0',
       descripcion = 'Días después del vencimiento antes de suspender una suscripción sin pago. 0 = bloqueo inmediato al vencer.'
 WHERE clave = 'suscripcion.dias_gracia' AND organizacion_id IS NULL;
