import { Pencil, PlusCircle, RefreshCcw, RotateCcw, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { apiFetch } from '../api/client'
import { Badge, Button, Field, Input, PageSection, Table } from '../components/ui'
import { useAuth } from '../context/AuthContext'

const EMPTY_FORM = {
  name: '',
  contactName: '',
  phone: '',
  email: '',
  active: true
}

export default function SuppliersPage() {
  const { token, hasPermission } = useAuth()
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [actionBusyId, setActionBusyId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [suppliers, setSuppliers] = useState([])
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  const canManageCatalog = hasPermission('catalog:manage')

  const summary = useMemo(
    () => ({
      total: suppliers.length,
      active: suppliers.filter((supplier) => supplier.active).length,
      inactive: suppliers.filter((supplier) => !supplier.active).length
    }),
    [suppliers]
  )

  function beginEdit(supplier) {
    setEditingId(supplier.id)
    setForm({
      name: supplier.name || '',
      contactName: supplier.contactName || '',
      phone: supplier.phone || '',
      email: supplier.email || '',
      active: Boolean(supplier.active)
    })
    setError('')
    setSuccess('')
  }

  function cancelEdit() {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setError('')
  }

  async function load() {
    setLoading(true)
    setError('')
    try {
      const list = await apiFetch('/suppliers', { token })
      setSuppliers(list)
    } catch (ex) {
      setError(ex.message || 'Unable to load suppliers')
    } finally {
      setLoading(false)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (!canManageCatalog) return
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const payload = {
        name: form.name.trim(),
        contactName: form.contactName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        active: Boolean(form.active)
      }
      if (editingId) {
        await apiFetch(`/suppliers/${editingId}`, {
          method: 'PUT',
          token,
          body: payload
        })
        setSuccess('Supplier updated')
      } else {
        await apiFetch('/suppliers', {
          method: 'POST',
          token,
          body: payload
        })
        setSuccess('Supplier created')
      }
      cancelEdit()
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to save supplier')
    } finally {
      setBusy(false)
    }
  }

  async function setActive(supplier, active) {
    if (!canManageCatalog) return
    setActionBusyId(supplier.id)
    setError('')
    setSuccess('')
    try {
      if (active) {
        await apiFetch(`/suppliers/${supplier.id}`, {
          method: 'PUT',
          token,
          body: {
            name: supplier.name,
            contactName: supplier.contactName,
            phone: supplier.phone,
            email: supplier.email,
            active: true
          }
        })
        setSuccess(`${supplier.name} reactivated`)
      } else {
        await apiFetch(`/suppliers/${supplier.id}`, {
          method: 'DELETE',
          token
        })
        setSuccess(`${supplier.name} deactivated`)
      }
      if (editingId === supplier.id) {
        cancelEdit()
      }
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to update supplier')
    } finally {
      setActionBusyId(null)
    }
  }

  if (loading) {
    return <PageSection title="Loading suppliers" subtitle="Fetching supplier records." />
  }

  return (
    <div className="page-grid">
      <PageSection
        title="Suppliers"
        subtitle="Create and manage supplier records used for receiving stock."
        actions={<Button type="button" variant="secondary" icon={RefreshCcw} onClick={load}>Refresh</Button>}
      >
        {error ? <div className="error-banner">{error}</div> : null}
        {success ? <div className="success-banner">{success}</div> : null}

        <div className="grid-4" style={{ marginBottom: 16 }}>
          <div className="metric metric-teal">
            <div className="metric-label">Total</div>
            <div className="metric-value">{summary.total}</div>
          </div>
          <div className="metric metric-blue">
            <div className="metric-label">Active</div>
            <div className="metric-value">{summary.active}</div>
          </div>
          <div className="metric metric-warning">
            <div className="metric-label">Inactive</div>
            <div className="metric-value">{summary.inactive}</div>
          </div>
        </div>

        {canManageCatalog ? (
          <form onSubmit={handleSubmit} className="form-grid">
            <Field label="Supplier name">
              <Input
                value={form.name}
                onChange={(event) => setForm({ ...form, name: event.target.value })}
                placeholder="Keen Wholesale Ltd"
                required
              />
            </Field>
            <Field label="Contact name">
              <Input
                value={form.contactName}
                onChange={(event) => setForm({ ...form, contactName: event.target.value })}
                placeholder="Supply Desk"
              />
            </Field>
            <Field label="Phone">
              <Input
                value={form.phone}
                onChange={(event) => setForm({ ...form, phone: event.target.value })}
                placeholder="+254700000000"
              />
            </Field>
            <Field label="Email">
              <Input
                type="email"
                value={form.email}
                onChange={(event) => setForm({ ...form, email: event.target.value })}
                placeholder="supply@keen.local"
              />
            </Field>
            <Field label="Active">
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, minHeight: 40 }}>
                <input
                  type="checkbox"
                  checked={form.active}
                  onChange={(event) => setForm({ ...form, active: event.target.checked })}
                />
                <span>Enabled</span>
              </label>
            </Field>
            <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
              <Button type="submit" icon={editingId ? Pencil : PlusCircle} busy={busy}>
                {editingId ? 'Update supplier' : 'Create supplier'}
              </Button>
              {editingId ? (
                <Button type="button" variant="secondary" icon={X} onClick={cancelEdit}>
                  Cancel
                </Button>
              ) : null}
            </div>
          </form>
        ) : null}
      </PageSection>

      <PageSection title="All suppliers" subtitle="Supplier list used in product setup and stock receiving.">
        <Table
          columns={['Name', 'Contact', 'Phone', 'Email', 'Status', 'Actions']}
          rows={suppliers}
          rowKey="id"
          renderRow={(supplier) => (
            <tr key={supplier.id}>
              <td>{supplier.name}</td>
              <td>{supplier.contactName || '-'}</td>
              <td>{supplier.phone || '-'}</td>
              <td>{supplier.email || '-'}</td>
              <td>
                <Badge tone={supplier.active ? 'teal' : 'danger'}>
                  {supplier.active ? 'Active' : 'Inactive'}
                </Badge>
              </td>
              <td>
                {canManageCatalog ? (
                  <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 0 }}>
                    <Button
                      type="button"
                      variant="secondary"
                      icon={Pencil}
                      className="btn-sm"
                      onClick={() => beginEdit(supplier)}
                      disabled={actionBusyId !== null || busy}
                    >
                      Edit
                    </Button>
                    {supplier.active ? (
                      <Button
                        type="button"
                        variant="danger"
                        icon={Trash2}
                        className="btn-sm"
                        busy={actionBusyId === supplier.id}
                        onClick={() => setActive(supplier, false)}
                        disabled={busy || actionBusyId !== null}
                      >
                        Deactivate
                      </Button>
                    ) : (
                      <Button
                        type="button"
                        variant="secondary"
                        icon={RotateCcw}
                        className="btn-sm"
                        busy={actionBusyId === supplier.id}
                        onClick={() => setActive(supplier, true)}
                        disabled={busy || actionBusyId !== null}
                      >
                        Reactivate
                      </Button>
                    )}
                  </div>
                ) : (
                  <span>-</span>
                )}
              </td>
            </tr>
          )}
        />
      </PageSection>
    </div>
  )
}
