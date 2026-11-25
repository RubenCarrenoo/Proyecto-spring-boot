window.Views = window.Views || {};

/**
 * Reservas: listado con filtros y acciones según estado y rol.
 *  - ADMIN/RECEPTIONIST: ven todas, reservan a nombre de cualquier miembro, confirman y completan.
 *  - MEMBER: la API sólo le devuelve las suyas; puede reservar y cancelar.
 * Las reglas (RN-01 cruce, RN-02 duración, RN-03 horario, RN-08 cancelación...) las valida el backend;
 * aquí sólo se muestra el mensaje y el código que devuelve.
 */
Views.reservas = async (el, { user }) => {
  const staff = user.rol !== 'MEMBER';
  const f = Views.reservas.filtros || (Views.reservas.filtros = { estado: '', fecha: '', miembroId: '', salaId: '' });
  const [reservas, salas, miembros] = await Promise.all([
    api.list('reservas', f),
    api.list('salas'),
    staff ? api.list('miembros') : Promise.resolve([])
  ]);
  reservas.sort((a, b) => b.inicio.localeCompare(a.inicio));

  el.innerHTML = `
    <h1>${staff ? 'Reservas' : 'Mis reservas'}</h1>
    <form class="filters wrap" id="filtros">
      <label>Estado<select name="estado"><option value="">Todos</option>${Object.entries(UI.ESTADOS)
        .filter(([k]) => ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].includes(k))
        .map(([k, v]) => `<option value="${k}" ${f.estado === k ? 'selected' : ''}>${v}</option>`).join('')}</select></label>
      <label>Fecha<input type="date" name="fecha" value="${UI.esc(f.fecha)}"></label>
      <label>Sala<select name="salaId"><option value="">Todas</option>${salas.map(s =>
        `<option value="${s.id}" ${String(f.salaId) === String(s.id) ? 'selected' : ''}>${UI.esc(s.nombre)}</option>`).join('')}</select></label>
      ${staff ? `<label>Miembro<select name="miembroId"><option value="">Todos</option>${miembros.map(m =>
        `<option value="${m.id}" ${String(f.miembroId) === String(m.id) ? 'selected' : ''}>${UI.esc(m.nombreCompleto)}</option>`).join('')}</select></label>` : ''}
      <button class="btn">Filtrar</button>
      <button class="btn ghost" type="button" id="limpiar">Limpiar</button>
    </form>
    <section class="panel">
      <div class="panel-head"><h2>${reservas.length} reservas</h2><button class="btn primary" id="nueva">Nueva reserva</button></div>
      ${UI.table([
        { label: '#', key: 'id' },
        { label: 'Sala', html: r => `${UI.esc(r.salaNombre)}<br><small class="muted">${UI.esc(r.sedeNombre)}</small>` },
        ...(staff ? [{ label: 'Miembro', key: 'miembroNombre' }] : []),
        { label: 'Inicio', html: r => UI.dateTime(r.inicio) },
        { label: 'Fin', html: r => UI.dateTime(r.fin) },
        { label: 'Horas', key: 'horas' },
        { label: 'Total', html: r => UI.money(r.costoTotal) },
        { label: 'Estado', html: r => UI.badge(r.estado) + (Number(r.cargoCancelacion) > 0
            ? `<br><small class="muted">cargo ${UI.money(r.cargoCancelacion)}</small>` : '') },
        { label: '', html: r => acciones(r) }
      ], reservas, 'No hay reservas con esos filtros.')}
    </section>`;

  function acciones(r) {
    const b = [`<button class="btn small" data-ver="${r.id}">Ver</button>`];
    if (staff && r.estado === 'PENDING') b.push(`<button class="btn small primary" data-confirmar="${r.id}">Confirmar</button>`);
    if (staff && r.estado === 'CONFIRMED') b.push(`<button class="btn small" data-completar="${r.id}">Completar</button>`);
    if (['PENDING', 'CONFIRMED'].includes(r.estado)) b.push(`<button class="btn small danger" data-cancelar="${r.id}">Cancelar</button>`);
    return `<div class="actions">${b.join('')}</div>`;
  }

  const recargar = () => Views.reservas(el, { user });

  el.querySelector('#filtros').onsubmit = e => {
    e.preventDefault();
    Object.assign(f, Object.fromEntries(new FormData(e.target)));
    recargar();
  };
  el.querySelector('#limpiar').onclick = () => { Object.keys(f).forEach(k => f[k] = ''); recargar(); };
  el.querySelector('#nueva').onclick = () => Views.nuevaReserva({}, recargar);

  const accion = (attr, fn, okMsg, confirmMsg) => el.querySelectorAll(`[data-${attr}]`).forEach(btn => btn.onclick = async () => {
    const id = btn.dataset[attr];
    if (confirmMsg && !UI.confirmar(confirmMsg(id))) return;
    try { const r = await fn(id); UI.toast(okMsg(r)); recargar(); } catch (err) { UI.error(err); }
  });
  accion('confirmar', api.confirmar, r => `Reserva #${r.id} confirmada.`);
  accion('completar', api.completar, r => `Reserva #${r.id} completada.`);
  accion('cancelar', api.cancelar,
    r => `Reserva #${r.id} cancelada. Cargo: ${UI.money(r.cargoCancelacion)}.`,
    id => `¿Cancelar la reserva #${id}?\n\nMás de 24 h antes: sin cargo · entre 2 y 24 h: 50 % · menos de 2 h: no se puede (RN-08).`);
  el.querySelectorAll('[data-ver]').forEach(b => b.onclick = () => detalle(Number(b.dataset.ver)));

  async function detalle(id) {
    let r;
    try { r = await api.get('reservas', id); } catch (err) { return UI.error(err); }
    const editable = ['PENDING', 'CONFIRMED'].includes(r.estado);
    UI.infoModal(`Reserva #${r.id}`, `
      <table class="kv">
        <tr><th>Sala</th><td>${UI.esc(r.salaNombre)} · ${UI.esc(r.sedeNombre)}</td></tr>
        <tr><th>Miembro</th><td>${UI.esc(r.miembroNombre)}</td></tr>
        <tr><th>Horario</th><td>${UI.dateTime(r.inicio)} → ${UI.dateTime(r.fin)} (${UI.esc(r.horas)} h)</td></tr>
        <tr><th>Asistentes</th><td>${UI.esc(r.asistentes)}</td></tr>
        <tr><th>Estado</th><td>${UI.badge(r.estado)}</td></tr>
        <tr><th>Costo sala</th><td>${UI.money(r.costoSala)}</td></tr>
        <tr><th>Costo servicios</th><td>${UI.money(r.costoServicios)}</td></tr>
        <tr><th>Cargo cancelación</th><td>${UI.money(r.cargoCancelacion)}</td></tr>
        <tr class="total"><th>Total</th><td>${UI.money(r.costoTotal)}</td></tr>
      </table>
      <h3>Servicios adicionales</h3>
      ${UI.table([
        { label: 'Servicio', key: 'nombre' }, { label: 'Cantidad', key: 'cantidad' },
        { label: 'Subtotal', html: s => UI.money(s.subtotal) },
        { label: '', html: s => editable ? `<button type="button" class="btn small danger" data-quitar="${s.servicioId}">Quitar</button>` : '' }
      ], r.servicios, 'Sin servicios.')}
      ${editable ? '<button type="button" class="btn small" id="add-serv">Agregar servicio</button>' : ''}`);

    const body = document.getElementById('modal-body');
    body.querySelectorAll('[data-quitar]').forEach(b => b.onclick = async () => {
      try { await api.quitarServicio(r.id, b.dataset.quitar); UI.toast('Servicio quitado.'); detalle(r.id); recargar(); }
      catch (err) { UI.error(err); }
    });
    const add = body.querySelector('#add-serv');
    if (add) add.onclick = async () => {
      const servicios = await api.list('servicios-adicionales');
      UI.formModal({
        title: `Agregar servicio a la reserva #${r.id}`,
        fields: [
          { name: 'servicioId', label: 'Servicio', type: 'select', numeric: true, required: true,
            options: servicios.map(s => ({ value: s.id, label: `${s.nombre} · ${UI.money(s.precioUnitario)}` })) },
          { name: 'cantidad', label: 'Cantidad', type: 'number', min: 1, max: 100, value: 1, required: true }
        ],
        okText: 'Agregar',
        onSubmit: async d => {
          await api.agregarServicio(r.id, d.servicioId, d.cantidad);
          UI.toast('Servicio agregado.');
          recargar();
          setTimeout(() => detalle(r.id), 0);
        }
      });
    };
  }
};

