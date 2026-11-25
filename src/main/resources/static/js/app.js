/**
 * Arranque de la SPA: verifica la sesión, arma el menú según el rol y enruta por hash (#/reservas...).
 * El menú sólo oculta lo que el rol no puede usar; la autorización real la hace la API (401/403).
 */
(() => {
  if (!Session.token) { location.replace('index.html'); return; }

  const ALL = ['ADMIN', 'RECEPTIONIST', 'MEMBER'];
  const STAFF = ['ADMIN', 'RECEPTIONIST'];

  const ROUTES = {
    inicio:         { label: 'Inicio',          roles: ALL,       render: Views.inicio },
    disponibilidad: { label: 'Disponibilidad',  roles: ALL,       render: Views.disponibilidad },
    reservas:       { label: 'Reservas',        roles: ALL,       render: Views.reservas },
    consumo:        { label: 'Mis horas',       roles: ['MEMBER'], render: Views.consumo },
    miembros:       { label: 'Miembros',        roles: STAFF,     render: Views.miembros },
    catalogos:      { label: 'Catálogos',       roles: ['ADMIN'], render: Views.catalogos }
  };

  const user = Session.user;
  const view = document.getElementById('view');
  const nav = document.getElementById('nav');

  document.getElementById('user-label').innerHTML =
    `${UI.esc(user.usuario)} <span class="role">${UI.esc(user.rol)}</span>`;
  document.getElementById('logout').onclick = () => { Session.clear(); location.replace('index.html'); };
  document.getElementById('menu-toggle').onclick = () => nav.classList.toggle('open');

  const allowed = Object.entries(ROUTES).filter(([, r]) => r.roles.includes(user.rol));
  nav.innerHTML = allowed.map(([k, r]) => `<a href="#/${k}" data-route="${k}">${r.label}</a>`).join('');

  async function route() {
    const [name, ...rest] = location.hash.replace(/^#\/?/, '').split('/');
    const key = ROUTES[name] && ROUTES[name].roles.includes(user.rol) ? name : 'inicio';
    nav.querySelectorAll('a').forEach(a => a.classList.toggle('active', a.dataset.route === key));
    nav.classList.remove('open');
    UI.loading(view);
    try {
      await ROUTES[key].render(view, { user, params: rest });
    } catch (err) {
      view.innerHTML = `<div class="alert error">[${UI.esc(err.codigo)}] ${UI.esc(err.message)}</div>`;
    }
  }

  window.addEventListener('hashchange', route);
  route();
})();
