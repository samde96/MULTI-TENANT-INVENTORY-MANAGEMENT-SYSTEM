import { Navigate, Route, Routes } from 'react-router-dom'
import AccessGate from './components/AccessGate'
import AppShell from './components/AppShell'
import { useAuth } from './context/AuthContext'
import { getHomePath, ROUTE_PERMISSIONS } from './utils/access'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ApprovalsPage from './pages/ApprovalsPage'
import DashboardPage from './pages/DashboardPage'
import LocationsPage from './pages/LocationsPage'
import SuppliersPage from './pages/SuppliersPage'
import InventoryPage from './pages/InventoryPage'
import OperationsPage from './pages/OperationsPage'
import PosPage from './pages/PosPage'
import ReportsPage from './pages/ReportsPage'
import AuditPage from './pages/AuditPage'
import { Loader2 } from 'lucide-react'

function ProtectedApp() {
  const { token, loading, user } = useAuth()

  if (loading) {
    return (
      <div className="loading-screen">
        <Loader2 className="spin" size={20} />
        <span>Loading Keen ERP...</span>
      </div>
    )
  }

  if (!token) {
    return <Navigate to="/login" replace />
  }

  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/app/dashboard" element={<DashboardPage />} />
        <Route path="/app/locations" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/locations']}><LocationsPage /></AccessGate>} />
        <Route path="/app/suppliers" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/suppliers']}><SuppliersPage /></AccessGate>} />
        <Route path="/app/inventory" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/inventory']}><InventoryPage /></AccessGate>} />
        <Route path="/app/operations" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/operations']}><OperationsPage /></AccessGate>} />
        <Route path="/app/pos" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/pos']}><PosPage /></AccessGate>} />
        <Route path="/app/approvals" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/approvals']}><ApprovalsPage /></AccessGate>} />
        <Route path="/app/reports" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/reports']}><ReportsPage /></AccessGate>} />
        <Route path="/app/audit" element={<AccessGate permissions={ROUTE_PERMISSIONS['/app/audit']}><AuditPage /></AccessGate>} />
        <Route path="*" element={<Navigate to={getHomePath(user)} replace />} />
      </Route>
    </Routes>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="*" element={<ProtectedApp />} />
    </Routes>
  )
}
