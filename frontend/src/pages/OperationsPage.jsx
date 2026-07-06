import { ArrowRightLeft, CheckCircle2, PlusCircle, RefreshCcw, RotateCcw, Send } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { apiFetch } from '../api/client'
import { Badge, Button, Field, Input, PageSection, Select, Table, TextArea } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { formatDateTime, toneForStatus } from '../utils/format'

function createLineItem() {
  return { productId: '', quantity: 1, unitCost: '' }
}

function toNumber(value) {
  return value === '' || value === null || value === undefined ? null : Number(value)
}

export default function OperationsPage() {
  const { token } = useAuth()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [products, setProducts] = useState([])
  const [locations, setLocations] = useState([])
  const [requests, setRequests] = useState([])
  const [transfers, setTransfers] = useState([])
  const [requestBusy, setRequestBusy] = useState(false)
  const [transferBusy, setTransferBusy] = useState(false)
  const [actionBusy, setActionBusy] = useState(false)
  const [selectedTransfer, setSelectedTransfer] = useState(null)
  const [requestForm, setRequestForm] = useState({
    sourceLocationId: '',
    destinationLocationId: '',
    note: '',
    items: [createLineItem()]
  })
  const [transferForm, setTransferForm] = useState({
    sourceLocationId: '',
    destinationLocationId: '',
    note: '',
    items: [createLineItem()]
  })
  const [receiveForm, setReceiveForm] = useState({
    note: '',
    items: []
  })

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  useEffect(() => {
    if (selectedTransfer) {
      setReceiveForm({
        note: '',
        items: selectedTransfer.items.map((item) => ({
          productId: item.product.id,
          quantityReceived: item.quantity
        }))
      })
    }
  }, [selectedTransfer])

  const activeLocations = useMemo(() => locations.filter((location) => location.active), [locations])

  const warehouseLocations = useMemo(
    () => activeLocations.filter((location) => String(location.type).includes('WAREHOUSE') || String(location.type).includes('STORE')),
    [activeLocations]
  )

  const shopLocations = useMemo(
    () => activeLocations.filter((location) => String(location.type).includes('SHOP') || String(location.type).includes('BRANCH')),
    [activeLocations]
  )

  const canCreateRequest = warehouseLocations.length > 0 && shopLocations.length > 0
  const canCreateTransfer = warehouseLocations.length > 0 && shopLocations.length > 0

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [productList, locationList, requestList, transferList] = await Promise.all([
        apiFetch('/products', { token }),
        apiFetch('/locations', { token }),
        apiFetch('/stock-requests', { token }),
        apiFetch('/stock-transfers', { token })
      ])
      setProducts(productList)
      setLocations(locationList)
      setRequests(requestList)
      setTransfers(transferList)

      const nextActiveLocations = locationList.filter((location) => location.active)
      setRequestForm((current) => ({
        ...current,
        sourceLocationId: current.sourceLocationId || nextActiveLocations.find((location) => String(location.type).includes('WAREHOUSE'))?.id || nextActiveLocations[0]?.id || '',
        destinationLocationId: current.destinationLocationId || nextActiveLocations.find((location) => String(location.type).includes('SHOP'))?.id || nextActiveLocations[0]?.id || ''
      }))
      setTransferForm((current) => ({
        ...current,
        sourceLocationId: current.sourceLocationId || nextActiveLocations.find((location) => String(location.type).includes('WAREHOUSE'))?.id || nextActiveLocations[0]?.id || '',
        destinationLocationId: current.destinationLocationId || nextActiveLocations.find((location) => String(location.type).includes('SHOP'))?.id || nextActiveLocations[0]?.id || ''
      }))
    } catch (ex) {
      setError(ex.message || 'Unable to load workflow data')
    } finally {
      setLoading(false)
    }
  }

  function updateLineItem(setter, index, key, value) {
    setter((current) => {
      const items = [...current.items]
      items[index] = { ...items[index], [key]: value }
      return { ...current, items }
    })
  }

  function addLineItem(setter) {
    setter((current) => ({ ...current, items: [...current.items, createLineItem()] }))
  }

  function removeLineItem(setter, index) {
    setter((current) => {
      const items = current.items.filter((_, itemIndex) => itemIndex !== index)
      return { ...current, items: items.length ? items : [createLineItem()] }
    })
  }

  async function submitRequest(event) {
    event.preventDefault()
    if (!canCreateRequest) {
      setError('Create at least one active warehouse/store and one active shop/branch location first.')
      return
    }
    setRequestBusy(true)
    setError('')
    try {
      await apiFetch('/stock-requests', {
        method: 'POST',
        token,
        body: {
          sourceLocationId: Number(requestForm.sourceLocationId),
          destinationLocationId: Number(requestForm.destinationLocationId),
          note: requestForm.note,
          items: requestForm.items
            .filter((item) => item.productId)
            .map((item) => ({
              productId: Number(item.productId),
              quantityRequested: Number(item.quantity)
            }))
        }
      })
      await load()
      setRequestForm((current) => ({ ...current, note: '', items: [createLineItem()] }))
    } catch (ex) {
      setError(ex.message || 'Unable to create request')
    } finally {
      setRequestBusy(false)
    }
  }

  async function submitTransfer(event) {
    event.preventDefault()
    if (!canCreateTransfer) {
      setError('Create at least one active warehouse/store and one active shop/branch location first.')
      return
    }
    setTransferBusy(true)
    setError('')
    try {
      await apiFetch('/stock-transfers', {
        method: 'POST',
        token,
        body: {
          sourceLocationId: Number(transferForm.sourceLocationId),
          destinationLocationId: Number(transferForm.destinationLocationId),
          note: transferForm.note,
          items: transferForm.items
            .filter((item) => item.productId)
            .map((item) => ({
              productId: Number(item.productId),
              quantity: Number(item.quantity),
              unitCost: toNumber(item.unitCost)
            }))
        }
      })
      await load()
      setTransferForm((current) => ({ ...current, note: '', items: [createLineItem()] }))
    } catch (ex) {
      setError(ex.message || 'Unable to create transfer')
    } finally {
      setTransferBusy(false)
    }
  }

  async function runAction(endpoint) {
    setActionBusy(true)
    setError('')
    try {
      await apiFetch(endpoint, { method: 'POST', token })
      await load()
    } catch (ex) {
      setError(ex.message || 'Action failed')
    } finally {
      setActionBusy(false)
    }
  }

  async function rejectRequest(id) {
    setActionBusy(true)
    setError('')
    try {
      await apiFetch(`/stock-requests/${id}/reject`, {
        method: 'POST',
        token,
        body: { note: 'Rejected from operations screen' }
      })
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to reject request')
    } finally {
      setActionBusy(false)
    }
  }

  async function receiveSelectedTransfer(event) {
    event.preventDefault()
    if (!selectedTransfer) return
    setActionBusy(true)
    setError('')
    try {
      await apiFetch(`/stock-transfers/${selectedTransfer.id}/receive`, {
        method: 'POST',
        token,
        body: {
          note: receiveForm.note,
          items: receiveForm.items.map((item) => ({
            productId: Number(item.productId),
            quantityReceived: Number(item.quantityReceived)
          }))
        }
      })
      setSelectedTransfer(null)
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to receive transfer')
    } finally {
      setActionBusy(false)
    }
  }

  if (loading) {
    return <PageSection title="Loading operations" subtitle="Preparing requests and transfers." />
  }

  return (
    <div className="page-grid">
      <PageSection title="Create stock request" subtitle="Request inventory from the warehouse or store.">
        {error ? <div className="error-banner">{error}</div> : null}
        {!canCreateRequest ? (
          <div className="error-banner">Create at least one active warehouse/store and one active shop/branch location first.</div>
        ) : null}
        <form onSubmit={submitRequest} className="subgrid">
          <div className="form-grid">
            <Field label="Source location">
              <Select value={requestForm.sourceLocationId} onChange={(event) => setRequestForm({ ...requestForm, sourceLocationId: event.target.value })} disabled={!warehouseLocations.length}>
                {warehouseLocations.map((location) => (
                  <option key={location.id} value={location.id}>{location.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Destination location">
              <Select value={requestForm.destinationLocationId} onChange={(event) => setRequestForm({ ...requestForm, destinationLocationId: event.target.value })} disabled={!shopLocations.length}>
                {shopLocations.map((location) => (
                  <option key={location.id} value={location.id}>{location.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Note">
              <TextArea value={requestForm.note} onChange={(event) => setRequestForm({ ...requestForm, note: event.target.value })} />
            </Field>
          </div>

          <div className="subgrid">
            {requestForm.items.map((item, index) => (
              <div key={`request-item-${index}`} className="form-grid">
                <Field label={`Item ${index + 1}`}>
                  <Select value={item.productId} onChange={(event) => updateLineItem(setRequestForm, index, 'productId', event.target.value)}>
                    <option value="">Select product</option>
                    {products.map((product) => (
                      <option key={product.id} value={product.id}>{product.name}</option>
                    ))}
                  </Select>
                </Field>
                <Field label="Quantity">
                  <Input type="number" min="1" value={item.quantity} onChange={(event) => updateLineItem(setRequestForm, index, 'quantity', event.target.value)} />
                </Field>
                <Field label=" " hint=" ">
                  <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 0 }}>
                    <Button type="button" variant="secondary" icon={PlusCircle} onClick={() => addLineItem(setRequestForm)}>Add row</Button>
                    <Button type="button" variant="danger" icon={RotateCcw} onClick={() => removeLineItem(setRequestForm, index)}>Remove</Button>
                  </div>
                </Field>
              </div>
            ))}
          </div>

          <div className="form-actions">
            <Button type="submit" busy={requestBusy} icon={ArrowRightLeft} disabled={!canCreateRequest}>Save request</Button>
          </div>
        </form>
      </PageSection>

      <PageSection title="Stock requests" subtitle="Submit, approve, or reject request records.">
        <Table
          columns={['Request', 'Source', 'Destination', 'Status', 'Created', 'Actions']}
          rows={requests}
          rowKey="id"
          renderRow={(request) => (
            <tr key={request.id}>
              <td>{request.requestNumber}</td>
              <td>{request.sourceLocation?.name}</td>
              <td>{request.destinationLocation?.name}</td>
              <td><Badge tone={toneForStatus(request.status)}>{request.status}</Badge></td>
              <td>{formatDateTime(request.createdAt)}</td>
              <td>
                <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 0 }}>
                  <Button type="button" variant="secondary" icon={Send} busy={actionBusy} onClick={() => runAction(`/stock-requests/${request.id}/submit`)}>Submit</Button>
                  <Button type="button" variant="secondary" icon={CheckCircle2} busy={actionBusy} onClick={() => runAction(`/stock-requests/${request.id}/approve`)}>Approve</Button>
                  <Button type="button" variant="danger" icon={RotateCcw} busy={actionBusy} onClick={() => rejectRequest(request.id)}>Reject</Button>
                </div>
              </td>
            </tr>
          )}
        />
      </PageSection>

      <PageSection title="Create transfer" subtitle="Dispatch inventory between locations.">
        {error ? null : (!canCreateTransfer ? (
          <div className="error-banner">Create at least one active warehouse/store and one active shop/branch location first.</div>
        ) : null)}
        <form onSubmit={submitTransfer} className="subgrid">
          <div className="form-grid">
            <Field label="Source location">
              <Select value={transferForm.sourceLocationId} onChange={(event) => setTransferForm({ ...transferForm, sourceLocationId: event.target.value })} disabled={!warehouseLocations.length}>
                {warehouseLocations.map((location) => (
                  <option key={location.id} value={location.id}>{location.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Destination location">
              <Select value={transferForm.destinationLocationId} onChange={(event) => setTransferForm({ ...transferForm, destinationLocationId: event.target.value })} disabled={!shopLocations.length}>
                {shopLocations.map((location) => (
                  <option key={location.id} value={location.id}>{location.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Note">
              <TextArea value={transferForm.note} onChange={(event) => setTransferForm({ ...transferForm, note: event.target.value })} />
            </Field>
          </div>

          <div className="subgrid">
            {transferForm.items.map((item, index) => (
              <div key={`transfer-item-${index}`} className="form-grid">
                <Field label={`Item ${index + 1}`}>
                  <Select value={item.productId} onChange={(event) => updateLineItem(setTransferForm, index, 'productId', event.target.value)}>
                    <option value="">Select product</option>
                    {products.map((product) => (
                      <option key={product.id} value={product.id}>{product.name}</option>
                    ))}
                  </Select>
                </Field>
                <Field label="Quantity">
                  <Input type="number" min="1" value={item.quantity} onChange={(event) => updateLineItem(setTransferForm, index, 'quantity', event.target.value)} />
                </Field>
                <Field label="Unit cost">
                  <Input type="number" step="0.01" min="0" value={item.unitCost} onChange={(event) => updateLineItem(setTransferForm, index, 'unitCost', event.target.value)} />
                </Field>
                <Field label=" ">
                  <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 0 }}>
                    <Button type="button" variant="secondary" icon={PlusCircle} onClick={() => addLineItem(setTransferForm)}>Add row</Button>
                    <Button type="button" variant="danger" icon={RotateCcw} onClick={() => removeLineItem(setTransferForm, index)}>Remove</Button>
                  </div>
                </Field>
              </div>
            ))}
          </div>

          <div className="form-actions">
            <Button type="submit" busy={transferBusy} icon={ArrowRightLeft} disabled={!canCreateTransfer}>Save transfer</Button>
          </div>
        </form>
      </PageSection>

      <PageSection title="Stock transfers" subtitle="Approve, dispatch, and receive transfer documents.">
        <Table
          columns={['Transfer', 'Source', 'Destination', 'Status', 'Created', 'Actions']}
          rows={transfers}
          rowKey="id"
          renderRow={(transfer) => (
            <tr key={transfer.id}>
              <td>{transfer.transferNumber}</td>
              <td>{transfer.sourceLocation?.name}</td>
              <td>{transfer.destinationLocation?.name}</td>
              <td><Badge tone={toneForStatus(transfer.status)}>{transfer.status}</Badge></td>
              <td>{formatDateTime(transfer.createdAt)}</td>
              <td>
                <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 0 }}>
                  <Button type="button" variant="secondary" icon={CheckCircle2} busy={actionBusy} onClick={() => runAction(`/stock-transfers/${transfer.id}/approve`)}>Approve</Button>
                  <Button type="button" variant="secondary" icon={Send} busy={actionBusy} onClick={() => runAction(`/stock-transfers/${transfer.id}/dispatch`)}>Dispatch</Button>
                  <Button type="button" variant="secondary" icon={ArrowRightLeft} busy={actionBusy} onClick={() => setSelectedTransfer(transfer)}>Receive</Button>
                </div>
              </td>
            </tr>
          )}
        />
      </PageSection>

      {selectedTransfer ? (
        <PageSection
          title={`Receive ${selectedTransfer.transferNumber}`}
          subtitle="Edit received quantities before posting the receiving confirmation."
          actions={<Button type="button" variant="secondary" icon={RefreshCcw} onClick={() => setSelectedTransfer(null)}>Close</Button>}
        >
          <form className="subgrid" onSubmit={receiveSelectedTransfer}>
            {selectedTransfer.items.map((item, index) => (
              <div key={`receive-item-${item.id}`} className="form-grid">
                <Field label="Product">
                  <Input value={item.product?.name} disabled />
                </Field>
                <Field label="Sent quantity">
                  <Input value={item.quantity} disabled />
                </Field>
                <Field label="Received quantity">
                  <Input
                    type="number"
                    min="0"
                    max={item.quantity}
                    value={receiveForm.items[index]?.quantityReceived ?? item.quantity}
                    onChange={(event) => {
                      const items = [...receiveForm.items]
                      items[index] = { ...items[index], quantityReceived: event.target.value, productId: item.product.id }
                      setReceiveForm({ ...receiveForm, items })
                    }}
                  />
                </Field>
              </div>
            ))}

            <Field label="Receipt note">
              <TextArea value={receiveForm.note} onChange={(event) => setReceiveForm({ ...receiveForm, note: event.target.value })} />
            </Field>

            <div className="form-actions">
              <Button type="submit" busy={actionBusy} icon={CheckCircle2}>Confirm receipt</Button>
            </div>
          </form>
        </PageSection>
      ) : null}
    </div>
  )
}
