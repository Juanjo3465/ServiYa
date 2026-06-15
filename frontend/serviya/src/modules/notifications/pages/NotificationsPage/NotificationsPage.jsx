import { useState } from "react";
import { DashboardLayout, Icon, ToastContainer, useToast, CLIENT_NAV } from '../../../../shared';

const TABS = ['Todas (12)', 'Sin leer (5)', 'Leídas'];

const NOTIFS = [
    { icon: 'check', cls: 'success', title: 'Solicitud aceptada', msg: 'María L. aceptó tu solicitud de "Limpieza de hogar" para el 14 de mayo a las 10:00 AM.', time: 'Hace 20 minutos · Sistema interno y correo', unread: true },
    { icon: 'reschedule', cls: 'warn', title: 'Propuesta de reprogramación', msg: 'Carlos M. propone cambiar tu servicio de plomería al martes 13 de mayo a las 10am. Motivo: compromiso previo.', time: 'Hace 1 hora · Sistema interno y correo', unread: true },
    { icon: 'check', cls: 'success', title: 'Servicio marcado como realizado', msg: 'Carlos M. marcó el servicio de reparación de tuberías como completado. Confirma si fue así para calificar.', time: 'Ayer · Sistema interno y correo', unread: true },
    { icon: 'check', cls: 'success', title: 'Solicitud aceptada', msg: 'Ana R. aceptó tu solicitud de instalación eléctrica para el 16 de mayo a las 2:00 PM.', time: 'Ayer · Sistema interno y correo', unread: true },
    { icon: 'close', cls: 'danger', title: 'Solicitud rechazada', msg: 'Pedro G. rechazó tu solicitud de jardinería. Busca otro oferente disponible.', time: 'Hace 2 días · Sistema interno y correo', unread: true },
    { icon: 'close', cls: 'danger', title: 'Servicio cancelado por el oferente', msg: 'Luis R. canceló el servicio de instalación de grifos del 8 de mayo. Puedes buscar otro oferente.', time: 'Hace 3 días · Sistema interno y correo', unread: false },
    { icon: 'alertTriangle', cls: 'danger', title: 'Reporte en tu contra', msg: 'Se ha recibido un reporte relacionado con tu solicitud #SR-4200. El administrador revisará el caso.', time: 'Hace 5 días · Sistema interno y correo', unread: false },
    { icon: 'tasks', cls: '', title: 'Solicitud enviada', msg: 'Tu solicitud de reparación de tuberías con Carlos M. fue enviada correctamente. Estado: Pendiente.', time: 'Hace 1 semana', unread: false },
];

export function NotificationsPage() {
    const { toasts, showToast } = useToast();
    const [tab, setTab] = useState(0);

    const visible = NOTIFS.filter((n) => (tab === 1 ? n.unread : tab === 2 ? !n.unread : true));

    return (
        <DashboardLayout sections={CLIENT_NAV} avatar="JP">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '8px' }}>
                <div className="ph" style={{ margin: 0 }}><h1>Notificaciones</h1><p>Todas tus notificaciones del sistema y correo</p></div>
                <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={() => showToast('Todas marcadas como leídas', 'success')}><Icon name="check" size={13} />Marcar todas como leídas</button>
            </div>

            <div className="tabs">
                {TABS.map((t, i) => (
                    <div key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => setTab(i)}>{t}</div>
                ))}
            </div>

            {visible.map((n, i) => (
                <div className={`notif-item ${n.unread ? 'unread' : 'read'}`} key={i}>
                    <div className={`notif-ico ${n.cls}`}><Icon name={n.icon} size={16} /></div>
                    <div className="notif-body"><div className="notif-title">{n.title}</div><div className="notif-msg">{n.msg}</div><div className="notif-time">{n.time}</div></div>
                </div>
            ))}
            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default NotificationsPage;
