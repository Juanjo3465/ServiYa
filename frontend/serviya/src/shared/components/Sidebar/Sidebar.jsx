import React from 'react';
import { NavLink } from 'react-router-dom';
import { Icon } from '../Icon/Icon';

/**
 * Config-driven dashboard sidebar.
 *
 * `sections` is an array of { title, items } where each item is
 * { label, to, icon, badge, badgeColor, end }. A "Cerrar sesión" link to
 * /login is always pinned to the bottom.
 */
export function Sidebar({ sections = [] }) {
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
            <NavLink to="/login" className="sb-item sb-danger">
                <Icon name="logout" size={16} />
                Cerrar sesión
            </NavLink>
        </aside>
    );
}
