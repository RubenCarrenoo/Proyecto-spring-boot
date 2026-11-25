window.Views = window.Views || {};

/** Miembros (ADMIN, RECEPTIONIST): listar con filtros, registrar con cuenta, editar, suspender/activar y ver consumo. */
Views.miembros = async (el, { user }) => {
  const filtros = Views.miembros.filtros || (Views.miembros.filtros = { nombre: '', estado: '' });
  const [miembros, planes] = await Promise.all([api.list('miembros', filtros), api.list('planes')]);

  el.innerHTML = `
    <h1>Miembros</h1>
    <form class="filters" id="filtros">
      <label>Nombre<input name="nombre" value="${UI.esc(filtros.nombre)}" placeholder="Buscar…"></label>
      <label>Estado<select name="estado">
        <option value="">Todos</option>
        <option value="ACTIVE" ${filtros.estado === 'ACTIVE' ? 'selected' : ''}>Activos</option>
        <option value="SUSPENDED" ${filtros.estado === 'SUSPENDED' ? 'selected' : ''}>Suspendidos</option>
      </select></label>
      <button class="btn">Filtrar</button>
    </form>
    <section class="panel">
      <div class="panel-head"><h2>${miembros.length} miembros</h2><button class="btn primary" id="nuevo">Registrar miembro</button></div>
      ${UI.table([
        { label: 'Id', key: 'id' },
        { label: 'Documento', key: 'documento' },
        { label: 'Nombre', key: 'nombreCompleto' },
        { label: 'Email', key: 'email' },
        { label: 'Plan', key: 'planNombre' },
        { label: 'Cuenta', html: m => m.usuario ? `<code>${UI.esc(m.usuario)}</code>` : '<span class="muted">sin cuenta</span>' },
        { label: 'Estado', html: m => UI.badge(m.estado) },
        { label: '', html: m => `<div class="actions">
            <button class="btn small" data-consumo="${m.id}">Consumo</button>
            <button class="btn small" data-edit="${m.id}">Editar</button>
            ${m.estado === 'ACTIVE'
              ? `<button class="btn small warn" data-suspender="${m.id}">Suspender</button>`
              : `<button class="btn small" data-activar="${m.id}">Activar</button>`}
            ${user.rol === 'ADMIN' ? `<button class="btn small danger" data-del="${m.id}">Eliminar</button>` : ''}
          </div>` }
      ], miembros, 'No hay miembros con esos filtros.')}
    </section>`;

  const recargar = () => Views.miembros(el, { user });
  const planOpts = planes.map(p => ({ value: p.id, label: `${p.nombre} (${p.horasIncluidasMes} h/mes)` }));
  const porId = id => miembros.find(m => m.id === Number(id));

  el.querySelector('#filtros').onsubmit = e => {
    e.preventDefault();
    Object.assign(filtros, Object.fromEntries(new FormData(e.target)));
    recargar();
  };

  el.querySelector('#nuevo').onclick = () => UI.formModal({
    title: 'Registrar miembro',
    fields: [
      { name: 'documento', label: 'Documento', required: true },
      { name: 'nombreCompleto', label: 'Nombre completo', required: true },
      { name: 'email', label: 'Email', type: 'email', required: true },
      { name: 'planId', label: 'Plan', type: 'select', numeric: true, required: true, options: planOpts },
      { name: 'usuario', label: 'Usuario (opcional)', help: 'Si lo completás se crea su cuenta MEMBER para iniciar sesión.' },
      { name: 'password', label: 'Contraseña (opcional)', type: 'password', help: 'Mínimo 8 caracteres. Obligatoria si hay usuario.' }
    ],
    onSubmit: async d => {
      const m = await api.create('miembros', d);
      UI.toast(`Miembro ${m.nombreCompleto} registrado${m.usuario ? ` con usuario ${m.usuario}` : ''}.`);
      recargar();
    }
  });

  el.querySelectorAll('[data-edit]').forEach(b => b.onclick = () => {
    const m = porId(b.dataset.edit);
    UI.formModal({
      title: `Editar miembro #${m.id}`,
      fields: [
        { name: 'documento', label: 'Documento', required: true, value: m.documento },
        { name: 'nombreCompleto', label: 'Nombre completo', required: true, value: m.nombreCompleto },
        { name: 'email', label: 'Email', type: 'email', required: true, value: m.email },
        { name: 'planId', label: 'Plan', type: 'select', numeric: true, required: true, options: planOpts, value: m.planId }
      ],
      onSubmit: async d => { await api.update('miembros', m.id, d); UI.toast('Miembro actualizado.'); recargar(); }
    });
  });

  const accion = (sel, fn, msg) => el.querySelectorAll(sel).forEach(b => b.onclick = async () => {
    const id = Object.values(b.dataset)[0];
    if (msg && !UI.confirmar(msg(porId(id)))) return;
    try { await fn(id); UI.toast('Listo.'); recargar(); } catch (err) { UI.error(err); }
  });
  accion('[data-suspender]', api.suspender, m => `¿Suspender a ${m.nombreCompleto}? No podrá reservar (RN-05).`);
  accion('[data-activar]', api.activar);
  accion('[data-del]', id => api.remove('miembros', id), m => `¿Eliminar a ${m.nombreCompleto}?`);

  el.querySelectorAll('[data-consumo]').forEach(b => b.onclick = async () => {
    try {
      const c = await api.consumo(b.dataset.consumo, UI.thisMonth());
      UI.infoModal(`Consumo de ${c.nombreMiembro} · ${c.mes}`, Views.consumo.tabla(c));
    } catch (err) { UI.error(err); }
  });
};
