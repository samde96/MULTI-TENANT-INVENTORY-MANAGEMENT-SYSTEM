import { RefreshCcw } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { apiFetch } from '../api/client'
import { Badge, Button, MetricCard, PageSection, Table } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { hasAnyPermission } from '../utils/access'
import { formatDateTime, formatMoney, toneForStatus } from '../utils/format'

export default function ReportsPage() {
  const { token, user } = useAuth()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [dashboard, setDashboard] = useState(null)
  const [balances, setBalances] = useState([])
  const [sales, setSales] = useState([])
  const [ledger, setLedger] = useState([])
  const [requests, setRequests] = useState([])
  const [transfers, setTransfers] = useState([])
  const canViewSummary = hasAnyPermission(user, ['reports:view'])
  const canViewInventory = hasAnyPermission(user, ['inventory:view', 'inventory:receive'])
  const canViewRequests = hasAnyPermission(user, ['requests:view', 'requests:manage'])
  const canViewTransfers = hasAnyPermission(user, ['transfers:view', 'transfers:manage'])
  const canViewSales = hasAnyPermission(user, ['sales:view', 'sales:manage'])

  useEffect(() => {
    if (!token || !user) {
      return
    }
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, user])

  const lowStockBalances = useMemo(() => {
    return balances.filter((balance) => balance.quantityOnHand <= (balance.product?.reorderLevel || 0))
  }, [balances])

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [summary, balanceList, saleList, ledgerList, requestList, transferList] = await Promise.all([
        canViewSummary ? apiFetch('/reports/dashboard', { token }) : Promise.resolve(null),
        canViewInventory ? apiFetch('/inventory/balances', { token }) : Promise.resolve([]),
        canViewSales ? apiFetch('/sales', { token }) : Promise.resolve([]),
        canViewInventory ? apiFetch('/inventory/ledger', { token }) : Promise.resolve([]),
        canViewRequests ? apiFetch('/stock-requests', { token }) : Promise.resolve([]),
        canViewTransfers ? apiFetch('/stock-transfers', { token }) : Promise.resolve([])
      ])
      setDashboard(summary)
      setBalances(balanceList)
      setSales(saleList)
      setLedger(ledgerList)
      setRequests(requestList)
      setTransfers(transferList)
    } catch (ex) {
      setError(ex.message || 'Unable to load reports')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <PageSection title="Loading reports" subtitle="Gathering metrics and movement logs." />
  }

  return (
    <div className="page-grid">
      <PageSection
        title="Report dashboard"
        subtitle="Totals, stock health, and movement activity."
        actions={<Button type="button" variant="secondary" icon={RefreshCcw} onClick={load}>Refresh</Button>}
      >
        {error ? <div className="error-banner">{error}</div> : null}
        <div className="grid-4">
          <MetricCard label="Stock value" value={formatMoney(dashboard?.totalStockValue)} tone="teal" />
          <MetricCard label="Sales today" value={formatMoney(dashboard?.totalSalesToday)} tone="blue" />
          <MetricCard label="Low stock" value={dashboard?.lowStockBalances ?? 0} tone="warning" />
          <MetricCard label="Requests" value={requests.length} tone="slate" />
        </div>
      </PageSection>

      <div className="split-grid">
        {canViewInventory ? (
          <PageSection title="Low stock items" subtitle="Balances at or below reorder level.">
            <Table
              columns={['Product', 'Location', 'Qty', 'Reorder', 'Status']}
              rows={lowStockBalances}
              rowKey="id"
              renderRow={(balance) => (
                <tr key={balance.id}>
                  <td>{balance.product?.name}</td>
                  <td>{balance.location?.name}</td>
                  <td>{balance.quantityOnHand}</td>
                  <td>{balance.product?.reorderLevel ?? 0}</td>
                  <td><Badge tone={toneForStatus('warning')}>Low</Badge></td>
                </tr>
              )}
            />
          </PageSection>
        ) : null}

        {canViewInventory ? (
          <PageSection title="Ledger" subtitle="Recent stock movement entries.">
            <Table
              columns={['Movement', 'Product', 'Qty', 'Ref', 'Time']}
              rows={ledger.slice(0, 8)}
              rowKey="id"
              renderRow={(entry) => (
                <tr key={entry.id}>
                  <td><Badge tone={toneForStatus(entry.movementType)}>{entry.movementType}</Badge></td>
                  <td>{entry.product?.name}</td>
                  <td>{entry.quantity}</td>
                  <td>{entry.referenceNumber}</td>
                  <td>{formatDateTime(entry.createdAt)}</td>
                </tr>
              )}
            />
          </PageSection>
        ) : null}
      </div>

      <div className="split-grid">
        {canViewTransfers ? (
          <PageSection title="Transfers" subtitle="Recent transfer records.">
            <Table
              columns={['Transfer', 'From', 'To', 'Status', 'Time']}
              rows={transfers.slice(0, 6)}
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

        {canViewSales ? (
          <PageSection title="Sales" subtitle="Recent completed receipts.">
            <Table
              columns={['Receipt', 'Location', 'Amount', 'Method', 'Time']}
              rows={sales.slice(0, 6)}
              rowKey="id"
              renderRow={(sale) => (
                <tr key={sale.id}>
                  <td>{sale.receiptNumber}</td>
                  <td>{sale.location?.name}</td>
                  <td>{formatMoney(sale.totalAmount)}</td>
                  <td>{sale.paymentMethod}</td>
                  <td>{formatDateTime(sale.createdAt)}</td>
                </tr>
              )}
            />
          </PageSection>
        ) : null}
      </div>
    </div>
  )
}
