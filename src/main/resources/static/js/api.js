/**
 * Cliente HTTP de la API CoWorkHub.
 * - Guarda el JWT en sessionStorage (se borra al cerrar la pestaña).
 * - Agrega el encabezado Authorization: Bearer <token> a cada petición.
 * - Convierte el cuerpo de error uniforme {error, codigo, timestamp} en un ApiError.
 */
const API_BASE = window.COWORKHUB_API || (location.port === '8080' ? '' : 'http://localhost:8080');

const Session = {
  get token() { return sessionStorage.getItem('cw.token'); },
  get user() {
    try { return JSON.parse(sessionStorage.getItem('cw.user') || 'null'); } catch { return null; }
  },
  save(login) {
    sessionStorage.setItem('cw.token', login.token);
    sessionStorage.setItem('cw.user', JSON.stringify({
      usuario: login.usuario, rol: login.rol, miembroId: login.miembroId
    }));
  },
  clear() {
    sessionStorage.removeItem('cw.token');
    sessionStorage.removeItem('cw.user');
  },
  hasRole(...roles) { return !!this.user && roles.includes(this.user.rol); }
};

class ApiError extends Error {
  constructor(status, body) {
    super(body?.error || `Error HTTP ${status}`);
    this.status = status;
    this.codigo = body?.codigo || String(status);
  }
}

async function request(method, path, body) {
  const headers = { 'Accept': 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (Session.token) headers['Authorization'] = `Bearer ${Session.token}`;

  let res;
  try {
    res = await fetch(API_BASE + path, {
      method, headers, body: body !== undefined ? JSON.stringify(body) : undefined
    });
  } catch {
    throw new ApiError(0, { error: 'No se pudo conectar con la API. ¿Está corriendo el backend?', codigo: 'RED' });
  }

  if (res.status === 401 && path !== '/auth/login') {
    Session.clear();
    location.href = 'index.html?expirada=1';
    throw new ApiError(401, { error: 'Sesión expirada', codigo: '401' });
  }
  if (res.status === 204) return null;

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new ApiError(res.status, data);
  return data;
}

/** Arma un query string ignorando valores vacíos. */
function qs(params) {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v === undefined || v === null || v === '') return;
    p.append(k, Array.isArray(v) ? v.join(',') : v);
  });
  const s = p.toString();
  return s ? `?${s}` : '';
}

const api = {
  login: (usuario, password) => request('POST', '/auth/login', { usuario, password }),
  perfil: () => request('GET', '/auth/perfil'),

  // Catálogos genéricos: /api/sedes, /api/salas, /api/equipamientos, /api/servicios-adicionales, /api/planes
  list: (res, params = {}) => request('GET', `/api/${res}${qs(params)}`),
  get: (res, id) => request('GET', `/api/${res}/${id}`),
  create: (res, body) => request('POST', `/api/${res}`, body),
  update: (res, id, body) => request('PUT', `/api/${res}/${id}`, body),
  remove: (res, id) => request('DELETE', `/api/${res}/${id}`),

  // Miembros
  suspender: id => request('PATCH', `/api/miembros/${id}/suspender`),
  activar: id => request('PATCH', `/api/miembros/${id}/activar`),
  consumo: (id, mes) => request('GET', `/api/miembros/${id}/consumo${qs({ mes })}`),

  // Disponibilidad
  disponibilidad: params => request('GET', `/api/disponibilidad${qs(params)}`),

  // Reservas
  reservar: body => request('POST', '/api/reservas', body),
  confirmar: id => request('POST', `/api/reservas/${id}/confirmar`),
  cancelar: id => request('POST', `/api/reservas/${id}/cancelar`),
  completar: id => request('POST', `/api/reservas/${id}/completar`),
  agregarServicio: (id, servicioId, cantidad) =>
    request('POST', `/api/reservas/${id}/servicios`, { servicioId, cantidad }),
  quitarServicio: (id, servicioId) => request('DELETE', `/api/reservas/${id}/servicios/${servicioId}`)
};
