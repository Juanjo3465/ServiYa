import { useState } from "react";
import { useNavigate } from 'react-router-dom';
import { AppNavbar, Icon, Stars, ToastContainer, useToast } from '../../../../shared';

import './SearchPage.css';

const CATEGORIES = ['Todas', 'Plomería', 'Electricidad', 'Limpieza', 'Jardinería', 'Pintura', 'Carpintería'];

const RESULTS = [
    { id: 1, name: 'Reparación de tuberías', offerer: 'Carlos M.', initials: 'CM', rating: 4.9, count: 32, price: '$50k', distance: '2.3 km', avail: 'Hoy' },
    { id: 2, name: 'Instalación de grifos', offerer: 'Luis R.', initials: 'LR', rating: 4.6, count: 18, price: '$70k', distance: '1.1 km', avail: 'Hoy' },
    { id: 3, name: 'Destape de cañerías', offerer: 'Pedro G.', initials: 'PG', rating: 5.0, count: 9, price: '$35k', distance: '3.8 km', avail: 'Mañana' },
    { id: 4, name: 'Reparación calentador', offerer: 'Miguel H.', initials: 'MH', rating: 4.3, count: 14, price: '$90k', distance: '4.2 km', avail: 'Hoy' },
    { id: 5, name: 'Limpieza tanque de agua', offerer: 'Juan P.', initials: 'JP', rating: 4.8, count: 22, price: '$120k', distance: '0.8 km', avail: 'Hoy' },
    { id: 6, name: 'Plomería general', offerer: 'Sandra R.', initials: 'SR', rating: 4.7, count: 31, price: '$80k', distance: '2.9 km', avail: 'Semana' },
];

const availBadge = (a) => (a === 'Hoy' ? 'badge-success' : 'badge-warn');

