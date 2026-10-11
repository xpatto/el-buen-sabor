import { requestJson, formatPrice, element } from './catalog.js';

const filter = document.querySelector('#category-filter');
const products = document.querySelector('#products');
const status = document.querySelector('#menu-status');
const error = document.querySelector('#menu-error');
const retry = document.querySelector('#menu-retry');
let currentRequest;
let revision = 0;

function productCard(product) {
    const card = element('article', 'product-card');
    const heading = element('div', 'product-heading');
    heading.append(element('h3', '', product.nombre), element('p', 'price', formatPrice(product.precio)));
    card.append(element('p', 'eyebrow', product.categoria), heading, element('p', 'description', product.descripcion));
    return card;
}

async function loadProducts() {
    currentRequest?.abort();
    currentRequest = new AbortController();
    const requestRevision = ++revision;
    products.replaceChildren();
    products.setAttribute('aria-busy', 'true');
    status.textContent = 'Cargando el menú…';
    error.textContent = '';
    retry.hidden = true;
    try {
        const query = filter.value ? '?categoria=' + encodeURIComponent(filter.value) : '';
        const data = await requestJson('/api/productos' + query, { signal: currentRequest.signal });
        if (requestRevision !== revision) return;
        const cards = data.map(productCard);
        products.replaceChildren(...cards);
        status.textContent = data.length ? data.length + ' productos disponibles.' : 'Por ahora no hay productos en esta categoría.';
    } catch (failure) {
        if (failure.name === 'AbortError' || requestRevision !== revision) return;
        status.textContent = '';
        error.textContent = 'No pudimos cargar el menú. Comprueba tu conexión e inténtalo de nuevo.';
        retry.hidden = false;
    } finally {
        if (requestRevision === revision) products.setAttribute('aria-busy', 'false');
    }
}

async function loadMenu() {
    filter.disabled = true;
    retry.hidden = true;
    error.textContent = '';
    status.textContent = 'Cargando categorías…';
    try {
        const categories = await requestJson('/api/categorias');
        const selected = filter.value;
        const all = element('option', '', 'Todos');
        all.value = '';
        filter.replaceChildren(all);
        for (const category of categories) {
            const option = element('option', '', category.nombre);
            option.value = category.slug;
            filter.append(option);
        }
        filter.value = categories.some(category => category.slug === selected) ? selected : '';
        filter.disabled = false;
        await loadProducts();
    } catch {
        status.textContent = '';
        error.textContent = 'No pudimos cargar las categorías. Comprueba tu conexión e inténtalo de nuevo.';
        retry.hidden = false;
    }
}

filter.addEventListener('change', loadProducts);
retry.addEventListener('click', loadMenu);
loadMenu();
