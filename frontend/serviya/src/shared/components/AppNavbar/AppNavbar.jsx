import { Link } from 'react-router-dom';
import { Icon } from '../Icon/Icon';

/**
 * Top navigation bar for authenticated areas: logo, optional center links,
 * a notifications bell and the user avatar.
 */
export function AppNavbar({ avatar = 'JP', links = [], showBell = true, hasUnread = true }) {
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
                        {hasUnread && <span className="nav-dot" />}
                    </Link>
                )}
                <Link to="/profile" className="nav-av">{avatar}</Link>
            </div>
        </nav>
    );
}
