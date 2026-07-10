import { useEffect, useState } from 'react';
import { AppNavbar } from '../AppNavbar/AppNavbar';
import { Sidebar } from '../Sidebar/Sidebar';
import { notificationApi } from '../../api';

/**
 * Standard authenticated page shell: top nav + role sidebar + main content.
 * Pass the role's sidebar `sections` and the avatar initials.
 */
export function DashboardLayout({ sections, avatar, navLinks = [], children }) {
    const [hasUnread, setHasUnread] = useState(false);

    useEffect(() => {
        const check = () => {
            notificationApi.getNotifications({ read: false, size: 1 })
                .then(page => setHasUnread(page.totalElements > 0))
                .catch(() => {});
        };
        check();
        const id = setInterval(check, 30000);
        return () => clearInterval(id);
    }, []);

    return (
        <>
            <AppNavbar avatar={avatar} links={navLinks} hasUnread={hasUnread} />
            <div className="layout">
                <Sidebar sections={sections} />
                <main className="main">{children}</main>
            </div>
        </>
    );
}
