/** Utilidades de interfaz: escape de HTML, formato, notificaciones y formularios en modal. */
const UI = (() => {
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  const money = v => v == null ? '—' :
    Number(v).toLocaleString('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

  const dateTime = v => v ? new Date(v).toLocaleString('es-CO', {
    weekday: 'short', day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit'
  }) : '—';

  const time = v => v ? String(v).slice(0, 5) : '';

  const today = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  const thisMonth = () => today().slice(0, 7);

  const ESTADOS = {
    PENDING: 'Pendiente', CONFIRMED: 'Confirmada', COMPLETED: 'Completada', CANCELLED: 'Cancelada',
    ACTIVE: 'Activo', SUSPENDED: 'Suspendido'
  };
  const TIPOS_SALA = { MEETING_ROOM: 'Sala de reuniones', PRIVATE_OFFICE: 'Oficina privada', DESK: 'Escritorio' };
  const badge = estado => `<span class="badge ${esc(estado).toLowerCase()}">${esc(ESTADOS[estado] || estado)}</span>`;

  function toast(msg, tipo = 'ok') {
    const el = document.createElement('div');
    el.className = `toast ${tipo}`;
    el.textContent = msg;
    document.getElementById('toasts').appendChild(el);
    setTimeout(() => el.remove(), 4500);
  }

  /** Muestra un error de la API con su código de regla (RN-xx, 400, 403...). */
  function error(err) {
    toast(`${err.codigo ? `[${err.codigo}] ` : ''}${err.message}`, 'error');
  }

  /**
   * Campo de formulario declarativo.
   * f = {name, label, type: text|number|email|password|time|date|datetime-local|select|multiselect|textarea,
   *      required, options:[{value,label}], step, min, max, value, help}
   */
  function field(f) {
    const req = f.required ? 'required' : '';
    const attrs = ['step', 'min', 'max', 'placeholder']
      .filter(a => f[a] !== undefined).map(a => `${a}="${esc(f[a])}"`).join(' ');
    let input;
    if (f.type === 'select') {
      input = `<select name="${f.name}" ${req}>
        ${f.required ? '' : '<option value="">—</option>'}
        ${f.options.map(o => `<option value="${esc(o.value)}" ${String(o.value) === String(f.value ?? '') ? 'selected' : ''}>${esc(o.label)}</option>`).join('')}
      </select>`;
    } else if (f.type === 'multiselect') {
      const sel = (f.value || []).map(String);
      input = `<div class="checks">${f.options.map(o => `
        <label class="check"><input type="checkbox" name="${f.name}" value="${esc(o.value)}" ${sel.includes(String(o.value)) ? 'checked' : ''}> ${esc(o.label)}</label>`).join('') || '<span class="muted">Sin opciones</span>'}</div>`;
    } else {
      input = `<input name="${f.name}" type="${f.type || 'text'}" value="${esc(f.value ?? '')}" ${req} ${attrs}>`;
    }
    return `<label class="field">${esc(f.label)}${f.required ? ' *' : ''}${input}${f.help ? `<small class="muted">${esc(f.help)}</small>` : ''}</label>`;
  }

  /** Lee el formulario respetando los tipos declarados. */
  function readForm(form, fields) {
    const out = {};
    for (const f of fields) {
      if (f.type === 'multiselect') {
        out[f.name] = [...form.querySelectorAll(`input[name="${f.name}"]:checked`)].map(i => Number(i.value));
        continue;
      }
      const raw = form.elements[f.name]?.value?.trim() ?? '';
      if (raw === '') { out[f.name] = null; continue; }
      if (f.type === 'number' || f.numeric) out[f.name] = Number(raw);
      else if (f.type === 'datetime-local') out[f.name] = raw.length === 16 ? `${raw}:00` : raw;
      else out[f.name] = raw;
    }
    return out;
  }

  /**
   * Abre un formulario en modal. onSubmit(datos) puede lanzar ApiError: se muestra dentro del modal
   * y el modal sigue abierto para corregir.
   */
  function formModal({ title, fields, okText = 'Guardar', onSubmit, extraHtml = '' }) {
    const dlg = document.getElementById('modal');
    const form = document.getElementById('modal-form');
    const errBox = document.getElementById('modal-error');
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-body').innerHTML =
      `<div class="grid-form">${fields.map(field).join('')}</div>${extraHtml}`;
    document.getElementById('modal-ok').textContent = okText;
    document.getElementById('modal-ok').hidden = !onSubmit;
    errBox.hidden = true;

    form.onsubmit = async e => {
      e.preventDefault();
      if (!onSubmit) return dlg.close();
      if (!form.checkValidity()) { form.reportValidity(); return; }
      const ok = document.getElementById('modal-ok');
      ok.disabled = true;
      try {
        await onSubmit(readForm(form, fields), form);
        dlg.close();
      } catch (err) {
        errBox.textContent = `${err.codigo ? `[${err.codigo}] ` : ''}${err.message}`;
        errBox.hidden = false;
      } finally {
        ok.disabled = false;
      }
    };
    form.querySelectorAll('[data-close]').forEach(b => b.onclick = () => dlg.close());
    if (!dlg.open) dlg.showModal();
    return form;
  }

  /** Modal sólo informativo (sin enviar). */
  function infoModal(title, html) {
    formModal({ title, fields: [], extraHtml: html });
  }

  function confirmar(msg) { return window.confirm(msg); }

  function loading(el) { el.innerHTML = '<p class="muted">Cargando…</p>'; }

  function table(columns, rows, empty = 'No hay registros.') {
    if (!rows.length) return `<p class="empty">${esc(empty)}</p>`;
    return `<div class="table-wrap"><table class="data">
      <thead><tr>${columns.map(c => `<th>${esc(c.label)}</th>`).join('')}</tr></thead>
      <tbody>${rows.map(r => `<tr>${columns.map(c => `<td data-label="${esc(c.label)}">${c.html ? c.html(r) : esc(r[c.key])}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></div>`;
  }

  return { esc, money, dateTime, time, today, thisMonth, badge, toast, error, formModal, infoModal,
    confirmar, loading, table, ESTADOS, TIPOS_SALA };
})();
