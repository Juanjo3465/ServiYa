import { createBrowserRouter } from "react-router-dom";
import App from "./App";

import { HomePage, SearchPage, ServiceDetailPage, OffererServicesPage, OffererServiceDetailPage, AvailabilityPage } from "../modules/services";
import { LoginPage, RegisterPage, RecoverPasswordPage, ResetPasswordPage } from "../modules/users";
import { ProfilePage, OffererProfilePage } from "../modules/profiles";
import {
    ClientDashboardPage,
    OffererDashboardPage,
    ClientRequestsPage,
    ReschedulesPage,
    ClientSchedulePage,
    OffererSchedulePage,
    ClientHistoryPage,
    OffererHistoryPage,
    OffererRequestsPage,
    RequestServicePage,
    RequestDetailPage,
} from "../modules/requests";
import { NotificationsPage } from "../modules/notifications";
import { AdminDashboardPage, AdminUsersPage, AdminReportsPage, AdminFeedbackPage, AdminServicesPage } from "../modules/admin";

const page = (element) => ({ element: <App>{element}</App> });

export const router = createBrowserRouter([
    // Public
    { path: "/", ...page(<HomePage />) },
    { path: "/login", ...page(<LoginPage />) },
    { path: "/register", ...page(<RegisterPage />) },
    { path: "/recover", ...page(<RecoverPasswordPage />) },
    { path: "/reset-password", ...page(<ResetPasswordPage />) },
    { path: "/services", ...page(<SearchPage />) },
    { path: "/services/:id", ...page(<ServiceDetailPage />) },
    { path: "/offerers/:id", ...page(<OffererProfilePage />) },
    { path: "/request-service", ...page(<RequestServicePage />) },

    // Account (shared by roles)
    { path: "/profile", ...page(<ProfilePage />) },
    { path: "/notifications", ...page(<NotificationsPage />) },

    // Client
    { path: "/dashboard", ...page(<ClientDashboardPage />) },
    { path: "/requests", ...page(<ClientRequestsPage />) },
    { path: "/requests/:id", ...page(<RequestDetailPage />) },   // detalle de solicitud (cliente u oferente)
    { path: "/reschedules", ...page(<ReschedulesPage />) },
    { path: "/schedule", ...page(<ClientSchedulePage />) },
    { path: "/history", ...page(<ClientHistoryPage />) },

    // Offerer
    { path: "/offerer/dashboard", ...page(<OffererDashboardPage />) },
    { path: "/offerer/requests", ...page(<OffererRequestsPage />) },
    { path: "/offerer/services", ...page(<OffererServicesPage />) },
    { path: "/offerer/services/:id", ...page(<OffererServiceDetailPage />) },
    { path: "/offerer/availability", ...page(<AvailabilityPage />) },
    { path: "/offerer/schedule", ...page(<OffererSchedulePage />) },
    { path: "/offerer/history", ...page(<OffererHistoryPage />) },

    // Admin
    { path: "/admin/dashboard", ...page(<AdminDashboardPage />) },
    { path: "/admin/users", ...page(<AdminUsersPage />) },
    { path: "/admin/reports", ...page(<AdminReportsPage />) },
    { path: "/admin/feedback", ...page(<AdminFeedbackPage />) },
    { path: "/admin/services", ...page(<AdminServicesPage />) },
]);
