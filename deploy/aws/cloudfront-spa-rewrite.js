// CloudFront Function "spa-rewrite" (evento viewer-request).
// Archivos (con extensión) se sirven tal cual; cualquier ruta de la app va al index.html del host.
function handler(event) {
  var req = event.request;
  var ultimo = req.uri.split('/').pop();
  if (ultimo.indexOf('.') === -1) {
    req.uri = '/index.html';
  }
  return req;
}
