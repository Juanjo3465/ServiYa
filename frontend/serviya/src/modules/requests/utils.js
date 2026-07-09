export const STATUS_MAP = {
    PENDING:              { label: 'Pendiente',             badge: 'badge-warn' },
    ACCEPTED:             { label: 'Aceptada',              badge: 'badge-success' },
    PRESUMABLY_COMPLETED: { label: 'Por confirmar',        badge: 'badge-primary' },
    REJECTED:             { label: 'Rechazada',             badge: 'badge-danger' },
    CANCELLED:            { label: 'Cancelada',             badge: 'badge-danger' },
    COMPLETED:            { label: 'Completada',            badge: 'badge-success' },
    RESCHEDULED:          { label: 'Reprogramada',          badge: 'badge-warn' },
    NOT_PROVIDED:         { label: 'No prestado',           badge: 'badge-danger' },
};

const DAYS = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
const MONTHS = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

export function formatDate(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    const dayName = DAYS[d.getDay()];
    const day = d.getDate();
    const month = MONTHS[d.getMonth()];
    const hours = d.getHours();
    const minutes = d.getMinutes().toString().padStart(2, '0');
    const ampm = hours >= 12 ? 'PM' : 'AM';
    const h12 = hours % 12 || 12;
    return `${dayName} ${day} ${month}, ${h12}:${minutes} ${ampm}`;
}

export function formatShortDate(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return `${d.getDate()} ${MONTHS[d.getMonth()]}`;
}

export function timeAgo(iso) {
    if (!iso) return '';
    const diff = Date.now() - new Date(iso).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return mins <= 1 ? 'Hace 1 min' : `Hace ${mins} min`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs === 1 ? 'Hace 1 hora' : `Hace ${hrs} horas`;
    const days = Math.floor(hrs / 24);
    if (days < 30) return days === 1 ? 'Hace 1 día' : `Hace ${days} días`;
    return '';
}

export function getInitials(name) {
    if (!name) return '?';
    return name.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase();
}

export function formatPrice(price) {
    if (price == null) return '';
    return '$' + Number(price).toLocaleString('es-CO');
}

const ICON_MAP = {
    'plomería': 'wrench',
    'fontanería': 'wrench',
    'limpieza': 'home',
    'electricidad': 'bolt',
    'jardinería': 'leaf',
    'pintura': 'paintbrush',
    'carpintería': 'hammer',
    'mecánica': 'tool',
    'reparación': 'wrench',
    'clases': 'bookOpen',
    'fotografía': 'camera',
    'chef': 'coffee',
    'entrenamiento': 'heart',
    'diseño': 'palette',
    'traducción': 'globe',
    'maquillaje': 'eye',
    'programación': 'code',
    'eventos': 'calendar',
    'asistencia': 'headphones',
    'diseño interior': 'layout',
    'yoga': 'heart',
};

export function categoryIcon(categoryName) {
    if (!categoryName) return 'fileText';
    const name = categoryName.toLowerCase();
    for (const [key, icon] of Object.entries(ICON_MAP)) {
        if (name.includes(key)) return icon;
    }
    return 'fileText';
}
