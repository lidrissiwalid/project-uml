/* ═══════════════════════════════════════════════════
   STATE
═══════════════════════════════════════════════════ */
let currentUser = null;
let cart = [];
let editingMenuItemId = null;
let workerRefreshTimer = null;
let _workerDeliveredToday = 0; // tracked client-side (backend doesn't return DELIVERED to WORKER)

const API = '/stadiumeats'; // context-path prefix for Tomcat deployment

/* ═══════════════════════════════════════════════════
   UTILITIES
═══════════════════════════════════════════════════ */
function showSpinner(){ document.getElementById('spinner').classList.add('active') }
function hideSpinner(){ document.getElementById('spinner').classList.remove('active') }

function toast(msg, type='success'){
  const c = document.getElementById('toast-container');
  const t = document.createElement('div');
  t.className = 'toast ' + type;
  t.textContent = msg;
  c.appendChild(t);
  setTimeout(()=>{
    t.style.animation='slideOut .3s ease forwards';
    setTimeout(()=>t.remove(),300);
  }, 3500);
}

async function apiFetch(path, options={}){
  showSpinner();
  try {
    const res = await fetch(API + path, {
      credentials: 'include',
      headers: {'Content-Type':'application/json', ...(options.headers||{})},
      ...options
    });
    const data = await res.json().catch(()=>({}));
    if(!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
    return data;
  } finally {
    hideSpinner();
  }
}

function fmt(n){ return parseFloat(n).toFixed(2) + ' DH' }
function statusBadge(s){
  return `<span class="status-badge ${s}">${s.replace('_',' ')}</span>`;
}

/* ═══════════════════════════════════════════════════
   PAGE ROUTING
═══════════════════════════════════════════════════ */
const NAV_BTNS=['btn-menu','btn-cart','btn-my-orders','btn-worker-orders','btn-admin-menu','btn-admin-orders','btn-admin-users'];
function setNavActive(id){
  NAV_BTNS.forEach(b=>{ const el=document.getElementById(b); if(el) el.classList.remove('active'); });
  if(id){ const el=document.getElementById(id); if(el) el.classList.add('active'); }
}

function showPage(name){
  document.querySelectorAll('.page').forEach(p=>p.classList.remove('active'));
  const p = document.getElementById('page-' + name);
  if(p) p.classList.add('active');

  // Stop worker refresh if leaving worker page
  if(name !== 'worker' && workerRefreshTimer){
    clearInterval(workerRefreshTimer);
    workerRefreshTimer = null;
  }

  // Update navbar active button
  const navMap={menu:'btn-menu',cart:'btn-cart',orders:'btn-my-orders',worker:'btn-worker-orders'};
  setNavActive(navMap[name]||null);

  if(name === 'menu')   loadMenu();
  if(name === 'cart')   renderCartPage();
  if(name === 'orders') loadMyOrders();
  if(name === 'worker') { loadWorkerDashboard(); startWorkerRefresh(); }
  if(name === 'admin')  { loadAdminMenu(); }
}

function redirectByRole(role){
  document.getElementById('navbar').style.display='flex';
  document.getElementById('nav-username').textContent=currentUser.username;
  const rb=document.getElementById('nav-role-badge'); rb.textContent=role; rb.className='role-badge '+role;
  // Hide all role buttons first
  NAV_BTNS.forEach(id=>{ const el=document.getElementById(id); if(el) el.style.display='none'; });
  if(role==='CLIENT'){
    ['btn-menu','btn-cart','btn-my-orders'].forEach(id=>document.getElementById(id).style.display='inline-block');
    showPage('menu');
  } else if(role==='WORKER'){
    document.getElementById('btn-worker-orders').style.display='inline-block';
    showPage('worker');
  } else if(role==='ADMIN'){
    ['btn-admin-menu','btn-admin-orders','btn-admin-users'].forEach(id=>document.getElementById(id).style.display='inline-block');
    showPage('admin');
    setNavActive('btn-admin-menu');
  }
}

function goToMenu()   { showPage('menu'); }
function goToCart()   { showPage('cart'); }
function goToOrders() { showPage('orders'); }
function goToWorker() { showPage('worker'); }
function goToAdminTab(tab){
  switchAdminTab(tab);
  const m={menu:'btn-admin-menu',orders:'btn-admin-orders',users:'btn-admin-users'};
  setNavActive(m[tab]||null);
}

/* ═══════════════════════════════════════════════════
   AUTH
═══════════════════════════════════════════════════ */
function switchAuthTab(tab){
  document.getElementById('form-login').style.display = tab==='login' ? '' : 'none';
  document.getElementById('form-register').style.display = tab==='register' ? '' : 'none';
  document.getElementById('tab-login').classList.toggle('active', tab==='login');
  document.getElementById('tab-register').classList.toggle('active', tab==='register');
}

async function handleLogin(e){
  e.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;
  try {
    const data = await apiFetch('/api/auth/login',{
      method:'POST', body: JSON.stringify({username,password})
    });
    currentUser = data;
    sessionStorage.setItem('stadiumeats_user', JSON.stringify(data));
    toast('Welcome back, ' + data.username + '!');
    redirectByRole(data.role);
  } catch(err){
    toast(err.message || 'Login failed','error');
  }
}

async function handleRegister(e){
  e.preventDefault();
  const username = document.getElementById('reg-username').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  try {
    const data = await apiFetch('/api/auth/register',{
      method:'POST', body: JSON.stringify({username,email,password})
    });
    currentUser = data;
    sessionStorage.setItem('stadiumeats_user', JSON.stringify(data));
    toast('Account created! Welcome, ' + data.username + '!');
    redirectByRole(data.role);
  } catch(err){
    toast(err.message || 'Registration failed','error');
  }
}

async function logout(){
  // Always attempt server-side logout but never block on failure
  try {
    await fetch(API + '/api/auth/logout', { method: 'POST', credentials: 'include' });
  } catch(e){}

  // Clear in-memory state
  currentUser = null;
  cart = [];
  editingMenuItemId = null;

  // Clear session storage
  sessionStorage.removeItem('stadiumeats_user');

  // Stop worker auto-refresh
  if(workerRefreshTimer){ clearInterval(workerRefreshTimer); workerRefreshTimer = null; }

  // Reset delivered-today counter
  _workerDeliveredToday = 0;

  // Hide navbar and all nav buttons
  document.getElementById('navbar').style.display='none';
  NAV_BTNS.forEach(id=>{ const el=document.getElementById(id); if(el) el.style.display='none'; });
  // Reset active tab state
  setNavActive(null);

  // Update cart badge
  updateCartBadge();

  // Return to login screen
  showPage('auth');

  // Clear login form fields
  document.getElementById('login-username').value = '';
  document.getElementById('login-password').value = '';

  // Ensure we start on the Login tab (not Register)
  switchAuthTab('login');

  toast('Logged out successfully');
}

/* ═══════════════════════════════════════════════════
   MENU + CART
═══════════════════════════════════════════════════ */
async function loadMenu(){
  const grid = document.getElementById('menu-grid');
  grid.innerHTML = '<div style="color:var(--text-muted);text-align:center;padding:40px;grid-column:1/-1">Loading menu…</div>';
  try {
    const items = await apiFetch('/api/menu');
    grid.innerHTML = '';
    if(!items || items.length === 0){
      grid.innerHTML = '<div style="color:var(--text-muted);text-align:center;padding:60px;grid-column:1/-1">No menu items available at this time.</div>';
      return;
    }
    items.forEach(item=>{
      const card = document.createElement('div');
      card.className = 'menu-card';
      card.innerHTML = `
        <img class="card-img" src="${item.imageUrl||''}" alt="${item.name}" loading="lazy"
             onerror="this.style.background='var(--bg3)';this.src=''"/>
        <div class="card-body">
          <div class="card-category">${item.category}</div>
          <div class="card-name">${item.name}</div>
          <div class="card-desc">${item.description||''}</div>
          <div class="card-footer">
            <span class="card-price">${fmt(item.price)}</span>
            <button class="btn-add" onclick="addToCart(${item.id},'${escHtml(item.name)}',${item.price})">+ Add</button>
          </div>
        </div>`;
      grid.appendChild(card);
    });
  } catch(err){
    grid.innerHTML = '<div style="color:var(--red);text-align:center;padding:60px;grid-column:1/-1">⚠️ Could not load menu. Please try again.</div>';
    toast('Failed to load menu','error');
  }
}

function escHtml(s){ return s.replace(/'/g,"\\'") }




/* ═══════════════════════════════════════════════════
   CART — render as a full page
═══════════════════════════════════════════════════ */
function renderCartPage(){
  const list = document.getElementById('cart-items-list');
  const footer = document.getElementById('cart-footer-section');
  const totalEl = document.getElementById('cart-total');
  const total = cart.reduce((s,i)=>s+i.price*i.qty, 0);
  const totalQty = cart.reduce((s,i)=>s+i.qty, 0);

  // Update cart badge in navbar
  updateCartBadge();

  if(cart.length === 0){
    list.innerHTML = `
      <div class="cart-empty-state">
        <div style="font-size:3rem;margin-bottom:12px">🛒</div>
        <p>Your cart is empty.<br/>Go add some items!</p>
        <button class="btn-go-menu" onclick="goToMenu()">Browse Menu</button>
      </div>`;
    footer.style.display = 'none';
    return;
  }

  footer.style.display = '';
  totalEl.textContent = fmt(total);

  list.innerHTML = cart.map(item => `
    <div class="cart-item-row">
      <div class="ci-info">
        <div class="ci-name">${item.name}</div>
        <div class="ci-unit">${fmt(item.price)} each</div>
      </div>
      <div class="qty-ctrl">
        <button class="qty-btn" onclick="changeQty(${item.id},-1)">−</button>
        <span class="qty-val">${item.qty}</span>
        <button class="qty-btn" onclick="changeQty(${item.id},1)">+</button>
      </div>
      <div class="ci-subtotal">${fmt(item.price * item.qty)}</div>
      <button class="btn-remove-item" onclick="removeFromCart(${item.id})" title="Remove">✕</button>
    </div>`).join('');
}

function updateCartBadge(){
  const totalQty = cart.reduce((s,i)=>s+i.qty,0);
  const btn = document.getElementById('btn-cart');
  if(!btn) return;
  btn.textContent = totalQty > 0 ? `🛒 Cart (${totalQty})` : '🛒 Cart';
}

function addToCart(id, name, price){
  const existing = cart.find(i=>i.id===id);
  if(existing){ existing.qty++; } else { cart.push({id, name, price: parseFloat(price), qty:1}); }
  updateCartBadge();
  toast(name + ' added to cart 🎉');
}

function changeQty(id, delta){
  const item = cart.find(i=>i.id===id);
  if(!item) return;
  item.qty += delta;
  if(item.qty <= 0) cart = cart.filter(i=>i.id!==id);
  // If cart page is open, re-render it
  if(document.getElementById('page-cart').classList.contains('active')) renderCartPage();
  updateCartBadge();
}

function removeFromCart(id){
  cart = cart.filter(i=>i.id!==id);
  if(document.getElementById('page-cart').classList.contains('active')) renderCartPage();
  updateCartBadge();
}

async function placeOrder(){
  const seat=document.getElementById('seat-number').value.trim();
  const payment=document.getElementById('payment-method').value;
  if(!seat){ toast('Please enter your seat number','error'); return; }
  if(cart.length===0){ toast('Your cart is empty','error'); return; }
  if(payment==='ONLINE'){ showPayModal(seat); return; }
  // CASH flow
  const btn=document.getElementById('btn-place-order');
  btn.disabled=true; btn.textContent='Placing order…';
  try {
    const order=await apiFetch('/api/orders',{method:'POST',
      body:JSON.stringify({seatNumber:seat,paymentMethod:'CASH',items:cart.map(i=>({menuItemId:i.id,quantity:i.qty}))})
    });
    cart=[]; updateCartBadge();
    document.getElementById('seat-number').value='';
    toast(`Order #${order.id} placed! Pay on delivery. 🚀`);
    showPage('orders');
  } catch(err){ toast(err.message||'Failed to place order','error'); }
  finally{ btn.disabled=false; btn.textContent='Place Order 🚀'; }
}

/* ══ PAYMENT MODAL LOGIC ══ */
let _pSeat='';
function showPayModal(seat){
  _pSeat=seat;
  ['cc-name','cc-number','cc-expiry','cc-cvv'].forEach(id=>document.getElementById(id).value='');
  document.getElementById('cc-num-prev').textContent='•••• •••• •••• ••••';
  document.getElementById('cc-name-prev').textContent='Your Name';
  document.getElementById('cc-exp-prev').textContent='MM/YY';
  document.querySelectorAll('.cf-err').forEach(e=>e.classList.remove('show'));
  document.querySelectorAll('.cf-g input').forEach(e=>e.classList.remove('err'));
  document.getElementById('pay-err').classList.remove('show');
  document.getElementById('pay-modal-overlay').classList.add('open');
}
function closePayModal(){ document.getElementById('pay-modal-overlay').classList.remove('open'); }
function fmtCard(el){
  const v=el.value.replace(/\D/g,'').slice(0,16);
  el.value=v.replace(/(.{4})/g,'$1 ').trim();
  const d=v+'•'.repeat(16-v.length);
  document.getElementById('cc-num-prev').textContent=d.replace(/(.{4})/g,'$1 ').trim();
  updateCCPreview();
}
function fmtExpiry(el){
  let v=el.value.replace(/\D/g,'');
  if(v.length>=3) v=v.slice(0,2)+'/'+v.slice(2,4);
  el.value=v;
}
function updateCCPreview(){
  document.getElementById('cc-name-prev').textContent=(document.getElementById('cc-name').value.trim()||'Your Name').toUpperCase().slice(0,22);
  document.getElementById('cc-exp-prev').textContent=document.getElementById('cc-expiry').value.trim()||'MM/YY';
}
function validatePayForm(){
  let ok=true;
  function chk(inputId,errId,valid){
    document.getElementById(inputId).classList.toggle('err',!valid);
    document.getElementById(errId).classList.toggle('show',!valid);
    if(!valid) ok=false;
  }
  const name=document.getElementById('cc-name').value.trim();
  const num=document.getElementById('cc-number').value.replace(/\s/g,'');
  const exp=document.getElementById('cc-expiry').value;
  const cvv=document.getElementById('cc-cvv').value;
  chk('cc-name','cc-name-err', name.length>0);
  chk('cc-number','cc-number-err', num.length===16);
  let expOk=false;
  if(/^\d{2}\/\d{2}$/.test(exp)){
    const [m,y]=exp.split('/').map(Number);
    const now=new Date(), cy=now.getFullYear()%100, cm=now.getMonth()+1;
    expOk=m>=1&&m<=12&&(y>cy||(y===cy&&m>=cm));
  }
  chk('cc-expiry','cc-expiry-err', expOk);
  chk('cc-cvv','cc-cvv-err', /^\d{3,4}$/.test(cvv));
  return ok;
}
async function submitOnlineOrder(){
  if(!validatePayForm()) return;
  const btn=document.getElementById('btn-pay-now');
  const spin=document.getElementById('pay-spin');
  const txt=document.getElementById('pay-text');
  btn.disabled=true; spin.style.display='inline-block'; txt.style.display='none';
  document.getElementById('pay-err').classList.remove('show');
  try {
    const order=await apiFetch('/api/orders',{method:'POST',
      body:JSON.stringify({seatNumber:_pSeat,paymentMethod:'ONLINE',items:cart.map(i=>({menuItemId:i.id,quantity:i.qty}))})
    });
    closePayModal();
    cart=[]; updateCartBadge();
    document.getElementById('seat-number').value='';
    toast('Payment successful! Order #'+order.id+' placed. 🎉');
    showPage('orders');
  } catch(err){
    const e=document.getElementById('pay-err');
    e.textContent=err.message||'Payment failed. Try again.';
    e.classList.add('show');
  } finally{ btn.disabled=false; spin.style.display='none'; txt.style.display=''; }
}

/* ═══════════════════════════════════════════════════
   CLIENT: MY ORDERS
═══════════════════════════════════════════════════ */
async function loadMyOrders(){
  try {
    const orders = await apiFetch('/api/orders');
    const list = document.getElementById('orders-list');
    if(!orders.length){
      list.innerHTML = '<div style="text-align:center;color:var(--text-muted);padding:40px">No orders yet. Start ordering!</div>';
      return;
    }
    list.innerHTML = orders.map(o=>renderOrderCard(o, false)).join('');
  } catch(err){
    toast('Failed to load orders','error');
  }
}

function renderOrderCard(o, showActions){
  const items = (o.items||[]).map(i=>`
    <div class="order-item-row">
      <span>${i.menuItemName} × ${i.quantity}</span>
      <span>${fmt(i.unitPrice * i.quantity)}</span>
    </div>`).join('');

  const actions = showActions ? `
    <div class="wo-actions" style="margin-top:10px">
      ${o.status==='PENDING' ? `<button class="btn-action btn-accept" onclick="updateStatus(${o.id},'IN_DELIVERY')">🚀 Accept & Deliver</button>` : ''}
      ${o.status==='IN_DELIVERY' ? `<button class="btn-action btn-deliver" onclick="updateStatus(${o.id},'DELIVERED')">✅ Mark Delivered</button>` : ''}
    </div>` : '';

  return `
    <div class="order-card" id="order-${o.id}">
      <div class="order-header">
        <span class="order-id">Order #${o.id}</span>
        ${statusBadge(o.status)}
        <span class="order-meta">Seat: <strong>${o.seatNumber}</strong> · ${o.createdAt||''}</span>
      </div>
      <div class="order-items-list">${items}</div>
      <div class="order-total-row">
        <span>Total · ${o.paymentMethod}</span>
        <span>${fmt(o.totalPrice)}</span>
      </div>
      ${actions}
    </div>`;
}

/* ═══════════════════════════════════════════════════
   WORKER DASHBOARD
═══════════════════════════════════════════════════ */
async function loadWorkerDashboard(){
  try {
    const orders = await apiFetch('/api/orders');
    // Backend only returns PENDING + IN_DELIVERY to WORKER role
    const pending    = orders.filter(o=>o.status==='PENDING');
    const delivering = orders.filter(o=>o.status==='IN_DELIVERY');

    document.getElementById('stat-pending').textContent    = pending.length;
    document.getElementById('stat-delivering').textContent = delivering.length;
    // Delivered count is tracked client-side (backend doesn't send DELIVERED orders to WORKER)
    document.getElementById('stat-delivered').textContent  = _workerDeliveredToday;

    document.getElementById('worker-pending-list').innerHTML =
      pending.length ? pending.map(o=>renderOrderCard(o,true)).join('') :
        '<div style="color:var(--text-muted);font-size:.85rem">No pending orders 🎉</div>';

    document.getElementById('worker-delivery-list').innerHTML =
      delivering.length ? delivering.map(o=>renderOrderCard(o,true)).join('') :
        '<div style="color:var(--text-muted);font-size:.85rem">No active deliveries</div>';

  } catch(err){
    toast('Failed to load orders','error');
  }
}

function startWorkerRefresh(){
  if(workerRefreshTimer) clearInterval(workerRefreshTimer);
  workerRefreshTimer = setInterval(loadWorkerDashboard, 30000);
}

async function updateStatus(orderId, status){
  try {
    await apiFetch(`/api/orders/${orderId}/status`,{
      method:'PUT', body: JSON.stringify({status})
    });
    toast('Order #' + orderId + ' → ' + status.replace('_',' '));
    // Track delivered count locally — backend only returns PENDING/IN_DELIVERY to WORKER
    if(currentUser && currentUser.role === 'WORKER' && status === 'DELIVERED'){
      _workerDeliveredToday++;
    }
    if(currentUser.role === 'WORKER') loadWorkerDashboard();
    else loadAdminOrders();
  } catch(err){
    toast(err.message || 'Update failed','error');
  }
}

/* ═══════════════════════════════════════════════════
   ADMIN: MENU MANAGEMENT
═══════════════════════════════════════════════════ */
async function loadAdminMenu(){
  try {
    const items = await apiFetch('/api/menu');
    document.getElementById('menu-count').textContent = items.length + ' items';
    const tbody = document.getElementById('admin-menu-tbody');
    tbody.innerHTML = items.map(i=>`
      <tr>
        <td><strong>${i.name}</strong></td>
        <td>${i.category}</td>
        <td>${fmt(i.price)}</td>
        <td><span class="avail-badge ${i.available?'yes':'no'}">${i.available?'Yes':'No'}</span></td>
        <td>
          <button class="btn-edit" onclick="openMenuModal(${JSON.stringify(i).replace(/"/g,'&quot;')})">Edit</button>
          <button class="btn-del" onclick="deleteMenuItem(${i.id},'${escHtml(i.name)}')">Delete</button>
        </td>
      </tr>`).join('');
  } catch(err){ toast('Failed to load menu','error'); }
}

function openMenuModal(item){
  editingMenuItemId = item ? item.id : null;
  document.getElementById('modal-title').textContent = item ? 'Edit Menu Item' : 'Add Menu Item';
  document.getElementById('mi-name').value = item ? item.name : '';
  document.getElementById('mi-desc').value = item ? (item.description||'') : '';
  document.getElementById('mi-price').value = item ? item.price : '';
  document.getElementById('mi-cat').value = item ? item.category : '';
  document.getElementById('mi-img').value = item ? (item.imageUrl||'') : '';
  document.getElementById('mi-avail').value = item ? String(item.available) : 'true';
  document.getElementById('menu-modal-overlay').classList.add('open');
}

function closeMenuModal(e){
  if(!e || e.target === document.getElementById('menu-modal-overlay')){
    document.getElementById('menu-modal-overlay').classList.remove('open');
    editingMenuItemId = null;
  }
}

async function saveMenuItem(e){
  e.preventDefault();
  const payload = {
    name: document.getElementById('mi-name').value.trim(),
    description: document.getElementById('mi-desc').value.trim(),
    price: parseFloat(document.getElementById('mi-price').value),
    category: document.getElementById('mi-cat').value.trim(),
    imageUrl: document.getElementById('mi-img').value.trim(),
    available: document.getElementById('mi-avail').value === 'true'
  };
  try {
    if(editingMenuItemId){
      await apiFetch(`/api/menu/${editingMenuItemId}`,{method:'PUT',body:JSON.stringify(payload)});
      toast('Menu item updated ✓');
    } else {
      await apiFetch('/api/menu',{method:'POST',body:JSON.stringify(payload)});
      toast('Menu item added ✓');
    }
    closeMenuModal();
    loadAdminMenu();
  } catch(err){ toast(err.message||'Save failed','error'); }
}

async function deleteMenuItem(id, name){
  if(!confirm(`Delete "${name}"? This cannot be undone.`)) return;
  try {
    await apiFetch(`/api/menu/${id}`,{method:'DELETE'});
    toast(name + ' deleted');
    loadAdminMenu();
  } catch(err){ toast(err.message||'Delete failed','error'); }
}

/* ══ ADMIN: ALL ORDERS (with filter/search) ══ */
let _adminOrders=[], _orderFilter='';
function setOrderFilter(btn,status){
  _orderFilter=status;
  document.querySelectorAll('.fbtn').forEach(b=>b.classList.remove('on'));
  btn.classList.add('on');
  renderAdminOrders();
}
function renderAdminOrders(){
  const search=(document.getElementById('order-search').value||'').toLowerCase();
  const list=document.getElementById('admin-orders-list');
  let rows=_adminOrders;
  if(_orderFilter) rows=rows.filter(o=>o.status===_orderFilter);
  if(search) rows=rows.filter(o=>String(o.id).includes(search)||(o.clientUsername||'').toLowerCase().includes(search));
  if(!rows.length){ list.innerHTML='<div style="color:var(--text-muted);text-align:center;padding:40px">No orders found.</div>'; return; }
  list.innerHTML=rows.map(o=>{
    const items=(o.items||[]).map(i=>`<div class="order-item-row"><span>${i.menuItemName} × ${i.quantity}</span><span>${fmt(i.unitPrice*i.quantity)}</span></div>`).join('');
    const sel=`<select style="width:auto;padding:5px 10px;font-size:.8rem" onchange="updateStatus(${o.id},this.value)">${['PENDING','CONFIRMED','IN_DELIVERY','DELIVERED','CANCELLED'].map(s=>`<option value="${s}" ${o.status===s?'selected':''}>${s.replace('_',' ')}</option>`).join('')}</select>`;
    return `<div class="order-card"><div class="order-header"><span class="order-id">Order #${o.id}</span>${statusBadge(o.status)}<span class="order-meta">Seat: <strong>${o.seatNumber}</strong> · ${o.createdAt||''}</span></div><div class="order-items-list">${items}</div><div class="order-total-row"><span>Total · ${o.paymentMethod}</span><span>${fmt(o.totalPrice)}</span></div><div style="display:flex;align-items:center;gap:10px;margin-top:10px;font-size:.8rem;color:var(--text-muted)">Client: <strong style="color:var(--text)">${o.clientUsername||''}</strong><div style="margin-left:auto">${sel}</div></div></div>`;
  }).join('');
}
async function loadAdminOrders(){
  try{ _adminOrders=await apiFetch('/api/orders'); renderAdminOrders(); }
  catch(err){ toast('Failed to load orders','error'); }
}

/* ══ ADMIN: USERS ══ */
async function loadAdminUsers(){
  try {
    const users=await apiFetch('/api/auth/users');
    document.getElementById('admin-users-tbody').innerHTML=users.map(u=>{
      const options = ['CLIENT','WORKER','ADMIN'].map(r => `<option value="${r}" ${u.role===r?'selected':''}>${r}</option>`).join('');
      const selectHtml = `<select style="width:auto;padding:5px 10px;font-size:.8rem" onchange="updateUserRole(${u.id},this.value)">${options}</select>`;
      return `<tr><td>${u.id}</td><td><strong>${u.username}</strong></td><td>${u.email}</td><td>${selectHtml}</td><td style="font-size:.75rem;color:var(--text-muted)">${u.createdAt||'—'}</td></tr>`;
    }).join('');
  } catch(err){
    document.getElementById('admin-users-tbody').innerHTML='<tr><td colspan="5" style="text-align:center;color:var(--text-muted);padding:30px">Users list not available.</td></tr>';
  }
}

async function updateUserRole(userId, newRole){
  try {
    await apiFetch('/api/auth/users/' + userId + '/role', {
      method: 'PUT',
      body: JSON.stringify({ role: newRole })
    });
    toast('User role updated to ' + newRole);
    loadAdminUsers();
  } catch(err) {
    toast(err.message || 'Failed to update user role', 'error');
    loadAdminUsers(); // Revert UI
  }
}

/* ═══════════════════════════════════════════════════
   APP INIT — restore session or show login
═══════════════════════════════════════════════════ */
(async function initApp(){
  const saved = sessionStorage.getItem('stadiumeats_user');
  if(saved){
    try {
      currentUser = JSON.parse(saved);
      // Verify the server-side session is still alive with a lightweight call
      await apiFetch('/api/menu');
      redirectByRole(currentUser.role);
      return;
    } catch(e){
      // Session expired on server — clear stale data and show login
      sessionStorage.removeItem('stadiumeats_user');
      currentUser = null;
    }
  }
  // No valid session — show login page
  showPage('auth');
})();

function switchAdminTab(tab){
  ['menu','orders','users'].forEach(t=>{
    document.getElementById('admin-tab-'+t).style.display = t===tab?'':'none';
    document.getElementById('atab-'+t).classList.toggle('active', t===tab);
  });
  if(tab==='menu') loadAdminMenu();
  if(tab==='orders') loadAdminOrders();
  if(tab==='users') loadAdminUsers();
}
