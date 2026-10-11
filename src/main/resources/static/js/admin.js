import { requestJson, formatPrice, normalizePrice, productBody, element } from './catalog.js';

const panel = typeof document === 'undefined' ? null : document.querySelector('#admin-panel');
if (panel) initializeAdmin();

export async function deleteProductWithConfirmation(product, token, {
    confirm = globalThis.confirm, request = requestJson, onConfirmed = () => {}
} = {}) {
    if (!confirm('¿Eliminar definitivamente ' + product.nombre + '? Esta acción no se puede deshacer.')) return false;
    onConfirmed();
    await request('/api/admin/productos/' + encodeURIComponent(product.id), { method: 'DELETE', token });
    return true;
}

function initializeAdmin() {
    const token = document.querySelector('meta[name="csrf-token"]')?.content;
    const list = document.querySelector('#admin-products');
    const status = document.querySelector('#admin-status');
    const error = document.querySelector('#admin-error');
    const login = document.querySelector('#session-login');
    const refresh = document.querySelector('#refresh-products');
    const add = document.querySelector('#add-product');
    const editor = document.querySelector('#product-editor');
    const form = document.querySelector('#product-form');
    const fields = document.querySelector('#product-fields');
    const title = document.querySelector('#editor-title');
    const cancel = document.querySelector('#cancel-edit');
    const categorySelect = form.elements.namedItem('categoriaId');
    let categories = [];
    let products = [];
    let editingId = null;
    let busy = false;
    let sessionEnded = false;

    function setBusy(value) {
        busy = value;
        panel.setAttribute('aria-busy', String(value));
        add.disabled = value || sessionEnded || categories.length === 0;
        refresh.disabled = value || sessionEnded;
        fields.disabled = value || sessionEnded;
        list.querySelectorAll('button').forEach(button => { button.disabled = value || sessionEnded; });
    }

    function showError(failure, writing = false) {
        error.textContent = failure.message || 'No se pudo completar la operación.';
        if (writing && (!failure.status || failure.status >= 500)) {
            error.textContent += ' Antes de repetir el envío, actualiza el listado para comprobar si se guardó.';
        }
        if (failure.status === 401 || failure.status === 403) {
            sessionEnded = true;
            login.hidden = false;
        }
    }

    function renderProducts() {
        list.replaceChildren(...products.map(product => {
            const card = element('article', 'admin-product');
            const summary = element('div', 'admin-product-summary');
            const name = element('h3', '', product.nombre);
            summary.append(name, element('p', 'muted', product.categoria),
                element('p', 'price', formatPrice(product.precio)),
                element('span', product.activo ? 'badge active' : 'badge inactive', product.activo ? 'Visible' : 'Oculto'));
            const actions = element('div', 'actions');
            const edit = element('button', 'button secondary', 'Editar');
            edit.type = 'button';
            edit.setAttribute('aria-label', 'Editar ' + product.nombre);
            edit.addEventListener('click', () => openEditor(product));
            const toggle = element('button', 'button secondary', product.activo ? 'Ocultar' : 'Reactivar');
            toggle.type = 'button';
            toggle.setAttribute('aria-label', (product.activo ? 'Ocultar ' : 'Reactivar ') + product.nombre);
            toggle.addEventListener('click', () => changeAvailability(product));
            const remove = element('button', 'button danger', 'Eliminar');
            remove.type = 'button';
            remove.setAttribute('aria-label', 'Eliminar definitivamente ' + product.nombre);
            remove.addEventListener('click', () => deleteProduct(product));
            actions.append(edit, toggle, remove);
            card.append(summary, actions);
            return card;
        }));
    }

    async function reloadProducts() {
        products = await requestJson('/api/admin/productos');
        renderProducts();
    }

    async function loadPanel() {
        if (busy || sessionEnded) return;
        setBusy(true);
        error.textContent = '';
        status.textContent = 'Cargando catálogo…';
        try {
            const result = await Promise.all([requestJson('/api/categorias'), requestJson('/api/admin/productos')]);
            categories = result[0];
            products = result[1];
            const selected = categorySelect.value;
            const placeholder = element('option', '', 'Selecciona una categoría');
            placeholder.value = '';
            categorySelect.replaceChildren(placeholder);
            categories.forEach(category => {
                const option = element('option', '', category.nombre);
                option.value = category.id;
                categorySelect.append(option);
            });
            categorySelect.value = categories.some(category => category.id === selected) ? selected : '';
            renderProducts();
            status.textContent = products.length ? products.length + ' productos en el catálogo.' : 'El catálogo está vacío. Agrega el primer producto.';
            if (!categories.length) error.textContent = 'No hay categorías disponibles para registrar productos.';
        } catch (failure) {
            status.textContent = '';
            showError(failure);
        } finally {
            setBusy(false);
        }
    }

    function openEditor(product = null) {
        if (busy || sessionEnded) return;
        editingId = product?.id ?? null;
        form.reset();
        title.textContent = product ? 'Editar producto' : 'Agregar producto';
        form.elements.namedItem('nombre').value = product?.nombre ?? '';
        form.elements.namedItem('descripcion').value = product?.descripcion ?? '';
        form.elements.namedItem('precio').value = product ? normalizePrice(product.precio) : '';
        categorySelect.value = product?.categoriaId ?? '';
        editor.hidden = false;
        error.textContent = '';
        form.elements.namedItem('nombre').focus();
    }

    function closeEditor() {
        editor.hidden = true;
        editingId = null;
        form.reset();
        add.focus();
    }

    async function saveProduct(event) {
        event.preventDefault();
        if (busy || sessionEnded || !form.reportValidity()) return;
        let body;
        try {
            body = productBody(Object.fromEntries(new FormData(form)), categories);
        } catch (failure) {
            showError(failure);
            return;
        }
        setBusy(true);
        error.textContent = '';
        status.textContent = 'Guardando producto…';
        let saved = false;
        try {
            await requestJson(editingId ? '/api/admin/productos/' + encodeURIComponent(editingId) : '/api/admin/productos',
                { method: editingId ? 'PUT' : 'POST', body, token });
            saved = true;
            closeEditor();
            await reloadProducts();
            status.textContent = 'Producto guardado correctamente.';
        } catch (failure) {
            status.textContent = saved ? 'El producto se guardó, pero no pudimos actualizar el listado.' : '';
            showError(failure, !saved);
        } finally {
            setBusy(false);
            if (saved && !sessionEnded) add.focus();
        }
    }

    async function changeAvailability(product) {
        if (busy || sessionEnded) return;
        if (typeof product.activo !== 'boolean') {
            showError(new Error('El estado recibido no es válido. Actualiza el listado.'));
            return;
        }
        setBusy(true);
        error.textContent = '';
        status.textContent = 'Actualizando disponibilidad…';
        let saved = false;
        try {
            await requestJson('/api/admin/productos/' + encodeURIComponent(product.id) + '/estado',
                { method: 'PATCH', body: JSON.stringify({ activo: !product.activo }), token });
            saved = true;
            await reloadProducts();
            status.textContent = product.activo ? 'Producto ocultado del menú.' : 'Producto visible en el menú.';
        } catch (failure) {
            status.textContent = saved ? 'La disponibilidad se guardó, pero no pudimos actualizar el listado.' : '';
            showError(failure, !saved);
        } finally {
            setBusy(false);
            if (saved && !sessionEnded) refresh.focus();
        }
    }

    async function deleteProduct(product) {
        if (busy || sessionEnded) return;
        let deleted = false;
        try {
            deleted = await deleteProductWithConfirmation(product, token, {
                onConfirmed: () => {
                    setBusy(true);
                    error.textContent = '';
                    status.textContent = 'Eliminando producto…';
                }
            });
            if (!deleted) return;
            if (editingId === product.id) closeEditor();
            await reloadProducts();
            status.textContent = 'Producto eliminado definitivamente.'
                + (products.length ? '' : ' El catálogo está vacío.');
        } catch (failure) {
            status.textContent = deleted ? 'El producto se eliminó, pero no pudimos actualizar el listado.' : '';
            showError(failure, !deleted);
        } finally {
            setBusy(false);
            if (deleted && !sessionEnded) refresh.focus();
        }
    }

    add.addEventListener('click', () => openEditor());
    cancel.addEventListener('click', closeEditor);
    refresh.addEventListener('click', loadPanel);
    form.addEventListener('submit', saveProduct);
    loadPanel();
}
