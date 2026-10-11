// Los números del contrato se conservan como texto ANTES de JSON.parse.
// El tokenizador distingue cadenas JSON (incluidos escapes) de números.
// Así no se pierden centavos ni identificadores long por pasar por Number.
export function parseCatalogJson(text) {
    return JSON.parse(text.replace(/"(?:\\.|[^"\\])*"|-?(?:0|[1-9]\d*)(?:\.\d+)?(?:[eE][+-]?\d+)?/g,
        token => token.startsWith('"') ? token : JSON.stringify(token)));
}

export function normalizePrice(value) {
    if (typeof value !== 'string' || !/^(0|[1-9]\d{0,16})(\.\d{1,2})?$/.test(value)) {
        throw new Error('Escribe un precio positivo con punto y hasta dos decimales; por ejemplo, 45.50.');
    }
    const [whole, fraction = ''] = value.split('.');
    const decimals = fraction.padEnd(2, '0');
    const cents = BigInt(whole + decimals);
    if (cents <= 0n || cents > 9223372036854775807n) {
        throw new Error('El precio debe estar entre 0.01 y 92233720368547758.07.');
    }
    return whole + '.' + decimals;
}

export function formatPrice(value) {
    const [whole, fraction] = normalizePrice(value).split('.');
    return '$' + whole.replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '.' + fraction + ' MXN';
}

export function productBody(values, categories) {
    const { nombre, descripcion, precio, categoriaId } = values;
    if (!nombre.trim() || nombre.length > 120) {
        throw new Error('El nombre es obligatorio y admite hasta 120 caracteres.');
    }
    if (!descripcion.trim() || descripcion.length > 2000) {
        throw new Error('La descripción es obligatoria y admite hasta 2000 caracteres.');
    }
    if (!/^[1-9]\d{0,18}$/.test(categoriaId) || BigInt(categoriaId) > 9223372036854775807n
            || !categories.some(category => category.id === categoriaId)) {
        throw new Error('Selecciona una categoría disponible.');
    }
    // Solo se interpolan números validados. Los textos siempre pasan por JSON.stringify.
    return '{"nombre":' + JSON.stringify(nombre) + ',"descripcion":' + JSON.stringify(descripcion)
        + ',"precio":' + normalizePrice(precio) + ',"categoriaId":' + categoriaId + '}';
}

export class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

export async function requestJson(url, { method = 'GET', body, token, signal } = {}, fetcher = globalThis.fetch) {
    const headers = { Accept: 'application/json' };
    if (method !== 'GET') {
        if (!token) throw new ApiError(403, 'Recarga la página para renovar la sesión antes de guardar.');
        headers['Content-Type'] = 'application/json';
        headers['X-CSRF-TOKEN'] = token;
    }
    let response;
    try {
        response = await fetcher(url, { method, body, headers, credentials: 'same-origin', signal, cache: 'no-store' });
    } catch (error) {
        if (error.name === 'AbortError') throw error;
        throw new ApiError(0, 'No se pudo conectar. Comprueba tu conexión y vuelve a cargar el listado.');
    }
    if (!response.ok) {
        const messages = {
            400: 'Revisa los campos: nombre, descripción, categoría y precio deben ser válidos.',
            401: 'Tu sesión ha terminado. Vuelve a iniciar sesión.',
            403: 'La sesión o el permiso no son válidos. Vuelve a iniciar sesión.',
            404: 'El producto ya no está disponible. Actualiza el listado.'
        };
        throw new ApiError(response.status, messages[response.status] || 'No se pudo completar la solicitud. Inténtalo más tarde.');
    }
    if (response.status === 204) return null;
    if (!response.headers.get('Content-Type')?.includes('application/json')) {
        throw new ApiError(0, 'La respuesta no es válida. Recarga la página.');
    }
    try {
        return parseCatalogJson(await response.text());
    } catch {
        throw new ApiError(0, 'No se pudo leer la respuesta. Actualiza el listado antes de continuar.');
    }
}

export function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}
