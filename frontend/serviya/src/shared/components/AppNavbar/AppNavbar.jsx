import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '../Icon/Icon';
import { Avatar } from '../Avatar/Avatar';
import { profileApi, getApiImageUrl } from '../../api';

/** Iniciales (máx. 2) a partir del nombre completo, para el avatar de texto. */
function initialsOf(name) {
    if (!name) return null;
    return name.split(' ').filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');
}

export function AppNavbar({ avatar = 'JP', avatarSrc: controlledAvatarSrc = null, links = [], showBell = true, unreadCount = 0 }) {
    // Foto e iniciales del perfil autenticado. Se piden una vez al montar; el setState solo
    // ocurre en el callback async (no de forma síncrona dentro del effect).
    const [fetchedAvatarSrc, setFetchedAvatarSrc] = useState(null);
    // Iniciales reales del usuario (fallback del avatar cuando no hay foto), así el "JP"/"CM"
    // hardcodeado de las páginas deja de verse.
    const [resolvedInitials, setResolvedInitials] = useState(avatar);

    useEffect(() => {
        let cancelled = false;
        profileApi.getMyProfile()
            .then((profile) => {
                if (cancelled) return;
                const initials = initialsOf(profile?.fullName || profile?.name);
                if (initials) setResolvedInitials(initials);
                setFetchedAvatarSrc(getApiImageUrl(profile?.profilePhotoUrl || null));
            })
            .catch(() => {});

        return () => {
            cancelled = true;
        };
    }, []);

    // El src controlado (p. ej. tras subir una foto en el perfil) tiene prioridad; si no,
    // se usa la foto del perfil recién cargada.
    const resolvedAvatarSrc = getApiImageUrl(controlledAvatarSrc) || fetchedAvatarSrc;

    return (
        <nav className="nav">
            <Link to="/" className="nav-logo">
                <img src="/logo.svg" alt="ServiYa" style={{ height: '24px' }} />
            </Link>
            <div className="nav-spacer" />
            {links.length > 0 && (
                <div className="nav-links">
                    {links.map((l) => (
                        <Link key={l.to} to={l.to}>{l.label}</Link>
                    ))}
                </div>
            )}
            <div className="nav-actions">
                {showBell && (
                    <Link to="/notifications" className="nav-icon" aria-label="Notificaciones">
                        <Icon name="bell" size={16} />
                        {unreadCount > 0 && <span className="nav-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
                    </Link>
                )}
                <Link to="/profile" className="nav-av">
                    <Avatar src={resolvedAvatarSrc} initials={resolvedInitials} />
                </Link>
            </div>
        </nav>
    );
}
