import { useState } from "react";
import { useToast } from '../../../../shared/hooks/useToast';
import { useNavigate } from 'react-router-dom';

import "./Hero.css";

const CATEGORIES = [
    { name: 'Plomería', icon: <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" /> },
    { name: 'Electricidad', icon: <path d="M13 2 3 14h9l-1 8 10-12h-9l1-8z" /> },
    { name: 'Limpieza', icon: <><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" /><polyline points="9 22 9 12 15 12 15 22" /></> },
    { name: 'Jardinería', icon: <path d="M12 22V12m0 0C12 7 7 4 7 4s0 5 5 8zm0 0c0-5 5-8 5-8s0 5-5 8z" /> },
    { name: 'Pintura', icon: <path d="m2 12 10-8 10 8v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2z" /> },
    { name: 'Carpintería', icon: <><rect x="3" y="3" width="18" height="18" rx="2" /><path d="M3 9h18M9 21V9" /></> },
    { name: 'Cerrajería', icon: <><rect x="3" y="11" width="18" height="11" rx="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" /></> },
    { name: 'Aires A/C', icon: <path d="M8 7H5a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3m-1-4-1-4-4 4-4-4-1 4" /> },
    { name: 'Vidrios', icon: <><rect x="2" y="3" width="20" height="14" rx="2" /><path d="M8 21h8M12 17v4" /></> },
    { name: 'Mudanzas', icon: <path d="M5 8h14M5 12h14M5 16h14" /> }
];

export function Hero() {
    const { showToast } = useToast();
    const [activeChip, setActiveChip] = useState('Plomería');
    const [searchQuery, setSearchQuery] = useState('');
    const navigate = useNavigate();

    const handleSearch = () => {
        if (!searchQuery.trim()) {
            showToast('Por favor escribe el servicio que buscas', 'warn');
            return;
        }
        navigate('/services');
    };

    return (
        <section className="hero">
            <h1>Tu hogar merece el<br /><span>mejor servicio</span></h1>
            <p>Conectamos clientes con profesionales verificados para todos los servicios de tu hogar, cerca de ti y cuando lo necesitas.</p>
            <div className="search-box">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--c-soft)" strokeWidth="2" style={{ flexShrink: 0 }}>
                    <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
                </svg>
                <input
                    placeholder="¿Qué servicio necesitas? ej: plomero, electricista..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                />
                <div className="s-div"></div>
                <select>
                    <option>Todas las categorías</option>
                    <option>Plomería</option>
                    <option>Electricidad</option>
                    <option>Limpieza</option>
                    <option>Jardinería</option>
                </select>
                <button className="btn btn-primary" style={{ borderRadius: '100px', padding: '9px 20px', flexShrink: 0 }} onClick={handleSearch}>
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" style={{ width: '14px', marginRight: '4px' }}><circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" /></svg>
                    Buscar
                </button>
            </div>
            <div className="hero-chips">
                {CATEGORIES.map((cat) => (
                    <span
                        key={cat.name}
                        className={`chip ${activeChip === cat.name ? 'active' : ''}`}
                        onClick={() => {
                            setActiveChip(cat.name);
                            showToast(`Filtro rápido: ${cat.name}`, 'info');
                        }}
                    >
                        {cat.name}
                    </span>
                ))}
            </div>
        </section>
    );
}