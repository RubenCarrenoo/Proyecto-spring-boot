window.Views = window.Views || {};

/** Mis horas (MEMBER): consumo mensual del plan. El miembroId sale del token, nunca de la URL. */
Views.consumo = async (el, { user }) => {
  const mes = Views.consumo.mes || UI.thisMonth();
  const c = await api.consumo(user.miembroId, mes);
  el.innerHTML = `
    <h1>Mis horas</h1>
    <form class="filters" id="f-mes">
      <label>Mes<input type="month" name="mes" value="${UI.esc(mes)}" required></label>
      <button class="btn">Consultar</button>
    </form>
    <section class="panel">
      <h2>Plan ${UI.esc(c.plan)} · ${UI.esc(c.mes)}</h2>
      ${meter(c)}
      ${Views.consumo.tabla(c)}
    </section>`;
  el.querySelector('#f-mes').onsubmit = e => {
    e.preventDefault();
    Views.consumo.mes = e.target.mes.value;
    Views.consumo(el, { user });
  };

  function meter(c) {
    const total = Number(c.horasIncluidasPlan) || 0;
    const usadas = Number(c.horasIncluidasUsadas) || 0;
    const pct = total ? Math.min(100, Math.round(usadas / total * 100)) : 0;
    return `<div class="meter" role="img" aria-label="${usadas} de ${total} horas usadas">
      <div class="meter-bar" style="width:${pct}%"></div></div>
      <p class="muted">${usadas} h usadas de ${total} h incluidas · quedan <strong>${UI.esc(c.horasIncluidasDisponibles)} h</strong></p>`;
  }
};

/** Tabla de consumo reutilizada en la pantalla de miembros. */
Views.consumo.tabla = c => `
  <table class="kv">
    <tr><th>Horas incluidas en el plan</th><td>${UI.esc(c.horasIncluidasPlan)} h</td></tr>
    <tr><th>Horas incluidas usadas</th><td>${UI.esc(c.horasIncluidasUsadas)} h</td></tr>
    <tr><th>Horas incluidas disponibles</th><td>${UI.esc(c.horasIncluidasDisponibles)} h</td></tr>
    <tr><th>Horas reservadas (no canceladas)</th><td>${UI.esc(c.horasReservadas)} h</td></tr>
    <tr><th>Horas excedentes cobradas</th><td>${UI.esc(c.horasExcedentes)} h</td></tr>
    <tr><th>Costo de salas</th><td>${UI.money(c.costoSalas)}</td></tr>
    <tr><th>Costo de servicios</th><td>${UI.money(c.costoServicios)}</td></tr>
    <tr><th>Penalizaciones por cancelación</th><td>${UI.money(c.penalizaciones)}</td></tr>
    <tr class="total"><th>Total a pagar</th><td>${UI.money(c.totalAPagar)}</td></tr>
  </table>`;
