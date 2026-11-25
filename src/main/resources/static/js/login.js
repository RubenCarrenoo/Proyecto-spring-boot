(() => {
  const form = document.getElementById('login-form');
  const alerta = document.getElementById('alerta');

  const mostrar = (msg, tipo = 'error') => {
    alerta.textContent = msg;
    alerta.className = `alert ${tipo}`;
    alerta.hidden = false;
  };

  if (Session.token) location.replace('app.html');
  if (new URLSearchParams(location.search).has('expirada')) {
    mostrar('Tu sesión expiró o el token no es válido. Iniciá sesión de nuevo.', 'info');
  }

  // Botones de usuarios de prueba: rellenan usuario y contraseña (no envían el formulario).
  document.querySelectorAll('.demo-user').forEach(btn => btn.addEventListener('click', () => {
    form.usuario.value = btn.dataset.user;
    form.password.value = btn.dataset.pass;
    document.querySelectorAll('.demo-user').forEach(b => b.classList.toggle('selected', b === btn));
    form.querySelector('button[type="submit"]').focus();
  }));

  form.addEventListener('submit', async e => {
    e.preventDefault();
    const { usuario, password } = Object.fromEntries(new FormData(form));
    if (!usuario || !password) return mostrar('Usuario y contraseña son obligatorios.');
    const btn = form.querySelector('button');
    btn.disabled = true;
    try {
      const login = await api.login(usuario.trim(), password);
      Session.save(login);
      location.replace('app.html');
    } catch (err) {
      mostrar(err.status === 401 ? 'Usuario o contraseña incorrectos.' : err.message);
    } finally {
      btn.disabled = false;
    }
  });
})();
