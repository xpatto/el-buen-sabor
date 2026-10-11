import test from 'node:test';
import assert from 'node:assert/strict';
import { parseCatalogJson, normalizePrice, formatPrice, productBody, requestJson, ApiError } from '../../main/resources/static/js/catalog.js';

test('JSON conserva centavos e identificadores fuera del rango seguro de Number', () => {
    const data = parseCatalogJson('[{"id":9223372036854775807,"precio":92233720368547758.07,"activo":false,"nombre":"12.34 y \\"precio\\":1.23"}]');
    assert.equal(data[0].id, '9223372036854775807');
    assert.equal(data[0].precio, '92233720368547758.07');
    assert.equal(data[0].activo, false);
    assert.equal(data[0].nombre, '12.34 y "precio":1.23');
});
test('normaliza únicamente precios válidos sin redondear', () => {
    for (const [input, expected] of [['1','1.00'], ['0.01','0.01'], ['15.5','15.50'], ['92233720368547758.07','92233720368547758.07']]) {
        assert.equal(normalizePrice(input), expected);
    }
});
for (const value of ['', ' ', '0', '-1', '1.001', '1.000', '1,50', '1e2', 'NaN', 'Infinity', '92233720368547758.08', '1;alert(1)', '01.20']) {
    test('rechaza precio inválido ' + JSON.stringify(value), () => assert.throws(() => normalizePrice(value)));
}
test('formato MXN exacto incluso para el máximo', () => {
    assert.equal(formatPrice('92233720368547758.07'), '$92,233,720,368,547,758.07 MXN');
    assert.equal(formatPrice('0.01'), '$0.01 MXN');
});
test('serializa números exactos y escapa textos sin cambiar el contrato', () => {
    const body = productBody({nombre:'<img onerror="x">',descripcion:'D\n"',precio:'92233720368547758.07',categoriaId:'9223372036854775807'},
        [{id:'9223372036854775807'}]);
    assert.match(body, /"precio":92233720368547758.07/);
    assert.match(body, /"categoriaId":9223372036854775807/);
    assert.equal(parseCatalogJson(body).nombre, '<img onerror="x">');
    assert.equal(parseCatalogJson(body).descripcion, 'D\n"');
    assert.equal(Object.hasOwn(parseCatalogJson(body), 'activo'), false);
});
for (const change of [{nombre:' '}, {nombre:'N'.repeat(121)}, {descripcion:''}, {descripcion:'D'.repeat(2001)},
    {categoriaId:''}, {categoriaId:'2'}, {categoriaId:'1.9'}, {categoriaId:'1,"activo":false'}]) {
    test('validación de formulario ' + Object.keys(change)[0] + ':' + String(Object.values(change)[0]).slice(0,12), () => {
        assert.throws(() => productBody({nombre:'N',descripcion:'D',precio:'1.00',categoriaId:'1',...change}, [{id:'1'}]));
    });
}
test('acepta los límites de texto', () => {
    assert.doesNotThrow(() => productBody({nombre:'N'.repeat(120),descripcion:'D'.repeat(2000),precio:'1',categoriaId:'1'}, [{id:'1'}]));
});
test('fetch conserva cookies del mismo origen y manda CSRF solo en escritura', async () => {
    const calls = [];
    const fetcher = async (url, options) => {
        calls.push({url, options});
        return new Response('{"precio":92233720368547758.07}', {headers:{'Content-Type':'application/json'}});
    };
    const result = await requestJson('/api/admin/productos', {method:'POST',body:'{}',token:'token-ficticio'}, fetcher);
    assert.equal(result.precio,'92233720368547758.07');
    assert.equal(calls[0].options.credentials,'same-origin');
    assert.equal(calls[0].options.headers['X-CSRF-TOKEN'],'token-ficticio');
    assert.equal(calls[0].options.headers['Content-Type'],'application/json');
    await requestJson('/api/productos', {}, fetcher);
    assert.equal(calls[1].options.headers['X-CSRF-TOKEN'],undefined);
});
for (const status of [400,401,403,404,500]) {
    test('error HTTP controlado ' + status, async () => {
        await assert.rejects(requestJson('/api/admin/productos', {}, async () => new Response('{"mensaje":"SQL secreto"}', {status})),
            error => error instanceof ApiError && error.status === status && !error.message.includes('SQL'));
    });
}
test('no escribe sin token ni repite una escritura tras error de red', async () => {
    let calls=0;
    const fetcher=async () => {calls++; throw new TypeError('offline');};
    await assert.rejects(requestJson('/api/admin/productos', {method:'POST',body:'{}'}, fetcher));
    assert.equal(calls,0);
    await assert.rejects(requestJson('/api/admin/productos', {method:'POST',body:'{}',token:'ficticio'}, fetcher));
    assert.equal(calls,1);
});
test('rechaza respuestas no JSON y JSON mal formado', async () => {
    await assert.rejects(requestJson('/api/productos',{},async () => new Response('<html>login</html>')));
    assert.throws(() => parseCatalogJson('{"precio":01.00}'));
});

test('DELETE acepta 204 vacío sin leer JSON y conserva CSRF', async () => {
    let calls=0;
    const result=await requestJson('/api/admin/productos/9223372036854775807',
        {method:'DELETE',token:'ficticio'},async (url, options)=>{
            calls++;
            assert.equal(url,'/api/admin/productos/9223372036854775807');
            assert.equal(options.method,'DELETE');
            assert.equal(options.credentials,'same-origin');
            assert.equal(options.headers['X-CSRF-TOKEN'],'ficticio');
            return {ok:true,status:204,headers:new Headers(),text:()=>{throw new Error('No debe leer cuerpo');}};
        });
    assert.equal(result,null);
    assert.equal(calls,1);
});
