// Configuración del icono por defecto de Leaflet.
//
// Problema: Leaflet resuelve las URLs de sus iconos (marker-icon.png, etc.) de forma relativa a su CSS,
// lo que NO funciona bajo el bundler de Vite → el marcador sale como "imagen rota". Aquí se reconfigura
// el icono por defecto apuntando a las imágenes empaquetadas (Vite las resuelve a URLs reales en el build).
//
// Uso: basta con `import '.../shared/leafletIcon';` (efecto secundario) una vez en cada página con mapa.
import L from 'leaflet';
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png';
import iconUrl from 'leaflet/dist/images/marker-icon.png';
import shadowUrl from 'leaflet/dist/images/marker-shadow.png';

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({ iconRetinaUrl, iconUrl, shadowUrl });
