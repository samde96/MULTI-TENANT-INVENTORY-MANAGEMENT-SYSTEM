import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import {
  BarChart3,
  Boxes,
  Building2,
  FileClock,
  LayoutDashboard,
  LogOut,
  PackageCheck,
  ReceiptText,
  ShieldCheck,
  Truck,
  UserCheck
} from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import { APP_NAV_ITEMS, hasAnyPermission } from '../utils/access'

const NAV_ITEMS = APP_NAV_ITEMS.map((item) => ({
  ...item,
  label: {
    '/app/dashboard': 'Overview',
    '/app/locations': 'Locations',
    '/app/suppliers': 'Suppliers',
    '/app/inventory': 'Inventory',
    '/app/operations': 'Operations',
    '/app/pos': 'POS',
    '/app/approvals': 'Approvals',
    '/app/reports': 'Reports',
    '/app/audit': 'Audit'
  }[item.to],
  icon: {
    '/app/dashboard': LayoutDashboard,
    '/app/locations': Building2,
    '/app/suppliers': Truck,
    '/app/inventory': Boxes,
    '/app/operations': PackageCheck,
    '/app/pos': ReceiptText,
    '/app/approvals': UserCheck,
    '/app/reports': BarChart3,
    '/app/audit': FileClock
  }[item.to]
}))

export default function AppShell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const visibleItems = NAV_ITEMS.filter((item) => hasAnyPermission(user, item.permissions))

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <ShieldCheck size={18} />
          </div>
          <div>
            <div className="brand-title">KEEN ERP</div>
            <div className="brand-subtitle">Inventory and POS</div>
          </div>
        </div>

        <nav className="nav-list">
          {visibleItems.map((item) => {
            const Icon = item.icon
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
              >
                <Icon size={16} />
                <span>{item.label}</span>
              </NavLink>
            )
          })}
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <div className="user-chip-name">{user?.fullName || user?.username}</div>
            <div className="user-chip-role">{user?.roles?.map((role) => role.name).join(' / ') || 'User'}</div>
          </div>
          <button
            className="btn btn-ghost"
            onClick={() => {
              logout()
              navigate('/login')
            }}
            type="button"
          >
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div>
            <div className="eyebrow">Operational workspace</div>
            <h1>Keen ERP</h1>
          </div>
          <div className="topbar-meta">
            <div className="badge badge-teal">{user?.roles?.map((role) => role.code).join(', ') || 'USER'}</div>
            <div className="badge badge-blue">{user?.locations?.length || 0} locations</div>
          </div>
        </header>
        <div className="content">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
