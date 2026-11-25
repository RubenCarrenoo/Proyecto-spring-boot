window.Views = window.Views || {};

/** Inicio: resumen distinto según el rol. */
Views.inicio = async (el, { user }) => {
  const perfil = await api.perfil();
  const saludo = perfil.nombreMiembro || perfil.usuario;

  if (user.rol === 'MEMBER') {
    const [consumo, reservas] = await Promise.all([
      api.consumo(perfil.miembroId, UI.thisMonth()),
      api.list('reservas')
    ]);
    const proximas = reservas
      .filter(r => ['PENDING', 'CONFIRMED'].includes(r.estado))
      .sort((a, b) => a.inicio.localeCompare(b.inicio))
      .slice(0, 5);
    el.innerHTML = `
      <h1>Hola, ${UI.esc(saludo)}</h1>
      <p class="muted">Plan ${UI.esc(consumo.plan)} · ${UI.esc(consumo.mes)}</p>
      <div class="cards">
        ${card('Horas disponibles del plan', `${consumo.horasIncluidasDisponibles} h`, `de ${consumo.horasIncluidasPlan} h incluidas`)}
        ${card('Horas reservadas este mes', `${consumo.horasReservadas} h`, `${consumo.horasExcedentes} h de excedente`)}
        ${card('Total a pagar del mes', UI.money(consumo.totalAPagar), `penalizaciones ${UI.money(consumo.penalizaciones)}`)}
      </div>
      <section class="panel">
        <div class="panel-head"><h2>Próximas reservas</h2><a class="btn primary" href="#/disponibilidad">Reservar sala</a></div>
        ${UI.table([
          { label: 'Sala', html: r => `${UI.esc(r.salaNombre)}<br><small class="muted">${UI.esc(r.sedeNombre)}</small>` },
          { label: 'Inicio', html: r => UI.dateTime(r.inicio) },
          { label: 'Horas', key: 'horas' },
          { label: 'Estado', html: r => UI.badge(r.estado) }
        ], proximas, 'No tenés reservas activas.')}
      </section>`;
    return;
  }

  const hoy = UI.today();
  const [reservasHoy, pendientes, suspendidos, salas] = await Promise.all([
    api.list('reservas', { fecha: hoy }),
    api.list('reservas', { estado: 'PENDING' }),
    api.list('miembros', { estado: 'SUSPENDED' }),
    api.list('salas')
  ]);
  el.innerHTML = `
    <h1>Hola, ${UI.esc(saludo)}</h1>
    <p class="muted">Rol ${UI.esc(user.rol)} · ${new Date().toLocaleDateString('es-CO', { dateStyle: 'full' })}</p>
    <div class="cards">
      ${card('Reservas de hoy', reservasHoy.length)}
      ${card('Pendientes de confirmar', pendientes.length, '', '#/reservas')}
      ${card('Miembros suspendidos', suspendidos.length, '', '#/miembros')}
      ${card('Salas activas', salas.filter(s => s.activa).length)}
    </div>
    <section class="panel">
      <div class="panel-head"><h2>Reservas de hoy</h2><a class="btn primary" href="#/disponibilidad">Nueva reserva</a></div>
      ${UI.table([
        { label: 'Sala', key: 'salaNombre' },
        { label: 'Miembro', key: 'miembroNombre' },
        { label: 'Inicio', html: r => UI.dateTime(r.inicio) },
        { label: 'Fin', html: r => UI.dateTime(r.fin) },
        { label: 'Estado', html: r => UI.badge(r.estado) }
      ], reservasHoy.sort((a, b) => a.inicio.localeCompare(b.inicio)), 'No hay reservas para hoy.')}
    </section>`;

  function card(title, value, sub = '', href = '') {
    const inner = `<span class="card-title">${UI.esc(title)}</span><strong class="card-value">${UI.esc(value)}</strong>${sub ? `<small class="muted">${UI.esc(sub)}</small>` : ''}`;
    return href ? `<a class="card link" href="${href}">${inner}</a>` : `<div class="card">${inner}</div>`;
  }
};
