import React from 'react';
import { Link } from 'react-router-dom';
import './AdminNavbar.css';

export function AdminNavbar() {
    return (
        <nav className="admin-nav">
            <Link
                to="/"
                className="nav-logo">
                <img
                    src="/logo.svg"
                    alt="ServiYa"
                    style={{ height: '24px' }} />
            </Link>
            <div className="nav-spacer"></div>
            <div style={{ display: 'flex', alignItems: 'center' }}>
                <span className="admin-badge">ADMIN</span>
            </div>
            <div className="nav-actions">
                <div className="nav-av" style={{ background: '#0F172A' }}>AD</div>
            </div>
        </nav>
    );
}