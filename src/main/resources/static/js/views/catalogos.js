window.Views = window.Views || {};

/**
 * Catálogos (RF-01): CRUD genérico para sedes, salas, equipamientos, servicios adicionales y planes.
 * Cada recurso se describe con sus columnas y sus campos; el mismo código hace listar/crear/editar/eliminar.
 * Sólo ADMIN ve esta pantalla (la API devuelve 403 a los demás en POST/PUT/DELETE).
 */
Views.catalogos = async (el, { params }) => {
  const RECURSOS = {
    sedes: {
      titulo: 'Sedes', singular: 'sede',
      columnas: [
        { label: 'Nombre', key: 'nombre' }, { label: 'Ciudad', key: 'ciudad' },
        { label: 'Dirección', key: 'direccion' },
        { label: 'Horario', html: r => `${UI.time(r.horaApertura)} – ${UI.time(r.horaCierre)}` }
      ],
      campos: async () => [
        { name: 'nombre', label: 'Nombre', required: true },
        { name: 'ciudad', label: 'Ciudad', required: true },
        { name: 'direccion', label: 'Dirección', required: true },
        { name: 'horaApertura', label: 'Hora de apertura', type: 'time', required: true },
        { name: 'horaCierre', label: 'Hora de cierre', type: 'time', required: true }
      ],
      aForm: r => ({ ...r, horaApertura: UI.time(r.horaApertura), horaCierre: UI.time(r.horaCierre) })
    },
    salas: {
      titulo: 'Salas', singular: 'sala',
      columnas: [
        { label: 'Nombre', key: 'nombre' }, { label: 'Sede', key: 'sedeNombre' },
        { label: 'Tipo', html: r => UI.esc(UI.TIPOS_SALA[r.tipo] || r.tipo) },
        { label: 'Capacidad', key: 'capacidad' },
        { label: 'Tarifa/hora', html: r => UI.money(r.tarifaHora) },
        { label: 'Equipamiento', html: r => UI.esc(r.equipamientos.map(e => e.nombre).join(', ') || '—') },
        { label: 'Activa', html: r => r.activa ? 'Sí' : 'No' }
      ],
      campos: async () => {
        const [sedes, equipos] = await Promise.all([api.list('sedes'), api.list('equipamientos')]);
        return [
          { name: 'nombre', label: 'Nombre', required: true },
          { name: 'tipo', label: 'Tipo', type: 'select', required: true,
            options: Object.entries(UI.TIPOS_SALA).map(([value, label]) => ({ value, label })) },
          { name: 'capacidad', label: 'Capacidad', type: 'number', min: 1, required: true },
          { name: 'tarifaHora', label: 'Tarifa por hora (COP)', type: 'number', min: 1, step: '0.01', required: true },
          { name: 'sedeId', label: 'Sede', type: 'select', numeric: true, required: true,
            options: sedes.map(s => ({ value: s.id, label: s.nombre })) },
          { name: 'equipamientoIds', label: 'Equipamiento', type: 'multiselect',
            options: equipos.map(e => ({ value: e.id, label: e.nombre })) }
        ];
      },
      aForm: r => ({ ...r, equipamientoIds: r.equipamientos.map(e => e.id) })
    },
    equipamientos: {
      titulo: 'Equipamientos', singular: 'equipamiento',
      columnas: [{ label: 'Nombre', key: 'nombre' }],
      campos: async () => [{ name: 'nombre', label: 'Nombre', required: true }]
    },
    'servicios-adicionales': {
      titulo: 'Servicios adicionales', singular: 'servicio',
      columnas: [{ label: 'Nombre', key: 'nombre' }, { label: 'Precio unitario', html: r => UI.money(r.precioUnitario) }],
      campos: async () => [
        { name: 'nombre', label: 'Nombre', required: true },
        { name: 'precioUnitario', label: 'Precio unitario (COP)', type: 'number', min: 1, step: '0.01', required: true }
      ]
    },
    planes: {
      titulo: 'Planes de membresía', singular: 'plan',
      columnas: [
        { label: 'Nombre', key: 'nombre' },
        { label: 'Horas incluidas/mes', key: 'horasIncluidasMes' },
        { label: 'Descuento excedente', html: r => `${Math.round(Number(r.descuentoExcedente) * 100)} %` },
        { label: 'Máx. reservas activas', key: 'maxReservasActivas' }
      ],
      campos: async () => [
        { name: 'nombre', label: 'Nombre', required: true },
        { name: 'horasIncluidasMes', label: 'Horas incluidas por mes', type: 'number', min: 0, required: true },
        { name: 'descuentoExcedente', label: 'Descuento sobre horas excedentes (0 a 1)', type: 'number',
          min: 0, max: 1, step: '0.0001', required: true, help: 'Ej.: 0.15 = 15 %' },
        { name: 'maxReservasActivas', label: 'Máximo de reservas activas', type: 'number', min: 1, required: true }
      ]
    }
  };

  const actual = RECURSOS[params[0]] ? params[0] : 'sedes';
  const cfg = RECURSOS[actual];
  const filas = await api.list(actual);

  el.innerHTML = `
    <h1>Catálogos</h1>
    <div class="tabs">${Object.entries(RECURSOS).map(([k, r]) =>
      `<a href="#/catalogos/${k}" class="${k === actual ? 'active' : ''}">${r.titulo}</a>`).join('')}</div>
    <section class="panel">
      <div class="panel-head"><h2>${cfg.titulo} <span class="muted">(${filas.length})</span></h2>
        <button class="btn primary" id="nuevo">Nuevo ${cfg.singular}</button></div>
      ${UI.table([
        { label: 'Id', key: 'id' }, ...cfg.columnas,
        { label: '', html: r => `<div class="actions">
            <button class="btn small" data-edit="${r.id}">Editar</button>
            <button class="btn small danger" data-del="${r.id}">Eliminar</button></div>` }
      ], filas)}
    </section>`;

  const recargar = () => Views.catalogos(el, { params: [actual] });

  async function abrir(fila) {
    const campos = await cfg.campos();
    const valores = fila ? (cfg.aForm ? cfg.aForm(fila) : fila) : {};
    UI.formModal({
      title: `${fila ? 'Editar' : 'Nuevo'} ${cfg.singular}`,
      fields: campos.map(c => ({ ...c, value: valores[c.name] })),
      onSubmit: async datos => {
        if (fila) await api.update(actual, fila.id, datos);
        else await api.create(actual, datos);
        UI.toast(`${cfg.singular[0].toUpperCase() + cfg.singular.slice(1)} ${fila ? 'actualizado' : 'creado'}.`);
        recargar();
      }
    });
  }

  el.querySelector('#nuevo').onclick = () => abrir(null);
  el.querySelectorAll('[data-edit]').forEach(b => b.onclick = () =>
    abrir(filas.find(f => f.id === Number(b.dataset.edit))));
  el.querySelectorAll('[data-del]').forEach(b => b.onclick = async () => {
    if (!UI.confirmar(`¿Eliminar ${cfg.singular} #${b.dataset.del}?`)) return;
    try {
      await api.remove(actual, b.dataset.del);
      UI.toast('Eliminado.');
      recargar();
    } catch (err) { UI.error(err); } // p. ej. RN-12: sala con reservas futuras
  });
};
