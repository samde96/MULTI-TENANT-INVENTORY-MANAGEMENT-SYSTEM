import { PlusCircle, RefreshCcw, Search, ShoppingCart, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { apiFetch } from '../api/client'
import { Badge, Button, Field, Input, PageSection, Select, Table, TextArea } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { formatMoney } from '../utils/format'

export default function PosPage() {
  const { token } = useAuth()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [products, setProducts] = useState([])
  const [balances, setBalances] = useState([])
  const [locations, setLocations] = useState([])
  const [selectedLocationId, setSelectedLocationId] = useState('')
  const [barcode, setBarcode] = useState('')
  const [query, setQuery] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [note, setNote] = useState('')
  const [cart, setCart] = useState([])
  const [busy, setBusy] = useState(false)
  const [receipt, setReceipt] = useState(null)

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token])

  const activeLocations = useMemo(() => locations.filter((location) => location.active), [locations])
  const activeProducts = useMemo(() => products.filter((product) => product.active), [products])

  const stockMap = useMemo(() => {
    const map = new Map()
    balances.forEach((balance) => {
      if (!balance.location?.id || !balance.product?.id) return
      map.set(`${balance.location.id}:${balance.product.id}`, balance.quantityOnHand)
    })
    return map
  }, [balances])

  const filteredProducts = useMemo(() => {
    const search = query.trim().toLowerCase()
    if (!search) return activeProducts
    return activeProducts.filter((product) =>
      [product.name, product.sku, product.barcode, product.category, product.brand]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(search))
    )
  }, [activeProducts, query])

  const total = cart.reduce((sum, item) => sum + item.quantity * Number(item.unitPrice || 0), 0)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [productList, locationList, balanceList] = await Promise.all([
        apiFetch('/products', { token }),
        apiFetch('/locations', { token }),
        apiFetch('/inventory/balances', { token })
      ])
      setProducts(productList)
      setLocations(locationList)
      setBalances(balanceList)
      const nextActiveLocations = locationList.filter((location) => location.active)
      setSelectedLocationId((current) => current || nextActiveLocations.find((location) => String(location.type).includes('SHOP'))?.id || nextActiveLocations[0]?.id || '')
    } catch (ex) {
      setError(ex.message || 'Unable to load POS data')
    } finally {
      setLoading(false)
    }
  }

  function addToCart(product) {
    const key = String(product.id)
    setCart((current) => {
      const existing = current.find((item) => item.productId === key)
      if (existing) {
        return current.map((item) => item.productId === key ? { ...item, quantity: item.quantity + 1 } : item)
      }
      return [
        ...current,
        {
          productId: key,
          name: product.name,
          quantity: 1,
          unitPrice: product.sellingPrice,
          stock: stockMap.get(`${selectedLocationId}:${product.id}`) ?? 0
        }
      ]
    })
  }

  function addByBarcode() {
    const value = barcode.trim().toLowerCase()
    if (!value) return
    const product = activeProducts.find((item) =>
      [item.barcode, item.sku, item.name].filter(Boolean).some((field) => String(field).toLowerCase() === value)
    )
    if (product) {
      addToCart(product)
      setBarcode('')
    }
  }

  async function checkout(event) {
    event.preventDefault()
    if (!cart.length || !selectedLocationId) {
      setError('Create and select an active location before completing a sale.')
      return
    }
    setBusy(true)
    setError('')
    try {
      const sale = await apiFetch('/sales', {
        method: 'POST',
        token,
        body: {
          locationId: Number(selectedLocationId),
          paymentMethod,
          note,
          items: cart.map((item) => ({
            productId: Number(item.productId),
            quantity: Number(item.quantity),
            unitPrice: Number(item.unitPrice)
          }))
        }
      })
      setReceipt(sale)
      setCart([])
      setNote('')
      await load()
    } catch (ex) {
      setError(ex.message || 'Unable to complete sale')
    } finally {
      setBusy(false)
    }
  }

  if (loading) {
    return <PageSection title="Loading POS" subtitle="Preparing checkout and stock data." />
  }

  return (
    <div className="page-grid">
      <PageSection title="Checkout" subtitle="Barcode search, cart control, and payment capture.">
        {error ? <div className="error-banner">{error}</div> : null}
        {!activeLocations.length ? (
          <div className="error-banner">Create an active shop location first, then open the POS.</div>
        ) : null}
        {receipt ? (
          <div className="badge badge-teal" style={{ marginBottom: 12 }}>
            <ShoppingCart size={14} />
            <span>Receipt {receipt.receiptNumber} created for {formatMoney(receipt.totalAmount)}</span>
          </div>
        ) : null}

        <div className="form-grid">
          <Field label="Location">
            <Select value={selectedLocationId} onChange={(event) => setSelectedLocationId(event.target.value)} disabled={!activeLocations.length}>
              {activeLocations.map((location) => (
                <option key={location.id} value={location.id}>{location.name}</option>
              ))}
            </Select>
          </Field>
              <Field label="Barcode or SKU">
                <Input
                  value={barcode}
                  onChange={(event) => setBarcode(event.target.value)}
                  onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  addByBarcode()
                }
                  }}
                  placeholder="Scan or type barcode"
                />
              </Field>
          <Field label="Search products">
            <Input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Filter catalog" />
          </Field>
        </div>

        <div className="form-actions" style={{ justifyContent: 'flex-start' }}>
          <Button type="button" variant="secondary" icon={Search} onClick={addByBarcode}>Add scanned item</Button>
          <Button type="button" variant="secondary" icon={RefreshCcw} onClick={load}>Refresh stock</Button>
        </div>
      </PageSection>

      <div className="split-grid">
        <PageSection title="Products" subtitle="Tap a product to add it to the cart.">
          <Table
            columns={['Product', 'SKU', 'Stock', 'Price', 'Action']}
            rows={filteredProducts.slice(0, 12)}
            rowKey="id"
            renderRow={(product) => (
              <tr key={product.id}>
                <td>{product.name}</td>
                <td>{product.sku}</td>
                <td>
                  <Badge tone={(stockMap.get(`${selectedLocationId}:${product.id}`) ?? 0) > 0 ? 'teal' : 'warning'}>
                    {stockMap.get(`${selectedLocationId}:${product.id}`) ?? 0}
                  </Badge>
                </td>
                <td>{formatMoney(product.sellingPrice)}</td>
                <td>
                  <Button type="button" variant="secondary" icon={PlusCircle} className="btn-sm" onClick={() => addToCart(product)}>
                    Add
                  </Button>
                </td>
              </tr>
            )}
          />
        </PageSection>

        <PageSection title="Cart" subtitle="Review quantities before posting the sale.">
          <form onSubmit={checkout} className="subgrid">
            <Table
              columns={['Item', 'Qty', 'Price', 'Line total', 'Remove']}
              rows={cart}
              rowKey="productId"
              renderRow={(item, index) => (
                <tr key={item.productId}>
                  <td>{item.name}</td>
                  <td>
                    <Input
                      type="number"
                      min="1"
                      value={item.quantity}
                      onChange={(event) => {
                        const value = Number(event.target.value)
                        setCart((current) => current.map((cartItem) => cartItem.productId === item.productId ? { ...cartItem, quantity: value } : cartItem))
                      }}
                    />
                  </td>
                  <td>
                    <Input
                      type="number"
                      step="0.01"
                      min="0"
                      value={item.unitPrice}
                      onChange={(event) => {
                        const value = Number(event.target.value)
                        setCart((current) => current.map((cartItem) => cartItem.productId === item.productId ? { ...cartItem, unitPrice: value } : cartItem))
                      }}
                    />
                  </td>
                  <td>{formatMoney(item.quantity * Number(item.unitPrice || 0))}</td>
                  <td>
                    <Button
                      type="button"
                      variant="danger"
                      icon={Trash2}
                      className="btn-sm"
                      onClick={() => setCart((current) => current.filter((cartItem) => cartItem.productId !== item.productId))}
                    >
                      Remove
                    </Button>
                  </td>
                </tr>
              )}
            />

            <div className="form-grid">
              <Field label="Payment method">
                <Select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value)}>
                  <option value="CASH">Cash</option>
                  <option value="MPESA">M-Pesa</option>
                  <option value="CARD">Card</option>
                  <option value="SPLIT">Split</option>
                </Select>
              </Field>
              <Field label="Note">
                <TextArea value={note} onChange={(event) => setNote(event.target.value)} />
              </Field>
              <Field label="Total">
                <Input value={formatMoney(total)} disabled />
              </Field>
            </div>

            <div className="form-actions">
              <Button type="submit" busy={busy} icon={ShoppingCart} disabled={!cart.length || !selectedLocationId}>Complete sale</Button>
            </div>
          </form>
        </PageSection>
      </div>
    </div>
  )
}
