import { Icon } from '../../../../shared';

import './MonthCalendar.css';

const WEEK = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

// Static May-2025 layout matching the mockup: leading days belong to the
// previous month, day 12 is "today", and 12/14/16 carry events.
const CELLS = [
    { d: 28, other: true }, { d: 29, other: true }, { d: 30, other: true }, { d: 1, other: true }, { d: 2, other: true }, { d: 3, other: true }, { d: 4, other: true },
    { d: 5 }, { d: 6 }, { d: 7 }, { d: 8 }, { d: 9 }, { d: 10 }, { d: 11 },
    { d: 12, today: true, event: true }, { d: 13 }, { d: 14, event: true }, { d: 15 }, { d: 16, event: true }, { d: 17 }, { d: 18 },
    { d: 19 }, { d: 20 }, { d: 21 }, { d: 22 }, { d: 23 }, { d: 24 }, { d: 25 },
];

export function MonthCalendar({ month = 'Mayo 2025' }) {
    return (
        <div>
            <div className="cal-bar">
                <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }}><Icon name="chevronLeft" size={14} /></button>
                <div style={{ fontSize: '15px', fontWeight: 700 }}>{month}</div>
                <button className="btn btn-ghost btn-sm" style={{ border: '1px solid var(--c-border)' }}><Icon name="chevronRight" size={14} /></button>
            </div>
            <div className="cal-grid">
                {WEEK.map((w) => <div className="cal-hd" key={w}>{w}</div>)}
                {CELLS.map((c, i) => (
                    <div key={i} className={`cal-day ${c.today ? 'today' : ''} ${c.event ? 'has-event' : ''} ${c.other ? 'other-month' : ''}`}>{c.d}</div>
                ))}
            </div>
        </div>
    );
}
