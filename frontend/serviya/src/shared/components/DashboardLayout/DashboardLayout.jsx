import { useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AppNavbar } from '../AppNavbar/AppNavbar';
import { Sidebar } from '../Sidebar/Sidebar';
import { notificationApi, chatApi } from '../../api';
import { useToast } from '../../hooks/useToast';

export function DashboardLayout({ sections, avatar, avatarSrc, navLinks = [], children }) {
    const { showToast } = useToast();

    // M49: notificaciones sin leer como query cacheada + polling (refetchInterval) en lugar de un
    // setInterval manual. Al compartir queryKey, todas las páginas del shell ven el mismo contador sin
    // duplicar temporizadores.
    const unreadQuery = useQuery({
        queryKey: ['unread-notifications'],
        queryFn: async () => (await notificationApi.getNotifications({ read: false, size: 1 })).totalElements ?? 0,
        refetchInterval: 30000,
    });
    const unreadCount = unreadQuery.data ?? 0;

    // Aviso (toast) cuando SUBE el número de no leídas. Efecto sin setState propio (showToast es externo).
    const prevCountRef = useRef(0);
    useEffect(() => {
        const total = unreadQuery.data;
        if (total == null) return;
        if (total > prevCountRef.current) {
            const diff = total - prevCountRef.current;
            showToast(
                `Tienes ${diff} notificación${diff > 1 ? 'es' : ''} nueva${diff > 1 ? 's' : ''}`,
                'info',
                '/notifications',
            );
        }
        prevCountRef.current = total;
    }, [unreadQuery.data, showToast]);

    // M49: mensajes de chat sin leer, solo si el menú tiene entrada «Mensajes». Si el chat responde 401
    // (aún sin el emisor de tokens que acepta), refetchInterval devuelve false y deja de consultar en vez
    // de insistir cada 30 s.
    const hasChatEntry = sections.some((s) => s.items.some((it) => it.to === '/messages'));
    const chatUnreadQuery = useQuery({
        queryKey: ['unread-chat'],
        queryFn: async () => {
            const page = await chatApi.listConversations({ limit: 50 });
            return (page.items || []).reduce((n, c) => n + (c.unread_count || 0), 0);
        },
        enabled: hasChatEntry,
        retry: false,
        refetchInterval: (query) => (query.state.error?.status === 401 ? false : 30000),
    });
    const chatUnread = chatUnreadQuery.data ?? 0;

    const sectionsWithBadges = chatUnread > 0
        ? sections.map((s) => ({
            ...s,
            items: s.items.map((it) => (it.to === '/messages' ? { ...it, badge: chatUnread > 99 ? '99+' : chatUnread } : it)),
        }))
        : sections;

    return (
        <>
            <AppNavbar avatar={avatar} avatarSrc={avatarSrc} links={navLinks} unreadCount={unreadCount} showBurger />
            <div className="layout">
                <Sidebar sections={sectionsWithBadges} />
                <main className="main">{children}</main>
            </div>
        </>
    );
}
