window.Views = window.Views || {};

/** Disponibilidad (RF): busca salas libres por sede, fecha, horario, capacidad y equipamiento; desde aquí se reserva. */
Views.disponibilidad = async (el, ctx) => {
  const [sedes, equipos] = await Promise.all([api.list('sedes'), api.list('equipamientos')]);
  const f = Views.disponibilidad.filtros || (Views.disponibilidad.filtros = {
    sedeId: sedes[0]?.id ?? '', fecha: manana(), horaInicio: '09:00', horaFin: '10:00', capacidadMinima: '', equipamientoIds: []
  });

  el.innerHTML = `
    <h1>Disponibilidad</h1>
    <form class="filters wrap" id="f-disp">
      <label>Sede<select name="sedeId" required>${sedes.map(s =>
        `<option value="${s.id}" ${String(s.id) === String(f.sedeId) ? 'selected' : ''}>${UI.esc(s.nombre)} (${UI.time(s.horaApertura)}–${UI.time(s.horaCierre)})</option>`).join('')}</select></label>
      <label>Fecha<input type="date" name="fecha" value="${f.fecha}" required></label>
      <label>Desde<input type="time" name="horaInicio" value="${f.horaInicio}" step="900" required></label>
      <label>Hasta<input type="time" name="horaFin" value="${f.horaFin}" step="900" required></label>
      <label>Capacidad mín.<input type="number" name="capacidadMinima" min="1" value="${UI.esc(f.capacidadMinima)}"></label>
      <fieldset class="checks inline"><legend>Equipamiento</legend>${equipos.map(e =>
        `<label class="check"><input type="checkbox" name="equipamientoIds" value="${e.id}" ${f.equipamientoIds.includes(String(e.id)) ? 'checked' : ''}> ${UI.esc(e.nombre)}</label>`).join('')}</fieldset>
      <button class="btn primary">Buscar</button>
    </form>
    <section class="panel" id="resultado"><p class="muted">Elegí un horario y buscá.</p></section>`;

  const form = el.querySelector('#f-disp');
  const out = el.querySelector('#resultado');

  form.onsubmit = e => { e.preventDefault(); buscar(); };
  buscar();

  async function buscar() {
    const fd = new FormData(form);
    Object.assign(f, Object.fromEntries(fd), { equipamientoIds: fd.getAll('equipamientoIds') });
    UI.loading(out);
    try {
      const salas = await api.disponibilidad(f);
      out.innerHTML = `<h2>${salas.length} salas libres el ${UI.esc(f.fecha)} de ${UI.esc(f.horaInicio)} a ${UI.esc(f.horaFin)}</h2>
        ${UI.table([
          { label: 'Sala', key: 'nombre' },
          { label: 'Tipo', html: s => UI.esc(UI.TIPOS_SALA[s.tipo] || s.tipo) },
          { label: 'Capacidad', key: 'capacidad' },
          { label: 'Tarifa/hora', html: s => UI.money(s.tarifaHora) },
          { label: 'Equipamiento', html: s => UI.esc(s.equipamientos.map(e => e.nombre).join(', ') || '—') },
          { label: '', html: s => `<button class="btn small primary" data-reservar="${s.id}">Reservar</button>` }
        ], salas, 'No hay salas libres con esos criterios.')}`;
      out.querySelectorAll('[data-reservar]').forEach(b => b.onclick = () => Views.nuevaReserva({
        salaId: Number(b.dataset.reservar),
        inicio: `${f.fecha}T${f.horaInicio}`,
        fin: `${f.fecha}T${f.horaFin}`,
        asistentes: f.capacidadMinima || 1
      }, () => { location.hash = '#/reservas'; }));
    } catch (err) {
      out.innerHTML = `<div class="alert error">[${UI.esc(err.codigo)}] ${UI.esc(err.message)}</div>`;
    }
  }

  function manana() {
    const d = new Date(Date.now() + 864e5);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
};
