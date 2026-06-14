import { createBrowserRouter } from "react-router-dom";
import App from "./App";
import { HomePage } from "../modules/services";
import { AdminReportsPage } from "../modules/admin";

export const router = createBrowserRouter([
    {
        path: "/",
        element: (
            <App>
                <HomePage />
            </App>
        ),
    },
    {
        path: "/admin/reports",
        element: (
            <App>
                <AdminReportsPage />
            </App>
        ),
    },
]);