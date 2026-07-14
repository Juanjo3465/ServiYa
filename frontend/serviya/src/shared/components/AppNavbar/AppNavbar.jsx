import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '../Icon/Icon';
import { profileApi, getApiImageUrl } from '../../api';

export function AppNavbar({ avatar = 'JP', avatarSrc: controlledAvatarSrc = null, links = [], showBell = true, unreadCount = 0 }) {
    const [resolvedAvatarSrc, setResolvedAvatarSrc] = useState(() => getApiImageUrl(controlledAvatarSrc));

    useEffect(() => {
        const nextSrc = getApiImageUrl(controlledAvatarSrc);
        setResolvedAvatarSrc(nextSrc);

        if (controlledAvatarSrc) {
            return;
        }

        let cancelled = false;
        profileApi.getMyProfile()
            .then((profile) => {
                if (!cancelled) {
                    const profileSrc = getApiImageUrl(profile?.profilePhotoUrl || null);
                    setResolvedAvatarSrc(profileSrc || nextSrc);
                }
            })
            .catch(() => {
                if (!cancelled) setResolvedAvatarSrc(nextSrc);
            });

        return () => {
            cancelled = true;
        };
    }, [controlledAvatarSrc]);

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
                    {resolvedAvatarSrc ? <img src={resolvedAvatarSrc} alt="Foto de perfil" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} /> : avatar}
                </Link>
            </div>
        </nav>
    );
}
