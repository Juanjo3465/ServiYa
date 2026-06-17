import { AppNavbar } from '../AppNavbar/AppNavbar';
import { Sidebar } from '../Sidebar/Sidebar';

/**
 * Standard authenticated page shell: top nav + role sidebar + main content.
 * Pass the role's sidebar `sections` and the avatar initials.
 */
export function DashboardLayout({ sections, avatar, navLinks = [], children }) {
    return (
        <>
            <AppNavbar avatar={avatar} links={navLinks} />
            <div className="layout">
                <Sidebar sections={sections} />
                <main className="main">{children}</main>
            </div>
        </>
    );
}
