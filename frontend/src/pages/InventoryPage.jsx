import { Pencil, PlusCircle, RefreshCcw, RotateCcw, Search, Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { apiFetch } from '../api/client'
import { Badge, Button, Field, Input, PageSection, Select, Table, TextArea } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { currencyOrDash, formatDateTime } from '../utils/format'

const EMPTY_PRODUCT_FORM = {
  name: '',
  sku: '',
  category: '',
  brand: '',
  unitOfMeasure: '',
  costPrice: '',
  sellingPrice: '',
  wholesalePrice: '',
  reorderLevel: '',
  supplierId: '',
  imageUrl: '',
  active: true
}

function toNullableNumber(value) {
  return value === '' || value === null || value === undefined ? null : Number(value)
}

const API_ORIGIN = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/api\/v1\/?$/, '')

function resolveImageUrl(value) {
  if (!value) return ''
  if (/^https?:\/\//i.test(value)) return value
  return `${API_ORIGIN}${String(value).startsWith('/') ? value : `/${value}`}`
}

export default function InventoryPage() {
  const { token, hasPermission } = useAuth()
  const [searchParams] = useSearchParams()
  const targetLocationId = searchParams.get('locationId') || ''
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [productError, setProductError] = useState('')
  const [productSuccess, setProductSuccess] = useState('')
  const [products, setProducts] = useState([])
  const [locations, setLocations] = useState([])
  const [suppliers, setSuppliers] = useState([])
  const [balances, setBalances] = useState([])
  const [editingProductId, setEditingProductId] = useState(null)
  const [productActionBusyId, setProductActionBusyId] = useState(null)
  const [barcodeInput, setBarcodeInput] = useState('')
  const [barcodeMessage, setBarcodeMessage] = useState('')
  const [locationFilter, setLocationFilter] = useState('')
  const [form, setForm] = useState({
    productId: '',
    locationId: '',
    quantity: 1,
    costPrice: '',
    note: ''
  })
  const [productForm, setProductForm] = useState(EMPTY_PRODUCT_FORM)
  const [productImageFile, setProductImageFile] = useState(null)
  const [productImageKey, setProductImageKey] = useState(0)
  const [busy, setBusy] = useState(false)
  const [productBusy, setProductBusy] = useState(false)

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, targetLocationId])

  const canManageCatalog = hasPermission('catalog:manage')
  const activeProducts = useMemo(() => products.filter((product) => product.active), [products])
  const hasProducts = activeProducts.length > 0
  const activeLocations = useMemo(() => locations.filter((location) => location.active), [locations])
  const canAddInventory = hasProducts && activeLocations.length > 0
  const selectedLocation = useMemo(
    () => activeLocations.find((location) => String(location.id) === String(form.locationId)) || null,
    [activeLocations, form.locationId]
  )

  const filteredBalances = useMemo(() => {
    if (!locationFilter) return balances
    return balances.filter((balance) => String(balance.location?.id) === locationFilter)
  }, [balances, locationFilter])

  async function load() {
    setLoading(true)
    setError('')
    setBarcodeMessage('')
    try {
      const [productList, locationList, supplierList, balanceList] = await Promise.all([
        apiFetch('/products', { token }),
        apiFetch('/locations', { token }),
        apiFetch('/suppliers', { token }),
        apiFetch('/inventory/balances', { token })
      ])
      setProducts(productList)
      setLocations(locationList)
      setSuppliers(supplierList)
      setBalances(balanceList)

      const nextActiveLocations = locationList.filter((location) => location.active)
      const nextActiveProducts = productList.filter((product) => product.active)
      const nextActiveLocationIds = new Set(nextActiveLocations.map((location) => String(location.id)))
      const nextActiveProductIds = new Set(nextActiveProducts.map((product) => String(product.id)))
      const preferredLocationId = targetLocationId && nextActiveLocationIds.has(targetLocationId)
        ? targetLocationId
        : String(nextActiveLocations[0]?.id || '')

      setForm((current) => {
        const currentProductId = String(current.productId || '')
        const nextProductId = currentProductId && nextActiveProductIds.has(currentProductId)
          ? currentProductId
          : String(nextActiveProducts[0]?.id || '')
        const nextProduct = nextActiveProducts.find((product) => String(product.id) === nextProductId)

        return {
          ...current,
          productId: nextProductId,
          locationId: targetLocationId && nextActiveLocationIds.has(targetLocationId)
            ? targetLocationId
            : (current.locationId && nextActiveLocationIds.has(String(current.locationId))
              ? String(current.locationId)
              : preferredLocationId),
          costPrice: currentProductId && nextActiveProductIds.has(currentProductId)
            ? current.costPrice
            : String(nextProduct?.costPrice ?? '')
        }
      })

      setLocationFilter((current) => {
        if (targetLocationId && nextActiveLocationIds.has(targetLocationId)) {
          return targetLocationId
        }
        return current && nextActiveLocationIds.has(String(current)) ? String(current) : preferredLocationId
      })
    } catch (ex) {
      setError(ex.message || 'Unable to load inventory data')
    } finally {
      setLoading(false)
    }
  }

  async function handleReceive(event) {
    event.preventDefault()
    if (!canAddInventory) {
      setError('Create at least one active product and one active location first.')
      return
    }
    setBusy(true)
    setError('')
    try {
      await apiFetch('/inventory/receive', {
        method: 'POST',
        token,
        body: {
          productId: Number(form.productId),
          locationId: Number(form.locationId),
          quantity: Number(form.quantity),
          costPrice: Number(form.costPrice),
          note: form.note
        }
      })
      await load()
      setForm((current) => ({ ...current, quantity: 1, note: '' }))
    } catch (ex) {
      setError(ex.message || 'Stock receipt failed')
    } finally {
      setBusy(false)
    }
  }

  function beginEditProduct(product) {
    setEditingProductId(product.id)
    setProductForm({
      name: product.name || '',
      sku: product.sku || '',
      category: product.category || '',
      brand: product.brand || '',
      unitOfMeasure: product.unitOfMeasure || '',
      costPrice: product.costPrice ?? '',
      sellingPrice: product.sellingPrice ?? '',
      wholesalePrice: product.wholesalePrice ?? '',
      reorderLevel: product.reorderLevel ?? '',
      supplierId: product.supplier?.id || '',
      imageUrl: product.imageUrl || '',
      active: Boolean(product.active)
    })
    setProductImageFile(null)
    setProductImageKey((current) => current + 1)
    setProductError('')
    setProductSuccess('')
  }

  function cancelProductEdit() {
    setEditingProductId(null)
    setProductForm(EMPTY_PRODUCT_FORM)
    setProductImageFile(null)
    setProductImageKey((current) => current + 1)
    setProductError('')
    setProductSuccess('')
  }

  function handleBarcodeScan(value) {
    const scanned = String(value || '').trim().toLowerCase()
    if (!scanned) return

    const product = activeProducts.find((item) =>
      [item.barcode, item.sku]
        .filter(Boolean)
        .some((field) => String(field).trim().toLowerCase() === scanned)
    )

    if (!product) {
      setBarcodeMessage('')
      setError(`No active product matched barcode "${value}".`)
      return
    }

    setForm((current) => ({
      ...current,
      productId: String(product.id),
      costPrice: String(product.costPrice ?? current.costPrice ?? '')
    }))
    setBarcodeInput('')
    setError('')
    setBarcodeMessage(`Matched ${product.name}`)
  }

  async function handleSubmitProduct(event) {
    event.preventDefault()
    if (!canManageCatalog) {
      setProductError('Product creation requires catalog management access.')
      return
    }

    setProductBusy(true)
    setProductError('')
    setProductSuccess('')
    try {
      let imageUrl = ''
      if (productImageFile) {
        const uploadResponse = await apiFetch('/products/image', {
          method: 'POST',
          token,
          body: (() => {
            const data = new FormData()
            data.append('image', productImageFile)
            return data
          })()
        })
        imageUrl = uploadResponse.url
      }

      const payload = {
        name: productForm.name.trim(),
        sku: productForm.sku.trim(),
        category: productForm.category.trim() || null,
        brand: productForm.brand.trim() || null,
        unitOfMeasure: productForm.unitOfMeasure.trim() || null,
        costPrice: Number(productForm.costPrice),
        sellingPrice: Number(productForm.sellingPrice),
        wholesalePrice: toNullableNumber(productForm.wholesalePrice),
        reorderLevel: toNullableNumber(productForm.reorderLevel),
        supplierId: toNullableNumber(productForm.supplierId),
        active: Boolean(productForm.active),
        imageUrl: imageUrl || productForm.imageUrl || null
      }

      const saved = await apiFetch(editingProductId ? `/products/${editingProductId}` : '/products', {
        method: editingProductId ? 'PUT' : 'POST',
        token,
        body: payload
      })

      setProductForm(EMPTY_PRODUCT_FORM)
      setProductImageFile(null)
      setProductImageKey((current) => current + 1)
      setEditingProductId(null)
      setProductSuccess(editingProductId
        ? `Product ${saved.name} updated`
        : `Product ${saved.name} created with barcode ${saved.barcode}`)
      await load()
      if (saved.active) {
        setForm((current) => ({
          ...current,
          productId: String(saved.id),
          costPrice: String(saved.costPrice ?? current.costPrice ?? '')
        }))
      }
    } catch (ex) {
      setProductError(ex.message || 'Unable to save product')
    } finally {
      setProductBusy(false)
    }
  }

  async function handleProductActive(product, active) {
    setProductActionBusyId(product.id)
    setProductError('')
    setProductSuccess('')
    try {
      await apiFetch(`/products/${product.id}`, {
        method: 'PUT',
        token,
        body: {
          name: product.name,
          sku: product.sku,
          category: product.category,
          brand: product.brand,
          unitOfMeasure: product.unitOfMeasure,
          costPrice: product.costPrice,
          sellingPrice: product.sellingPrice,
          wholesalePrice: product.wholesalePrice,
          reorderLevel: product.reorderLevel,
          supplierId: product.supplier?.id || null,
          active,
          imageUrl: product.imageUrl || null
        }
      })
      setProductSuccess(active ? `${product.name} reactivated` : `${product.name} deactivated`)
      if (editingProductId === product.id && !active) {
        cancelProductEdit()
      }
      await load()
    } catch (ex) {
      setProductError(ex.message || 'Unable to update product')
    } finally {
      setProductActionBusyId(null)
    }
  }

  async function handleDeleteProduct(product) {
    setProductActionBusyId(product.id)
    setProductError('')
    setProductSuccess('')
    try {
      await apiFetch(`/products/${product.id}`, {
        method: 'DELETE',
        token
      })
      setProductSuccess(`${product.name} deactivated`)
      if (editingProductId === product.id) {
        cancelProductEdit()
      }
      await load()
    } catch (ex) {
      setProductError(ex.message || 'Unable to deactivate product')
    } finally {
      setProductActionBusyId(null)
    }
  }

  if (loading) {
    return <PageSection title="Loading inventory" subtitle="Fetching products, balances, and locations." />
  }

  return (
    <div className="page-grid">
      {canManageCatalog ? (
        <PageSection
          title="Create product"
          subtitle="Add or edit a catalog item before you receive stock into a location. Barcode is generated automatically."
        >
          {productError ? <div className="error-banner">{productError}</div> : null}
          {productSuccess ? <div className="success-banner">{productSuccess}</div> : null}
          <form onSubmit={handleSubmitProduct} className="form-grid">
            <Field label="Name">
              <Input
                value={productForm.name}
                onChange={(event) => setProductForm({ ...productForm, name: event.target.value })}
                required
                placeholder="1kg Sugar"
              />
            </Field>
            <Field label="SKU">
              <Input
                value={productForm.sku}
                onChange={(event) => setProductForm({ ...productForm, sku: event.target.value })}
                required
                placeholder="SKU-SUGAR-1KG"
              />
            </Field>
            <Field label="Category">
              <Input
                value={productForm.category}
                onChange={(event) => setProductForm({ ...productForm, category: event.target.value })}
                placeholder="Groceries"
              />
            </Field>
            <Field label="Brand">
              <Input
                value={productForm.brand}
                onChange={(event) => setProductForm({ ...productForm, brand: event.target.value })}
                placeholder="Keen"
              />
            </Field>
            <Field label="Unit">
              <Input
                value={productForm.unitOfMeasure}
                onChange={(event) => setProductForm({ ...productForm, unitOfMeasure: event.target.value })}
                placeholder="bag"
              />
            </Field>
            <Field label="Cost price">
              <Input
                type="number"
                step="0.01"
                min="0"
                value={productForm.costPrice}
                onChange={(event) => setProductForm({ ...productForm, costPrice: event.target.value })}
                required
              />
            </Field>
            <Field label="Selling price">
              <Input
                type="number"
                step="0.01"
                min="0"
                value={productForm.sellingPrice}
                onChange={(event) => setProductForm({ ...productForm, sellingPrice: event.target.value })}
                required
              />
            </Field>
            <Field label="Wholesale price">
              <Input
                type="number"
                step="0.01"
                min="0"
                value={productForm.wholesalePrice}
                onChange={(event) => setProductForm({ ...productForm, wholesalePrice: event.target.value })}
              />
            </Field>
            <Field label="Reorder level">
              <Input
                type="number"
                min="0"
                value={productForm.reorderLevel}
                onChange={(event) => setProductForm({ ...productForm, reorderLevel: event.target.value })}
              />
            </Field>
            <Field label="Supplier">
              <Select
                value={productForm.supplierId}
                onChange={(event) => setProductForm({ ...productForm, supplierId: event.target.value })}
              >
                <option value="">No supplier</option>
                {suppliers.map((supplier) => (
                  <option key={supplier.id} value={supplier.id}>{supplier.name}</option>
                ))}
              </Select>
            </Field>
            <Field
              label="Image"
              hint={
                productImageFile
                  ? `Selected: ${productImageFile.name}`
                  : productForm.imageUrl
                    ? 'Current image will be kept unless you upload a new one.'
                    : 'Upload a product image.'
              }
            >
              <Input
                key={productImageKey}
                type="file"
                accept="image/*"
                onChange={(event) => setProductImageFile(event.target.files?.[0] || null)}
              />
            </Field>
            <Field label="Active">
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, minHeight: 40 }}>
                <input
                  type="checkbox"
                  checked={productForm.active}
                  onChange={(event) => setProductForm({ ...productForm, active: event.target.checked })}
                />
                <span>Enabled</span>
              </label>
            </Field>
            <div className="form-actions" style={{ justifyContent: 'flex-start', gridColumn: '1 / -1' }}>
              <Button type="submit" busy={productBusy} icon={editingProductId ? Pencil : PlusCircle}>
                {editingProductId ? 'Update product' : 'Create product'}
              </Button>
              {editingProductId ? (
                <Button type="button" variant="secondary" icon={X} onClick={cancelProductEdit} disabled={productBusy}>
                  Cancel
                </Button>
              ) : null}
            </div>
          </form>
        </PageSection>
      ) : null}

      <PageSection
        title="Add inventory"
        subtitle={
          selectedLocation
            ? `Add stock directly into ${selectedLocation.name}.`
            : 'Receive supplier deliveries or add starting stock into a selected store, shop, branch, or warehouse.'
        }
        actions={<Button type="button" variant="secondary" icon={RefreshCcw} onClick={load}>Refresh</Button>}
      >
        {error ? <div className="error-banner">{error}</div> : null}
        {barcodeMessage ? <div className="success-banner">{barcodeMessage}</div> : null}
        {!hasProducts ? (
          <div className="error-banner">Create at least one active product first, then add stock into a location.</div>
        ) : null}
        {!activeLocations.length ? (
          <div className="error-banner">Create at least one active location first, then add stock into it.</div>
        ) : null}
        <form onSubmit={handleReceive} className="auth-grid">
          <div className="form-grid">
            <div style={{ gridColumn: '1 / -1' }}>
              <Field label="Scan barcode" hint="Scan a barcode or type a SKU, then press Enter to select the product.">
                <Input
                  value={barcodeInput}
                  onChange={(event) => {
                    setBarcodeInput(event.target.value)
                    if (barcodeMessage) {
                      setBarcodeMessage('')
                    }
                  }}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault()
                      handleBarcodeScan(barcodeInput)
                    }
                  }}
                  placeholder="Scan barcode or SKU"
                  autoComplete="off"
                />
              </Field>
              <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 8 }}>
                <Button
                  type="button"
                  variant="secondary"
                  icon={Search}
                  onClick={() => handleBarcodeScan(barcodeInput)}
                  disabled={!products.length}
                >
                  Match barcode
                </Button>
              </div>
            </div>
            <Field label="Product">
              <Select
                value={form.productId}
                onChange={(event) => setForm({ ...form, productId: event.target.value })}
                disabled={!hasProducts}
              >
                {activeProducts.map((product) => (
                  <option key={product.id} value={product.id}>{product.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Location">
              <Select
                value={form.locationId}
                onChange={(event) => setForm({ ...form, locationId: event.target.value })}
                disabled={!activeLocations.length}
              >
                {activeLocations.map((location) => (
                  <option key={location.id} value={location.id}>{location.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Quantity">
              <Input
                type="number"
                min="1"
                value={form.quantity}
                onChange={(event) => setForm({ ...form, quantity: event.target.value })}
              />
            </Field>
            <Field label="Cost price">
              <Input
                type="number"
                step="0.01"
                min="0"
                value={form.costPrice}
                onChange={(event) => setForm({ ...form, costPrice: event.target.value })}
              />
            </Field>
            <Field label="Filter location">
              <Select value={locationFilter} onChange={(event) => setLocationFilter(event.target.value)}>
                <option value="">All locations</option>
                {activeLocations.map((location) => (
                  <option key={location.id} value={location.id}>{location.name}</option>
                ))}
              </Select>
            </Field>
            <Field label="Note">
              <TextArea value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} />
            </Field>
          </div>

          <div className="form-actions">
            <Button type="submit" busy={busy} icon={PlusCircle} disabled={!canAddInventory}>
              Add inventory
            </Button>
          </div>
        </form>
      </PageSection>

      <PageSection title="Inventory balances" subtitle="Current quantity by product and location.">
        <Table
          columns={['Product', 'Location', 'Qty', 'Cost', 'Reorder', 'Updated']}
          rows={filteredBalances}
          rowKey="id"
          renderRow={(balance) => (
            <tr key={balance.id}>
              <td>{balance.product?.name}</td>
              <td>{balance.location?.name}</td>
              <td>
                <Badge tone={balance.quantityOnHand <= (balance.product?.reorderLevel || 0) ? 'warning' : 'teal'}>
                  {balance.quantityOnHand}
                </Badge>
              </td>
              <td>{currencyOrDash(balance.product?.costPrice)}</td>
              <td>{balance.product?.reorderLevel ?? 0}</td>
              <td>{formatDateTime(balance.updatedAt)}</td>
            </tr>
          )}
        />
      </PageSection>

      <PageSection title="Catalog snapshot" subtitle="Products and suppliers currently in the system.">
        <Table
          columns={['Image', 'Product', 'SKU', 'Barcode', 'Supplier', 'Price', 'Status', 'Actions']}
          rows={products}
          rowKey="id"
          renderRow={(product) => (
            <tr key={product.id}>
              <td>
                {product.imageUrl ? (
                  <img
                    src={resolveImageUrl(product.imageUrl)}
                    alt={product.name}
                    style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 6, border: '1px solid var(--line)' }}
                  />
                ) : (
                  '-'
                )}
              </td>
              <td>{product.name}</td>
              <td>{product.sku}</td>
              <td>{product.barcode || '-'}</td>
              <td>{product.supplier?.name || suppliers.find((supplier) => supplier.id === product.supplierId)?.name || '-'}</td>
              <td>{currencyOrDash(product.sellingPrice)}</td>
              <td>
                <Badge tone={product.active ? 'teal' : 'danger'}>
                  {product.active ? 'Active' : 'Inactive'}
                </Badge>
              </td>
              <td>
                <div className="form-actions" style={{ justifyContent: 'flex-start', marginTop: 0 }}>
                  <Button
                    type="button"
                    variant="secondary"
                    icon={Pencil}
                    className="btn-sm"
                    onClick={() => beginEditProduct(product)}
                    disabled={productBusy || productActionBusyId !== null}
                  >
                    Edit
                  </Button>
                  {product.active ? (
                    <Button
                      type="button"
                      variant="danger"
                      icon={Trash2}
                      className="btn-sm"
                      busy={productActionBusyId === product.id}
                      onClick={() => handleDeleteProduct(product)}
                      disabled={productBusy || productActionBusyId !== null}
                    >
                      Deactivate
                    </Button>
                  ) : (
                    <Button
                      type="button"
                      variant="secondary"
                      icon={RotateCcw}
                      className="btn-sm"
                      busy={productActionBusyId === product.id}
                      onClick={() => handleProductActive(product, true)}
                      disabled={productBusy || productActionBusyId !== null}
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
