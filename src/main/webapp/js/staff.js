let services = [];
let selectedService = '';
let current = null;

const $ = id => document.getElementById(id);

const user = JSON.parse(sessionStorage.getItem('queueLessUser') || 'null');

if (!user) {
    location.href = 'login.html';
} else {
    $('staffName').textContent = user.name;
    $('staffRole').textContent = user.role;
    $('helloName').textContent = user.name.split(' ')[0];
    $('staffAvatar').textContent = user.name.charAt(0).toUpperCase();
    $('dateText').textContent = new Date().toLocaleDateString(undefined, {
        weekday: 'long',
        month: 'long',
        day: 'numeric',
        year: 'numeric'
    });
}

function toast(t, error = false) {
    const x = $('dashMessage');
    x.textContent = t;
    x.className = 'toast';

    if (error) x.style.background = '#b42318';

    setTimeout(() => x.className = 'toast hidden', 3000);
}

async function api(url, opts = {}) {

    opts.credentials = 'include';

    const r = await fetch('/' + url.replace(/^\/+/, ''), opts);

    let d = {};

    try {
        d = await r.json();
    } catch {}

    if (r.status === 401) {
        sessionStorage.removeItem('queueLessUser');
        location.href = 'login.html';
        throw new Error('Session expired.');
    }

    if (!r.ok || d.success === false) {
        throw new Error(d.message || 'Request failed.');
    }

    return d;
}

async function loadServices() {

    try {

        const j = await api('api/services');

        services = j.services || [];

        const active = services.filter(s => s.status === 'ACTIVE');

        const sel = $('dashboardService');

        if (!active.length) {

            sel.innerHTML =
                '<option value="">No services configured</option>';

            selectedService = '';

            renderServices();
            renderQueue([]);

            await loadSystemStats();

            return;
        }

        sel.innerHTML = active.map(s =>
            `<option value="${s.id}">
                ${esc(s.name)} · ${s.prefix}
            </option>`
        ).join('');

        if (
            !selectedService ||
            !active.some(s => String(s.id) === String(selectedService))
        ) {
            selectedService = String(active[0].id);
        }

        sel.value = selectedService;

        renderServices();

        await refreshAll();

    } catch (e) {

        toast(e.message, true);
    }
}

async function refreshAll() {

    await loadSystemStats();

    if (!selectedService) {
        renderQueue([]);
        return;
    }

    await Promise.all([
        loadStats(),
        loadQueue()
    ]);
}

async function loadSystemStats() {

    try {

        const d = await api('api/system-stats');

        $('customerCount').textContent = d.customers;
        $('serviceCount').textContent = d.services;
        $('entryTotal').textContent = d.queueEntries;

    } catch (e) {

        toast(e.message, true);
    }
}

async function loadStats() {

    try {

        const d = await api(
            'api/dashboard?serviceId=' + encodeURIComponent(selectedService)
        );

        $('waitingStat').textContent = d.waiting;
        $('servingStat').textContent = d.serving || '—';
        $('completedStat').textContent = d.completed;

        $('avgStat').innerHTML =
            `${d.averageWait || 0} <em>min</em>`;

        current = d.serving
            ? {
                token: d.serving,
                name: d.servingName
            }
            : null;

        await renderCurrent();

    } catch (e) {

        toast(e.message, true);
    }
}

async function renderCurrent() {

    if (current) {

        $('currentServing').classList.remove('empty');

        $('currentToken').textContent = current.token;
        $('currentName').textContent = current.name;

        $('currentService').textContent =
            services.find(
                s => String(s.id) === String(selectedService)
            )?.name || '';

        $('completeBtn').disabled = false;
        $('skipBtn').disabled = false;

    } else {

        $('currentServing').classList.add('empty');

        $('currentToken').textContent = '—';

        $('currentName').textContent =
            'No customer is being served';

        $('currentService').textContent =
            'Call the next waiting customer to begin.';

        $('completeBtn').disabled = true;
        $('skipBtn').disabled = true;
    }
}

async function loadQueue() {

    try {

        const d = await api(
            'api/queue/list?serviceId=' +
            encodeURIComponent(selectedService)
        );

        renderQueue(d.entries || []);

    } catch (e) {

        toast(e.message, true);
    }
}

