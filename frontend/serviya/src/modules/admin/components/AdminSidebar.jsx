import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './AdminSidebar.css';

export function AdminSidebar() {
    const location = useLocation();

    return (
        <aside className="sidebar">
            <div className="sb-sep">Administración</div>
            
            <Link to="/admin/dashboard" className={`sb-item ${location.pathname === '/admin/dashboard' ? 'active' : ''}`}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="3" width="7" height="7" /><rect x="14" y="3" width="7" height="7" /><rect x="14" y="14" width="7" height="7" /><rect x="3" y="14" width="7" height="7" />
                </svg>
                Panel
            </Link>

            <Link to="/admin/users" className={`sb-item ${location.pathname === '/admin/users' ? 'active' : ''}`}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
                </svg>
                Usuarios
            </Link>

            <Link to="/admin/reports" className={`sb-item ${location.pathname === '/admin/reports' ? 'active' : ''}`}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="m10.29 3.86-8.5 14.72A1 1 0 0 0 2.68 20h16.64a1 1 0 0 0 .89-1.42l-8.5-14.72a1 1 0 0 0-1.76 0z" /><path d="M12 9v4m0 4h.01" />
                </svg>
                Reportes<span className="sb-badge" style={{ background: 'var(--c-danger)' }}>7</span>
            </Link>

            <Link to="/services" className={`sb-item ${location.pathname === '/services' ? 'active' : ''}`}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
                </svg>
                Servicios
            </Link>

            <div className="sb-spacer"></div>

            <Link to="/login" className="sb-item sb-danger">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><polyline points="16 17 21 12 16 7" /><line x1="21" y1="12" x2="9" y2="12" />
                </svg>
                Cerrar sesión
            </Link>
        </aside>
    );
}