/**
 * Formulario de nueva reserva, compartido por Disponibilidad y Reservas.
 * El personal elige el miembro; el MEMBER reserva siempre a su nombre (el backend lo toma del token).
 */
Views.nuevaReserva = async (prefill = {}, despues = () => {}) => {
  const user = Session.user;
  const staff = user.rol !== 'MEMBER';
  let salas, servicios, miembros;
  try {
    [salas, servicios, miembros] = await Promise.all([
      api.list('salas'), api.list('servicios-adicionales'),
      staff ? api.list('miembros', { estado: 'ACTIVE' }) : Promise.resolve([])
    ]);
  } catch (err) { return UI.error(err); }

  const fields = [
    { name: 'salaId', label: 'Sala', type: 'select', numeric: true, required: true, value: prefill.salaId,
      options: salas.filter(s => s.activa).map(s => ({ value: s.id, label: `${s.nombre} · ${s.sedeNombre} (cap. ${s.capacidad}, ${UI.money(s.tarifaHora)}/h)` })) },
    ...(staff ? [{ name: 'miembroId', label: 'Miembro', type: 'select', numeric: true, required: true,
      options: miembros.map(m => ({ value: m.id, label: `${m.nombreCompleto} · ${m.planNombre}` })) }] : []),
    { name: 'inicio', label: 'Inicio', type: 'datetime-local', step: 900, required: true, value: prefill.inicio },
    { name: 'fin', label: 'Fin', type: 'datetime-local', step: 900, required: true, value: prefill.fin },
    { name: 'asistentes', label: 'Asistentes', type: 'number', min: 1, required: true, value: prefill.asistentes || 1 }
  ];
  const servHtml = servicios.length ? `
    <h3>Servicios adicionales (opcional)</h3>
    <div class="grid-form">${servicios.map(s => `
      <label class="field">${UI.esc(s.nombre)} · ${UI.money(s.precioUnitario)}
        <input type="number" min="0" max="100" value="0" data-servicio="${s.id}"></label>`).join('')}</div>` : '';

  UI.formModal({
    title: 'Nueva reserva',
    fields,
    okText: 'Reservar',
    extraHtml: `${servHtml}<p class="muted small">Mínimo 1 hora, en bloques de 15 minutos y dentro del horario de la sede.</p>`,
    onSubmit: async (datos, form) => {
      datos.servicios = [...form.querySelectorAll('[data-servicio]')]
        .map(i => ({ servicioId: Number(i.dataset.servicio), cantidad: Number(i.value) }))
        .filter(s => s.cantidad > 0);
      const r = await api.reservar(datos);
      UI.toast(`Reserva #${r.id} creada (${UI.ESTADOS[r.estado] || r.estado}). Total ${UI.money(r.costoTotal)}.`);
      despues(r);
    }
  });
};
