import test from 'node:test';
import assert from 'node:assert/strict';
import { deleteProductWithConfirmation } from '../../main/resources/static/js/admin.js';

const product={id:'9223372036854775807',nombre:'Torta <especial>'};

test('cancelar muestra nombre y advertencia, sin petición ni bloqueo de controles', async()=>{
    let calls=0, busy=0;
    const result=await deleteProductWithConfirmation(product,'ficticio',{
        confirm:message=>{
            assert.ok(message.includes(product.nombre));
            assert.match(message,/no se puede deshacer/);
            return false;
        },
        onConfirmed:()=>busy++,
        request:async()=>calls++
    });
    assert.equal(result,false);
    assert.equal(calls,0);
    assert.equal(busy,0);
});
test('confirmar bloquea controles antes de enviar DELETE con ID exacto y token', async()=>{
    const events=[];
    assert.equal(await deleteProductWithConfirmation(product,'ficticio',{
        confirm:()=>true,
        onConfirmed:()=>events.push('busy'),
        request:async(url,options)=>{
            events.push('request');
            assert.equal(url,'/api/admin/productos/9223372036854775807');
            assert.deepEqual(options,{method:'DELETE',token:'ficticio'});
            return null;
        }
    }),true);
    assert.deepEqual(events,['busy','request']);
});
test('error de red se propaga sin repetir DELETE',async()=>{
    let calls=0;
    await assert.rejects(deleteProductWithConfirmation(product,'ficticio',{
        confirm:()=>true,
        request:async()=>{calls++;throw new Error('offline');}
    }),/offline/);
    assert.equal(calls,1);
});
