import { Outlet } from 'react-router-dom';
import { Header } from './Header';
import { AdminSidebar } from './AdminSidebar';

/**
 * Admin layout: Header + Sidebar + main content area (no footer).
 */
export function AdminLayout() {
  return (
    <div className="flex min-h-screen flex-col">
      <Header />
      <div className="flex flex-1">
        <AdminSidebar />
        <main className="flex-1 overflow-auto p-6 bg-background/50">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
