// v1.2: desativa o cache antigo para evitar HTML desatualizado.
self.addEventListener('install', event => { self.skipWaiting(); });
self.addEventListener('activate', event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k=>k.startsWith('je-cobrancas-')).map(k=>caches.delete(k)))).then(() => self.registration.unregister()));
});
