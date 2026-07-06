import { Boxes, Pencil, PlusCircle, RefreshCcw, RotateCcw, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { Badge, Button, Field, Input, PageSection, Select, Table } from '../components/ui'
import { useAuth } from '../context/AuthContext'

const EMPTY_FORM = {
  code: '',
  name: '',
  type: 'SHOP',
  active: true
}

const LOCATION_TYPES = [
  { value: 'MAIN_WAREHOUSE', label: 'Main Warehouse' },
  { value: 'STORE', label: 'Store' },
  { value: 'SHOP', label: 'Shop' },
  { value: 'BRANCH', label: 'Branch' },
  { value: 'DAMAGED_GOODS_AREA', label: 'Damaged Goods Area' },
  { value: 'RETURNS_AREA', label: 'Returns Area' },
  { value: 'TRANSIT_AREA', label: 'Transit Area' }
]

export default function LocationsPage() {
  const { token } = useAuth()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [actionBusyId, setActionBusyId] = useState(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [locations, setLocations] = useState([])
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  const summary = useMemo(() => ({
    total: locations.length,
    active: locations.filter((location) => location.active).length,
    shops: locations.filter((location) => String(location.type).includes('SHOP')).length,
    branches: locations.filter((location) => String(location.type).includes('BRANCH')).length
  }), [locations])

  function formatType(value) {
    return String(value || '')
      .replaceAll('_', ' ')
      .toLowerCase()
      .replace(/\b\w/g, (letter) => letter.toUpperCase())
  }

  function beginEdit(location) {
    setEditingId(location.id)
    setForm({
      code: location.code || '',
      name: location.name || '',
      type: location.type || 'SHOP',
      active: Boolean(location.active)
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
      const list = await apiFetch('/locations', { token })
      setLocations(list)
    } catch (ex) {
      setError(ex.message || 'Unable to load locations')
    } finally {
      setLoading(false)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setSuccess('')
    try {
      const payload = {
        code: form.code.trim(),
        name: form.name.trim(),
        type: form.type,
        active: Boolean(form.active)
      }
      if (editingId) {
        await apiFetch(`/locations/${editingId}`, {
          method: 'PUT',
          token,
          body: payload
        })
        setSuccess('Location updated')
      } else {
        await apiFetch('/locations', {
          method: 'POST',
          token,
          body: payload
        })
        setSuccess('Location created')
      }
      cancelEdit()
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to save location')
    } finally {
      setBusy(false)
    }
  }

  async function setActive(location, active) {
    setActionBusyId(location.id)
    setError('')
    setSuccess('')
    try {
      if (active) {
        await apiFetch(`/locations/${location.id}`, {
          method: 'PUT',
          token,
          body: {
            code: location.code,
            name: location.name,
            type: location.type,
            active: true
          }
        })
        setSuccess(`${location.name} reactivated`)
      } else {
        await apiFetch(`/locations/${location.id}`, {
          method: 'DELETE',
          token
        })
        setSuccess(`${location.name} deactivated`)
      }
      if (editingId === location.id) {
        cancelEdit()
      }
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to update location')
    } finally {
      setActionBusyId(null)
    }
  }

  if (loading) {
    return <PageSection title="Loading locations" subtitle="Fetching branches, shops, and warehouse records." />
  }

  return (
    <div className="page-grid">
      <PageSection
        title="Locations"
        subtitle="Create and manage warehouses, shops, branches, and support areas."
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
          <div className="metric metric-slate">
            <div className="metric-label">Shops</div>
            <div className="metric-value">{summary.shops}</div>
          </div>
          <div className="metric metric-warning">
            <div className="metric-label">Branches</div>
            <div className="metric-value">{summary.branches}</div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="form-grid">
          <Field label="Code">
            <Input
              value={form.code}
              onChange={(event) => setForm({ ...form, code: event.target.value })}
              placeholder="SHOP-A"
            />
          </Field>
          <Field label="Name">
            <Input
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              placeholder="Shop A"
            />
          </Field>
          <Field label="Type">
            <Select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
              {LOCATION_TYPES.map((type) => (
                <option key={type.value} value={type.value}>{type.label}</option>
              ))}
            </Select>
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
              {editingId ? 'Update location' : 'Create location'}
            </Button>
            {editingId ? (
              <Button type="button" variant="secondary" icon={X} onClick={cancelEdit}>
                Cancel
              </Button>
            ) : null}
          </div>
        </form>
      </PageSection>

      <PageSection title="All locations" subtitle="Warehouse, shop, branch, and support locations in the system.">
        <Table
          columns={['Code', 'Name', 'Type', 'Status', 'Actions']}
          rows={locations}
          rowKey="id"
          renderRow={(location) => (
            <tr key={location.id}>
              <td>{location.code}</td>
              <td>{location.name}</td>
              <td>{formatType(location.type)}</td>
              <td>
                <Badge tone={location.active ? 'teal' : 'danger'}>
                  {location.active ? 'Active' : 'Inactive'}
                </Badge>
              </td>
              <td>
                <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 0 }}>
                  <Button
                    type="button"
                    variant="secondary"
                    icon={Pencil}
                    className="btn-sm"
                    onClick={() => beginEdit(location)}
                    disabled={actionBusyId !== null || busy}
                  >
                    Edit
                  </Button>
                  {location.active ? (
                    <Button
                      type="button"
                      variant="secondary"
                      icon={Boxes}
                      className="btn-sm"
                      onClick={() => navigate(`/app/inventory?locationId=${encodeURIComponent(String(location.id))}`)}
                      disabled={busy || actionBusyId !== null}
                    >
                      Add stock
                    </Button>
                  ) : null}
                  {location.active ? (
                    <Button
                      type="button"
                      variant="danger"
                      icon={Trash2}
                      className="btn-sm"
                      busy={actionBusyId === location.id}
                      onClick={() => setActive(location, false)}
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
                      busy={actionBusyId === location.id}
                      onClick={() => setActive(location, true)}
                      disabled={busy || actionBusyId !== null}
                    >
                      Reactivate
                    </Button>
                  )}
                </div>
              </td>
            </tr>
          )}
        />
      </PageSection>
    </div>
  )
}
