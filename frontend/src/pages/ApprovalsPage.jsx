import { RefreshCcw, ShieldCheck, UserCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { Badge, Button, PageSection, Table } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { formatDateTime } from '../utils/format'

export default function ApprovalsPage() {
  const { token, hasPermission } = useAuth()
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [pendingUsers, setPendingUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [locations, setLocations] = useState([])
  const [selectedUserId, setSelectedUserId] = useState(null)
  const [selectedRoleIds, setSelectedRoleIds] = useState([])
  const [selectedLocationIds, setSelectedLocationIds] = useState([])

  useEffect(() => {
    if (!hasPermission('admin:all')) {
      return
    }
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, hasPermission])

  async function load() {
    setLoading(true)
    setError('')
    setSuccess('')
    try {
      const [pending, options] = await Promise.all([
        apiFetch('/admin/users/pending', { token }),
        apiFetch('/admin/users/options', { token })
      ])
      setPendingUsers(pending)
      setRoles(options.roles || [])
      setLocations(options.locations || [])

      setSelectedUserId((current) => {
        if (pending.some((user) => user.id === current)) {
          return current
        }
        return pending[0]?.id || null
      })
    } catch (ex) {
      setError(ex.message || 'Unable to load approvals')
    } finally {
      setLoading(false)
    }
  }

  const selectedUser = pendingUsers.find((user) => user.id === selectedUserId) || null

  function selectUser(user) {
    setSelectedUserId(user.id)
    setSelectedRoleIds([])
    setSelectedLocationIds([])
    setError('')
    setSuccess('')
  }

  function toggleSelection(value, setValues) {
    setValues((current) => (current.includes(value) ? current.filter((item) => item !== value) : [...current, value]))
  }

  async function handleApprove(event) {
    event.preventDefault()
    if (!selectedUser) return
    if (!selectedRoleIds.length) {
      setError('Select at least one role')
      return
    }

    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const response = await apiFetch(`/admin/users/${selectedUser.id}/approve`, {
        method: 'POST',
        token,
        body: {
          roleIds: selectedRoleIds,
          locationIds: selectedLocationIds
        }
      })

      const nextPending = pendingUsers.filter((user) => user.id !== selectedUser.id)
      setPendingUsers(nextPending)
      setSuccess(response.message || `${response.username} approved`)

      const nextUser = nextPending[0] || null
      if (nextUser) {
        setSelectedUserId(nextUser.id)
        setSelectedRoleIds([])
        setSelectedLocationIds([])
      } else {
        setSelectedUserId(null)
        setSelectedRoleIds([])
        setSelectedLocationIds([])
      }
    } catch (ex) {
      setError(ex.message || 'Approval failed')
    } finally {
      setBusy(false)
    }
  }

  if (!hasPermission('admin:all')) {
    return <Navigate to="/app/dashboard" replace />
  }

  if (loading) {
    return <PageSection title="Loading approvals" subtitle="Fetching pending accounts and assignment options." />
  }

  return (
    <div className="page-grid">
      <PageSection
        title="Account approvals"
        subtitle="Review new registrations, assign access, and activate accounts."
        actions={
          <Button type="button" variant="secondary" icon={RefreshCcw} onClick={load}>
            Refresh
          </Button>
        }
      >
        {error ? <div className="error-banner">{error}</div> : null}
        {success ? <div className="success-banner">{success}</div> : null}
        <div className="split-grid">
          <div>
            <Table
              columns={['Username', 'Name', 'Email', 'Created']}
              rows={pendingUsers}
              rowKey="id"
              emptyMessage="No pending registrations."
              renderRow={(user) => (
                <tr
                  key={user.id}
                  className={`clickable${selectedUserId === user.id ? ' selected' : ''}`}
                  onClick={() => selectUser(user)}
                >
                  <td>{user.username}</td>
                  <td>{user.fullName}</td>
                  <td>{user.email}</td>
                  <td>{formatDateTime(user.createdAt)}</td>
                </tr>
              )}
            />
          </div>

          <form className="approval-panel" onSubmit={handleApprove}>
            <div className="approval-summary">
              <div className="approval-summary-head">
                <div>
                  <div className="approval-title">Selected account</div>
                  <div className="approval-subtitle">Choose the roles and locations before approval.</div>
                </div>
                <Badge tone={selectedUser ? 'warning' : 'slate'}>
                  {selectedUser ? 'Pending' : 'None selected'}
                </Badge>
              </div>

              {selectedUser ? (
                <div className="approval-details">
                  <div className="approval-name">
                    <ShieldCheck size={16} />
                    <span>{selectedUser.fullName}</span>
                  </div>
                  <div className="approval-meta">{selectedUser.username}</div>
                  <div className="approval-meta">{selectedUser.email}</div>
                  <div className="approval-meta">Submitted {formatDateTime(selectedUser.createdAt)}</div>
                </div>
              ) : (
                <div className="approval-empty">
                  Select a pending account from the list to review it.
                </div>
              )}
            </div>

            <div className="approval-grid">
              <div className="approval-group">
                <div className="approval-group-title">Roles</div>
                <div className="approval-list">
                  {roles.map((role) => (
                    <label key={role.id} className="approval-check">
                      <input
                        type="checkbox"
                        checked={selectedRoleIds.includes(role.id)}
                        onChange={() => toggleSelection(role.id, setSelectedRoleIds)}
                        disabled={!selectedUser || busy}
                      />
                      <span>
                        <strong>{role.name}</strong>
                        <small>{role.code}</small>
                      </span>
                    </label>
                  ))}
                </div>
              </div>

              <div className="approval-group">
                <div className="approval-group-title">Locations</div>
                <div className="approval-list">
                  {locations.map((location) => (
                    <label key={location.id} className="approval-check">
                      <input
                        type="checkbox"
                        checked={selectedLocationIds.includes(location.id)}
                        onChange={() => toggleSelection(location.id, setSelectedLocationIds)}
                        disabled={!selectedUser || busy}
                      />
                      <span>
                        <strong>{location.name}</strong>
                        <small>{location.code}</small>
                      </span>
                    </label>
                  ))}
                </div>
              </div>
            </div>

            <div className="form-actions">
              <Button
                type="submit"
                icon={UserCheck}
                busy={busy}
                disabled={!selectedUser || busy || !selectedRoleIds.length}
              >
                Approve account
              </Button>
            </div>
          </form>
        </div>
      </PageSection>
    </div>
  )
}
