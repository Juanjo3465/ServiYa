import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { DashboardLayout, Icon, ToastContainer, useToast, navForActiveRole } from '../../../../shared';
import { notificationApi } from '../../../../shared/api';

const TABS = ['Todas', 'Sin leer', 'Leídas'];

const TYPE_META = {
    new_request:       { icon: 'tasks',     cls: '' },
    request_accepted:  { icon: 'check',     cls: 'success' },
    request_rejected:  { icon: 'close',     cls: 'danger' },
    request_cancelled: { icon: 'close',     cls: 'danger' },
    service_completed: { icon: 'check',     cls: 'success' },
    reschedule_proposed: { icon: 'reschedule', cls: 'warn' },
    request_rescheduled: { icon: 'reschedule', cls: 'warn' },
};

function metaForType(type) {
    return TYPE_META[type] || { icon: 'bell', cls: '' };
}

function timeAgo(dateStr) {
    if (!dateStr) return '';
    const hasTz = /[+-]\d{2}:\d{2}$/.test(dateStr) || dateStr.endsWith('Z');
    const date = new Date(hasTz ? dateStr : dateStr + 'Z');
    const diffMs = Date.now() - date.getTime();
    const mins = Math.floor(diffMs / 60000);
    if (mins < 1) return 'Ahora';
    if (mins < 60) return mins === 1 ? 'Hace 1 minuto' : `Hace ${mins} minutos`;
    const horas = Math.floor(mins / 60);
    if (horas < 24) return horas === 1 ? 'Hace 1 hora' : `Hace ${horas} horas`;
    return date.toLocaleDateString('es-CO', {
        day: 'numeric', month: 'short', year: 'numeric'
    });
}

export function NotificationsPage() {
    const { toasts, showToast } = useToast();
    const [tab, setTab] = useState(0);
    const [pageNum, setPageNum] = useState(0);
    const queryClient = useQueryClient();

    // M49: notificaciones por pestaña+página como query cacheada (antes fetch-en-efecto).
    const notifsQuery = useQuery({
        queryKey: ['notifications', tab, pageNum],
        queryFn: () => {
            const params = { page: pageNum, size: 20 };
            if (tab === 1) params.read = false;
            if (tab === 2) params.read = true;
            return notificationApi.getNotifications(params);
        },
    });
    const page = notifsQuery.data ?? null;
    const loading = notifsQuery.isPending;
    const notifications = page?.content || [];

    // Marcar una como leída: optimista en la pestaña actual + invalida el contador del navbar.
    const markAsReadMutation = useMutation({
        mutationFn: (deliveryId) => notificationApi.markAsRead(deliveryId),
        onMutate: async (deliveryId) => {
            await queryClient.cancelQueries({ queryKey: ['notifications', tab, pageNum] });
            const snapshot = queryClient.getQueryData(['notifications', tab, pageNum]);
            queryClient.setQueryData(['notifications', tab, pageNum], (old) => (old?.content ? {
                ...old,
                content: old.content.map((n) => (n.deliveryId === deliveryId
                    ? { ...n, readAt: new Date().toISOString(), deliveryStatus: 'READ' } : n)),
            } : old));
            return { snapshot };
        },
        onError: (err, _id, ctx) => {
            if (ctx && ctx.snapshot !== undefined) queryClient.setQueryData(['notifications', tab, pageNum], ctx.snapshot);
            showToast('Error al marcar como leída', 'danger');
        },
        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
            queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
        },
    });
    const handleMarkAsRead = (deliveryId) => markAsReadMutation.mutate(deliveryId);

    const markAllMutation = useMutation({
        mutationFn: (ids) => Promise.all(ids.map((id) => notificationApi.markAsRead(id))),
        onSuccess: () => {
            showToast('Todas marcadas como leídas', 'success');
            queryClient.invalidateQueries({ queryKey: ['notifications'] });
            queryClient.invalidateQueries({ queryKey: ['unread-notifications'] });
        },
        onError: () => showToast('Error al marcar como leídas', 'danger'),
    });
    const handleMarkAllAsRead = () => {
        const unread = page?.content?.filter((n) => !n.readAt) || [];
        if (unread.length === 0) { showToast('No hay notificaciones sin leer', 'info'); return; }
        markAllMutation.mutate(unread.map((n) => n.deliveryId));
    };

    return (
        <DashboardLayout sections={navForActiveRole()} avatar="JP">
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', flexWrap: 'wrap', gap: '8px' }}>
                <div className="ph" style={{ margin: 0 }}>
                    <h1>Notificaciones</h1>
                    <p>{page ? `${page.totalElements} notificaciones` : 'Cargando...'}</p>
                </div>
                <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }} onClick={handleMarkAllAsRead}>
                    <Icon name="check" size={13} />Marcar todas como leídas
                </button>
            </div>

            <div className="tabs">
                {TABS.map((t, i) => (
                    <div key={t} className={`tab ${tab === i ? 'active' : ''}`} onClick={() => { setTab(i); setPageNum(0); }}>{t}</div>
                ))}
            </div>

            {loading ? (
                <p style={{ textAlign: 'center', color: 'var(--c-mid)', marginTop: '40px' }}>Cargando...</p>
            ) : notifications.length === 0 ? (
                <p style={{ textAlign: 'center', color: 'var(--c-mid)', marginTop: '40px' }}>No hay notificaciones</p>
            ) : (
                notifications.map((n) => {
                    const meta = metaForType(n.notificationType);
                    const isUnread = !n.readAt;
                    return (
                        <div
                            className={`notif-item ${isUnread ? 'unread' : 'read'}`}
                            key={n.deliveryId}
                            onClick={() => isUnread && handleMarkAsRead(n.deliveryId)}
                        >
                            <div className={`notif-ico ${meta.cls}`}>
                                <Icon name={meta.icon} size={16} />
                            </div>
                            <div className="notif-body">
                                <div className="notif-title">{n.title}</div>
                                <div className="notif-msg">{n.message}</div>
                                <div className="notif-time">{timeAgo(n.createdAt)}</div>
                            </div>
                        </div>
                    );
                })
            )}

            {page && page.totalPages > 1 && (
                <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '20px' }}>
                    {Array.from({ length: page.totalPages }, (_, i) => (
                        <button
                            key={i}
                            className={`btn btn-sm ${i === (page?.number ?? pageNum) ? 'btn-primary' : 'btn-ghost'}`}
                            onClick={() => setPageNum(i)}
                        >
                            {i + 1}
                        </button>
                    ))}
                </div>
            )}

            <ToastContainer toasts={toasts} />
        </DashboardLayout>
    );
}

export default NotificationsPage;