export function SearchPage() {
    const navigate = useNavigate();
    const { toasts, showToast } = useToast();
    const [activeCat, setActiveCat] = useState('Todas');
    const [starFilter, setStarFilter] = useState('4★+');
    const [activeFilters, setActiveFilters] = useState(['Plomería', '4★+', 'Disponible hoy', 'Menos de 5km', 'Persona natural']);

    const removeFilter = (f) => setActiveFilters((prev) => prev.filter((x) => x !== f));

    return (
        <>
            <AppNavbar avatar="JP" links={[{ to: '/services', label: 'Servicios' }]} />

            <div className="search-hd">
                <div className="search-bar">
                    <Icon name="search" size={16} style={{ color: 'var(--c-soft)' }} />
                    <input placeholder="Buscar servicios..." defaultValue="Plomería" />
                    <button className="btn btn-primary btn-sm" onClick={() => showToast('Búsqueda actualizada', 'success')}><Icon name="search" size={13} />Buscar</button>
                </div>
                <div className="search-chips">
                    {CATEGORIES.map((c) => (
                        <span key={c} className={`chip ${activeCat === c ? 'active' : ''}`} onClick={() => setActiveCat(c)}>{c}</span>
                    ))}
                </div>
            </div>

            <div className="search-layout">
                <aside className="filters">
                    <div className="filter-hd"><span>Filtros</span><span className="filter-clear" onClick={() => { setActiveFilters([]); showToast('Filtros limpiados', 'info'); }}>Limpiar todo</span></div>

                    <div className="filter-sec">
                        <div className="filter-sec-title">Categoría</div>
                        {['Plomería (12)', 'Electricidad (8)', 'Limpieza (15)', 'Jardinería (6)', 'Pintura (9)'].map((c, i) => (
                            <label className="filter-opt" key={c}><input type="checkbox" defaultChecked={i === 0} /> {c}</label>
                        ))}
                    </div>
                    <div className="divider" />

                    <div className="filter-sec">
                        <div className="filter-sec-title">Nombre / Oferente</div>
                        <div className="input-wrap" style={{ marginTop: '6px' }}><div className="input-ico"><Icon name="search" size={15} /></div><input className="input" placeholder="Buscar por nombre..." style={{ fontSize: '12px', padding: '8px 10px 8px 34px' }} /></div>
                    </div>
                    <div className="divider" />

                    <div className="filter-sec">
                        <div className="filter-sec-title">Puntuación mínima</div>
                        <div className="star-filter">
                            {['3★+', '4★+', '5★'].map((s) => (
                                <div key={s} className={`star-opt ${starFilter === s ? 'active' : ''}`} onClick={() => setStarFilter(s)}>{s}</div>
                            ))}
                        </div>
                    </div>
                    <div className="divider" />

                    <div className="filter-sec">
                        <div className="filter-sec-title">Precio (por servicio)</div>
                        <div className="price-wrap"><input className="price-in" placeholder="$0" defaultValue="$20.000" /><span style={{ color: 'var(--c-soft)', fontSize: '12px' }}>—</span><input className="price-in" placeholder="$500k" defaultValue="$200.000" /></div>
                    </div>
                    <div className="divider" />

                    <div className="filter-sec">
                        <div className="filter-sec-title">Disponibilidad</div>
                        {['Disponible hoy', 'Esta semana', 'Este mes'].map((c, i) => (
                            <label className="filter-opt" key={c}><input type="checkbox" defaultChecked={i === 0} /> {c}</label>
                        ))}
                    </div>
                    <div className="divider" />

                    <div className="filter-sec">
                        <div className="filter-sec-title">Tipo de oferente</div>
                        {['Persona natural', 'Empresa'].map((c, i) => (
                            <label className="filter-opt" key={c}><input type="checkbox" defaultChecked={i === 0} /> {c}</label>
                        ))}
                    </div>
                    <div className="divider" />

                    <div className="filter-sec">
                        <div className="filter-sec-title">Cercanía</div>
                        <select className="input" style={{ fontSize: '12px', padding: '8px 10px' }}>
                            <option>Menos de 5 km</option><option>Menos de 10 km</option><option>Menos de 20 km</option><option>Cualquier distancia</option>
                        </select>
                    </div>
                    <div className="divider" />

                    <div className="filter-sec">
                        <div className="filter-sec-title">Sector / Zona</div>
                        <select className="input" style={{ fontSize: '12px', padding: '8px 10px' }}>
                            <option>Todos los sectores</option><option>Chapinero</option><option>Usaquén</option><option>Suba</option><option>Kennedy</option>
                        </select>
                    </div>

                    <button className="btn btn-primary btn-full" style={{ marginTop: '14px' }} onClick={() => showToast('Filtros aplicados', 'success')}>
                        <Icon name="filter" size={15} />Aplicar filtros
                    </button>
                </aside>

                <div>
                    <div className="results-hd">
                        <div className="results-count"><strong>24 resultados</strong> para "Plomería"</div>
                        <select className="sort-sel">
                            <option>Más relevantes</option><option>Mayor puntuación</option><option>Menor precio</option><option>Más cercanos</option><option>Disponibilidad</option>
                        </select>
                    </div>

                    <div className="active-filters">
                        {activeFilters.map((f) => (
                            <div className="af" key={f}>{f} <button onClick={() => removeFilter(f)}>×</button></div>
                        ))}
                    </div>

                    <div className="r-cards">
                        {RESULTS.map((s) => (
                            <div className="r-card" key={s.id} onClick={() => navigate(`/services/${s.id}`)}>
                                <div className="r-card-img">
                                    <Icon name="wrench" size={38} strokeWidth={1.5} />
                                    <div className="r-card-av"><span className={`badge ${availBadge(s.avail)}`}>{s.avail}</span></div>
                                </div>
                                <div className="r-card-body">
                                    <div className="r-card-name">{s.name}</div>
                                    <div className="r-oferer"><div className="av av-xs">{s.initials}</div><span>{s.offerer}</span></div>
                                    <div style={{ fontSize: '11px', marginBottom: '8px' }}><Stars rating={s.rating} showValue count={s.count} size={11} /></div>
                                    <div className="r-card-ft">
                                        <span className="r-price">desde {s.price}</span>
                                        <span className="r-loc"><Icon name="mapPin" size={11} />{s.distance}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="pager">
                        <button className="btn btn-primary btn-sm">1</button>
                        <button className="btn btn-ghost btn-sm">2</button>
                        <button className="btn btn-ghost btn-sm">3</button>
                        <button className="btn btn-ghost btn-sm">4</button>
                        <span style={{ padding: '5px 6px', color: 'var(--c-soft)' }}>...</span>
                        <button className="btn btn-ghost btn-sm"><Icon name="chevronRight" size={14} /></button>
                    </div>
                </div>
            </div>
            <ToastContainer toasts={toasts} />
        </>
    );
}

export default SearchPage;
