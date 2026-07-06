import { RefreshCcw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { apiFetch } from '../api/client'
import { Badge, Button, MetricCard, PageSection, Table } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { hasAnyPermission } from '../utils/access'
import { formatDateTime, formatMoney, toneForStatus } from '../utils/format'

export default function DashboardPage() {
  const { token, user } = useAuth()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [summary, setSummary] = useState(null)
  const [requests, setRequests] = useState([])
  const [transfers, setTransfers] = useState([])
  const [sales, setSales] = useState([])
  const [notifications, setNotifications] = useState([])
  const canViewSummary = hasAnyPermission(user, ['reports:view'])
  const canViewRequests = hasAnyPermission(user, ['requests:view', 'requests:manage'])
  const canViewTransfers = hasAnyPermission(user, ['transfers:view', 'transfers:manage'])
  const canViewSales = hasAnyPermission(user, ['sales:view', 'sales:manage'])
  const canViewNotifications = hasAnyPermission(user, ['notifications:view'])

  useEffect(() => {
    if (!token || !user) {
      return
    }
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user])

  async function load() {
    setLoading(true)
    setError('')
    try {
      const requestsNext = canViewRequests ? apiFetch('/stock-requests', { token }) : Promise.resolve([])
      const transfersNext = canViewTransfers ? apiFetch('/stock-transfers', { token }) : Promise.resolve([])
      const salesNext = canViewSales ? apiFetch('/sales', { token }) : Promise.resolve([])
      const notificationsNext = canViewNotifications ? apiFetch('/notifications', { token }) : Promise.resolve([])

      const [dashboard, requestList, transferList, saleList, notificationList] = await Promise.all([
        canViewSummary ? apiFetch('/reports/dashboard', { token }) : Promise.resolve(null),
        requestsNext,
        transfersNext,
        salesNext,
        notificationsNext
      ])

      setSummary(dashboard)
      setRequests(requestList)
      setTransfers(transferList)
      setSales(saleList)
      setNotifications(notificationList)
    } catch (ex) {
      setError(ex.message || 'Unable to load dashboard')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <PageSection title="Loading dashboard" subtitle="Fetching the latest operational snapshot." />
  }

  return (
    <div className="page-grid">
      <PageSection
        title="Overview"
        subtitle="Live stock, transfers, sales, and workflow status."
        actions={<Button type="button" variant="secondary" icon={RefreshCcw} onClick={load}>Refresh</Button>}
      >
        {error ? <div className="error-banner">{error}</div> : null}
        <div className="grid-4">
          <MetricCard label="Stock value" value={formatMoney(summary?.totalStockValue)} note="All tracked locations" tone="teal" />
          <MetricCard label="Sales today" value={formatMoney(summary?.totalSalesToday)} note={`${summary?.completedSalesToday || 0} receipts`} tone="blue" />
          <MetricCard label="Pending requests" value={summary?.pendingRequests ?? 0} note="Awaiting review" tone="slate" />
          <MetricCard label="In transit" value={summary?.inTransitTransfers ?? 0} note="Approved transfers on the road" tone="blue" />
          <MetricCard label="Active products" value={summary?.activeProducts ?? 0} note="Catalog items in use" tone="teal" />
          <MetricCard label="Locations" value={summary?.totalLocations ?? 0} note="Warehouse, shop, transit, and others" tone="slate" />
          <MetricCard label="Low stock" value={summary?.lowStockBalances ?? 0} note="At or below reorder level" tone="warning" />
          <MetricCard label="Unread alerts" value={summary?.unreadNotifications ?? 0} note="Notifications not yet cleared" tone="danger" />
        </div>
      </PageSection>

      <div className="split-grid">
        {canViewRequests ? (
          <PageSection title="Recent requests" subtitle="Latest stock request activity.">
            <Table
              columns={['Request', 'Source', 'Destination', 'Status', 'Created']}
              rows={requests.slice(0, 5)}
              rowKey="id"
              renderRow={(request) => (
                <tr key={request.id}>
                  <td>{request.requestNumber}</td>
                  <td>{request.sourceLocation?.name}</td>
                  <td>{request.destinationLocation?.name}</td>
                  <td><Badge tone={toneForStatus(request.status)}>{request.status}</Badge></td>
                  <td>{formatDateTime(request.createdAt)}</td>
                </tr>
              )}
            />
          </PageSection>
        ) : null}

        {canViewTransfers ? (
          <PageSection title="Recent transfers" subtitle="Warehouse to shop movement at a glance.">
            <Table
              columns={['Transfer', 'Source', 'Destination', 'Status', 'Created']}
              rows={transfers.slice(0, 5)}
              rowKey="id"
              renderRow={(transfer) => (
                <tr key={transfer.id}>
                  <td>{transfer.transferNumber}</td>
                  <td>{transfer.sourceLocation?.name}</td>
                  <td>{transfer.destinationLocation?.name}</td>
                  <td><Badge tone={toneForStatus(transfer.status)}>{transfer.status}</Badge></td>
                  <td>{formatDateTime(transfer.createdAt)}</td>
                </tr>
              )}
            />
          </PageSection>
        ) : null}
      </div>

      <div className="split-grid">
        {canViewSales ? (
          <PageSection title="Recent sales" subtitle="Completed checkout activity.">
            <Table
              columns={['Receipt', 'Location', 'Cashier', 'Total', 'Time']}
              rows={sales.slice(0, 5)}
              rowKey="id"
              renderRow={(sale) => (
                <tr key={sale.id}>
                  <td>{sale.receiptNumber}</td>
                  <td>{sale.location?.name}</td>
                  <td>{sale.cashier?.fullName}</td>
                  <td>{formatMoney(sale.totalAmount)}</td>
                  <td>{formatDateTime(sale.createdAt)}</td>
                </tr>
              )}
            />
          </PageSection>
        ) : null}

        {canViewNotifications ? (
          <PageSection title="Notifications" subtitle="System messages that need attention.">
            <Table
              columns={['Title', 'Role', 'Status', 'Time']}
              rows={notifications.slice(0, 5)}
              rowKey="id"
              renderRow={(notification) => (
                <tr key={notification.id}>
                  <td>{notification.title}</td>
                  <td>{notification.recipientRole || 'All'}</td>
                  <td><Badge tone={notification.readFlag ? 'slate' : 'teal'}>{notification.readFlag ? 'Read' : 'Unread'}</Badge></td>
                  <td>{formatDateTime(notification.createdAt)}</td>
                </tr>
              )}
            />
          </PageSection>
        ) : null}
      </div>
    </div>
  )
}
