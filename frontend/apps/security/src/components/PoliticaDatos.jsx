// Página pública de política de tratamiento de datos (Habeas Data - Ley 1581 de 2012).
export default function PoliticaDatos() {
  return (
    <div className="ui-shell min-h-screen bg-gray-50 py-10 px-4">
      <div className="max-w-3xl mx-auto bg-white rounded-xl shadow p-8 text-gray-700 leading-relaxed">
        <h1 className="text-2xl font-bold mb-1">Política de Tratamiento de Datos Personales</h1>
        <p className="text-sm text-gray-500 mb-6">Versión 1.0 · Ley 1581 de 2012 (Habeas Data) y Decreto 1377 de 2013</p>

        <h2 className="font-semibold mt-5 mb-1">1. Responsable del tratamiento</h2>
        <p>SaaS Platform es responsable del tratamiento de los datos personales que usted suministra
          al registrarse y al gestionar su suscripción y métodos de pago.</p>

        <h2 className="font-semibold mt-5 mb-1">2. Datos que recolectamos</h2>
        <p>Datos de contacto (nombre, correo) y datos de facturación. De las tarjetas almacenamos
          únicamente el <strong>tipo, proveedor, nombre del titular, fecha de expiración y los últimos 4 dígitos</strong>.
          <strong> No almacenamos el número completo de la tarjeta ni el código de seguridad (CVV).</strong></p>

        <h2 className="font-semibold mt-5 mb-1">3. Finalidad</h2>
        <p>Gestionar su cuenta, habilitar el uso de la plataforma, procesar pagos y suscripciones,
          emitir facturación y cumplir obligaciones legales.</p>

        <h2 className="font-semibold mt-5 mb-1">4. Sus derechos</h2>
        <p>Usted puede conocer, actualizar, rectificar y solicitar la supresión de sus datos, así como
          revocar la autorización otorgada, escribiendo a nuestro canal de atención.</p>

        <h2 className="font-semibold mt-5 mb-1">5. Autorización</h2>
        <p>Al marcar la casilla de autorización en el registro de un método de pago, usted otorga su
          consentimiento libre, previo, expreso e informado para el tratamiento de sus datos conforme a
          esta política. Guardamos la fecha y la versión de la política autorizada.</p>

        <a href="/" className="inline-block mt-8 text-primary-700 underline">Volver</a>
      </div>
    </div>
  );
}
