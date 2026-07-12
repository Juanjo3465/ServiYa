import { Link } from 'react-router-dom';
import { Icon } from '../Icon/Icon';

export function AppNavbar({ avatar = 'JP', links = [], showBell = true, unreadCount = 0 }) {
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
                <Link to="/profile" className="nav-av">{avatar}</Link>
            </div>
        </nav>
    );
}
