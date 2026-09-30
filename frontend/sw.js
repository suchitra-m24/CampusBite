self.addEventListener('push', (event) => {
  let data = { title: 'CampusBite', body: 'You have an update on your order.', url: '/student/orders.html' };
  try {
    if (event.data) data = event.data.json();
  } catch (e) { /* fall back to default text above */ }

  event.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      data: { url: data.url },
      tag: 'campusbite-order-update',
      renotify: true,
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const targetUrl = (event.notification.data && event.notification.data.url) || '/student/orders.html';

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((windowClients) => {
      for (const client of windowClients) {
        if (client.url.includes(targetUrl) && 'focus' in client) return client.focus();
      }
      if (clients.openWindow) return clients.openWindow(targetUrl);
    })
  );
});