function renderQueue(entries) {

    $('entryCount').textContent =
        `${entries.length} ${
            entries.length === 1 ? 'entry' : 'entries'
        }`;

    const body = $('queueTable');
    const empty = $('queueEmpty');

    if (!entries.length) {

        body.innerHTML = '';

        empty.classList.remove('hidden');

        return;
    }

    empty.classList.add('hidden');

    body.innerHTML = entries.map(q => `

        <tr>

            <td class="token-cell">
                ${esc(q.token)}
            </td>

            <td>
                <b>${esc(q.name)}</b>
            </td>

            <td>
                ${esc(q.phone)}
            </td>

            <td>

                <span class="type-badge ${
                    q.registrationType === 'ONLINE'
                        ? 'type-online'
                        : 'type-physical'
                }">

                    ${q.registrationType}

                </span>

            </td>

            <td>
                ${esc(q.registeredAt)}
            </td>

            <td>

                <span class="queue-status ${q.status.toLowerCase()}">

                    ${q.status}

                </span>

            </td>

            <td>

                ${
                    q.status === 'WAITING'
                        ? `<button
                            class="table-action"
                            onclick="cancelEntry(${q.id})">
                            Cancel
                           </button>`
                        : '—'
                }

            </td>

        </tr>

    `).join('');
}

async function queueAction(action, id = null) {

    try {

        const body = new URLSearchParams({
            action: action
        });

        body.set('serviceId', selectedService);

        if (action !== 'next') {
            body.set('id', id);
        }

        const d = await api(
            'api/queue/action',
            {
                method: 'POST',
                body: body
            }
        );

        toast(d.message);

        await refreshAll();

    } catch (e) {

        toast(e.message, true);
    }
}

window.cancelEntry = id =>
    queueAction('cancel', id);

$('nextBtn').onclick =
    () => queueAction('next');

$('completeBtn').onclick =
    () => currentTokenId('complete');

$('skipBtn').onclick =
    () => currentTokenId('skip');

async function currentTokenId(action) {

    try {

        const d = await api(
            'api/queue/list?serviceId=' +
            encodeURIComponent(selectedService)
        );

        const q = (d.entries || [])
            .find(x => x.status === 'SERVING');

        if (q) {

            await queueAction(action, q.id);

        } else {

            toast(
                'There is no customer currently serving.',
                true
            );
        }

    } catch (e) {

        toast(e.message, true);
    }
}

$('dashboardService').onchange = e => {

    selectedService = e.target.value;

    refreshAll();
};

$('logoutBtn').onclick = async () => {

    try {

        await fetch('/api/logout', {
            method: 'POST',
            credentials: 'include'
        });

    } finally {

        sessionStorage.removeItem('queueLessUser');

        location.href = 'login.html';
    }
};

$('mobileMenu').onclick = () => {

    document
        .querySelector('.sidebar')
        .classList.toggle('open');
};

$('walkinBtn').onclick =
$('walkinBtn2').onclick =
    () => openWalkin();

$('newServiceBtn').onclick =
    () => openService();

$('closeModal').onclick =
    closeModal;

document
    .querySelector('.modal-backdrop')
    .onclick = closeModal;

function openWalkin() {

    openModal(`

        <span class="eyebrow">
            ON-SITE REGISTRATION
        </span>

        <h2>
            Register walk-in
        </h2>

        <p class="muted">
            Add a physical customer to the same queue
            as online registrations.
        </p>

        <form id="walkinForm">

            <label>
                Service

                <select name="serviceId" required>

                    ${services
                        .filter(s => s.status === 'ACTIVE')
                        .map(s =>
                            `<option value="${s.id}">
                                ${esc(s.name)} · ${s.prefix}
                            </option>`
                        )
                        .join('')}

                </select>

            </label>

            <label>
                Full name

                <input
                    name="name"
                    required
                    maxlength="120"
                    placeholder="Customer name">
            </label>

            <label>
                Phone number

                <input
                    name="phone"
                    required
                    maxlength="20"
                    placeholder="10-digit phone number">
            </label>

            <div class="modal-actions">

                <button
                    type="button"
                    class="btn btn-secondary"
                    onclick="closeModal()">

                    Cancel

                </button>

                <button
                    class="btn btn-primary">

                    Register & Get Token

                </button>

            </div>

        </form>
    `);

    if (!services.some(s => s.status === 'ACTIVE')) {

        toast('Create a service first.', true);
    }

    $('walkinForm').onsubmit = async e => {

        e.preventDefault();

        try {

            const d = await api(
                'api/register',
                {
                    method: 'POST',
                    body: new URLSearchParams(
                        new FormData(e.target)
                    )
                }
            );

            toast(
                `${d.token} registered successfully.`
            );

            closeModal();

            if (
                String(d.serviceId) !==
                String(selectedService)
            ) {

                selectedService = d.serviceId;

                $('dashboardService').value =
                    selectedService;
            }

            await loadServices();

        } catch (err) {

            toast(err.message, true);
        }
    };
}

