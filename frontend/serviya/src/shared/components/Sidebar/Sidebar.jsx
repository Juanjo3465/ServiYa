import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Icon } from '../Icon/Icon';
import { clearToken } from '../../api';

/**
 * Config-driven dashboard sidebar.
 *
 * `sections` is an array of { title, items } where each item is
 * { label, to, icon, badge, badgeColor, end }. A "Cerrar sesión" link to
 * /login is always pinned to the bottom.
 */
export function Sidebar({ sections = [] }) {
    const navigate = useNavigate();

    const handleLogout = () => {
        clearToken();          // invalida la sesión: elimina el JWT de localStorage
        navigate('/login');
    };

    return (
        <aside className="sidebar">
            {sections.map((section, i) => (
                <React.Fragment key={i}>
                    {section.title && <div className="sb-sep">{section.title}</div>}
                    {section.items.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.end}
                            className={({ isActive }) => `sb-item ${isActive ? 'active' : ''}`}
                        >
                            <Icon name={item.icon} size={16} />
                            {item.label}
                            {item.badge != null && (
                                <span className="sb-badge" style={item.badgeColor ? { background: item.badgeColor } : undefined}>
                                    {item.badge}
                                </span>
                            )}
                        </NavLink>
                    ))}
                </React.Fragment>
            ))}
            <div className="sb-spacer" />
            <button
                type="button"
                className="sb-item sb-danger"
                onClick={handleLogout}
                style={{ width: '100%', textAlign: 'left', background: 'none', border: 'none', font: 'inherit', cursor: 'pointer' }}
            >
                <Icon name="logout" size={16} />
                Cerrar sesión
            </button>
        </aside>
    );
}
