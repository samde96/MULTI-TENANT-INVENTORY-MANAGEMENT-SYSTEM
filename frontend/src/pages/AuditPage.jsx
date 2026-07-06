import { CheckCircle2, RefreshCcw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { apiFetch } from '../api/client'
import { Badge, Button, PageSection, Table } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { formatDateTime, toneForStatus } from '../utils/format'

export default function AuditPage() {
  const { token } = useAuth()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [logs, setLogs] = useState([])
  const [notifications, setNotifications] = useState([])
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [logList, notificationList] = await Promise.all([
        apiFetch('/audit-logs', { token }),
        apiFetch('/notifications', { token })
      ])
      setLogs(logList)
      setNotifications(notificationList)
    } catch (ex) {
      setError(ex.message || 'Unable to load audit data')
    } finally {
      setLoading(false)
    }
  }

  async function markRead(id) {
    setBusy(true)
    try {
      await apiFetch(`/notifications/${id}/read`, { method: 'PATCH', token })
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to mark notification as read')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <PageSection title="Loading audit data" subtitle="Pulling logs and notifications." />
  }

  return (
    <div className="page-grid">
      <PageSection
        title="Audit trail"
        subtitle="Security-sensitive actions recorded by the backend."
        actions={<Button type="button" variant="secondary" icon={RefreshCcw} onClick={load}>Refresh</Button>}
      >
        {error ? <div className="error-banner">{error}</div> : null}
        <Table
          columns={['Action', 'Entity', 'User', 'Success', 'Time']}
          rows={logs}
          rowKey="id"
          renderRow={(log) => (
            <tr key={log.id}>
              <td>{log.action}</td>
              <td>{log.entityType}</td>
              <td>{log.username}</td>
              <td><Badge tone={toneForStatus(log.success ? 'SUCCESS' : 'FAILED')}>{log.success ? 'Success' : 'Failed'}</Badge></td>
              <td>{formatDateTime(log.createdAt)}</td>
            </tr>
          )}
        />
      </PageSection>

      <PageSection title="Notifications" subtitle="In-app and email-style events recorded by the system.">
        <Table
          columns={['Title', 'Role', 'Channel', 'State', 'Action']}
          rows={notifications}
          rowKey="id"
          renderRow={(notification) => (
            <tr key={notification.id}>
              <td>{notification.title}</td>
              <td>{notification.recipientRole || 'All'}</td>
              <td>{notification.channel}</td>
              <td><Badge tone={notification.readFlag ? 'slate' : 'teal'}>{notification.readFlag ? 'Read' : 'Unread'}</Badge></td>
              <td>
                <Button
                  type="button"
                  variant="secondary"
                  icon={CheckCircle2}
                  className="btn-sm"
                  disabled={notification.readFlag || busy}
                  onClick={() => markRead(notification.id)}
                >
                  Mark read
                </Button>
              </td>
            </tr>
          )}
        />
      </PageSection>
    </div>
  )
}