function openService(edit = null) {

    const s = edit
        ? services.find(x => x.id === edit)
        : null;

    openModal(`

        <span class="eyebrow">
            SERVICE MANAGEMENT
        </span>

        <h2>
            ${s ? 'Edit service' : 'Create service'}
        </h2>

        <p class="muted">

            ${
                s
                    ? 'Update the queue configuration.'
                    : 'Create the first service to start accepting registrations.'
            }

        </p>

        <form id="serviceForm">

            <input
                type="hidden"
                name="id"
                value="${s?.id || ''}">

            <label>
                Service name

                <input
                    name="name"
                    value="${esc(s?.name || '')}"
                    required
                    maxlength="120"
                    placeholder="General Consultation">

            </label>

            <label>
                Description

                <input
                    name="description"
                    value="${esc(s?.description || '')}"
                    maxlength="500"
                    placeholder="Short description">

            </label>

            <label>
                Token prefix

                <input
                    name="prefix"
                    value="${esc(s?.prefix || '')}"
                    required
                    maxlength="5"
                    placeholder="A">

            </label>

            <label>
                Average service time (minutes)

                <input
                    name="averageTime"
                    type="number"
                    min="1"
                    max="240"
                    value="${s?.averageTime || 5}"
                    required>

            </label>

            ${
                s
                    ? `
                    <label>
                        Status

                        <select name="status">

                            <option ${
                                s.status === 'ACTIVE'
                                    ? 'selected'
                                    : ''
                            }>
                                ACTIVE
                            </option>

                            <option ${
                                s.status === 'INACTIVE'
                                    ? 'selected'
                                    : ''
                            }>
                                INACTIVE
                            </option>

                        </select>

                    </label>
                    `
                    : ''
            }

            <div class="modal-actions">

                <button
                    type="button"
                    class="btn btn-secondary"
                    onclick="closeModal()">

                    Cancel

                </button>

                <button class="btn btn-primary">

                    ${s ? 'Save Changes' : 'Create Service'}

                </button>

            </div>

        </form>
    `);

    $('serviceForm').onsubmit = async e => {

        e.preventDefault();

        try {

            const f = new FormData(e.target);

            const opts = {
                method: s ? 'PUT' : 'POST',
                body: new URLSearchParams(f)
            };

            const d = await api(
                'api/services',
                opts
            );

            toast(d.message);

            closeModal();

            await loadServices();

        } catch (err) {

            toast(err.message, true);
        }
    };
}

function renderServices() {

    const list = $('serviceList');

    if (!services.length) {

        list.innerHTML = `

            <div
                class="empty-state"
                style="grid-column:1/-1">

                <div class="empty-icon">
                    ＋
                </div>

                <h3>
                    No services found
                </h3>

                <p>
                    Create your first service
                    to start accepting registrations.
                </p>

            </div>
        `;

        return;
    }

    list.innerHTML = services.map(s => `

        <article class="service-item">

            <div class="service-item-top">

                <div>

                    <h3>
                        ${esc(s.name)}
                    </h3>

                    <p>
                        ${esc(
                            s.description ||
                            'No description added.'
                        )}
                    </p>

                </div>

                <span class="queue-status ${
                    s.status === 'ACTIVE'
                        ? 'completed'
                        : 'cancelled'
                }">

                    ${s.status}

                </span>

            </div>

            <div class="service-meta">

                <span class="meta-chip">
                    Prefix ${esc(s.prefix)}
                </span>

                <span class="meta-chip">
                    ${s.averageTime} min avg.
                </span>

            </div>

            <div class="service-actions">

                <button
                    class="mini-btn"
                    onclick="openService(${s.id})">

                    Edit

                </button>

                ${
                    s.status === 'ACTIVE'
                        ? `
                        <button
                            class="mini-btn"
                            onclick="deactivateService(${s.id})">

                            Deactivate

                        </button>
                        `
                        : ''
                }

            </div>

        </article>

    `).join('');
}

window.openService = openService;

window.deactivateService = async id => {

    if (
        !confirm(
            'Deactivate this service? New registrations will be disabled, but existing queue history remains.'
        )
    ) {
        return;
    }

    try {

        const d = await api(
            'api/services?id=' + id,
            {
                method: 'DELETE'
            }
        );

        toast(d.message);

        await loadServices();

    } catch (e) {

        toast(e.message, true);
    }
};

function openModal(html) {

    $('modalContent').innerHTML = html;

    $('modal').classList.remove('hidden');
}

function closeModal() {

    $('modal').classList.add('hidden');

    $('modalContent').innerHTML = '';
}

function esc(x) {

    return String(x ?? '').replace(
        /[&<>'"]/g,
        c => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[c])
    );
}

loadServices();

setInterval(() => {

    if (selectedService) {
        refreshAll();
    }

}, 5000);