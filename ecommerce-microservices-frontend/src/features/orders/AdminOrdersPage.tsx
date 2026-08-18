import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Package, Clock, CheckCircle2, Truck, XCircle, ChevronDown, ExternalLink } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Select } from '@/components/ui/select-native';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { Pagination } from '@/components/ui/pagination';
import { useAdminOrders, useUpdateOrderStatus } from './useOrderQueries';
import type { OrderStatus } from './types';

const PAGE_SIZE = 10;

const STATUS_CONFIG: Record<string, { label: string; icon: React.ReactNode; color: string }> = {
  CREATED: { label: 'Bekliyor', icon: <Clock className="h-4 w-4" />, color: 'text-amber-500 bg-amber-500/10' },
  PENDING: { label: 'Bekliyor', icon: <Clock className="h-4 w-4" />, color: 'text-amber-500 bg-amber-500/10' },
  PREPARING: { label: 'Hazırlanıyor', icon: <Package className="h-4 w-4" />, color: 'text-blue-500 bg-blue-500/10' },
  SHIPPED: { label: 'Kargoya Verildi', icon: <Truck className="h-4 w-4" />, color: 'text-violet-500 bg-violet-500/10' },
  DELIVERED: { label: 'Teslim Edildi', icon: <CheckCircle2 className="h-4 w-4" />, color: 'text-emerald-500 bg-emerald-500/10' },
  CANCELLED: { label: 'İptal Edildi', icon: <XCircle className="h-4 w-4" />, color: 'text-destructive bg-destructive/10' },
};

export function AdminOrdersPage() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>('');
  
  const { data, isLoading, isError, error, refetch } = useAdminOrders({ 
    page, 
    size: PAGE_SIZE, 
    sort: 'createdAt,desc',
    status: statusFilter || undefined,
  });

  const updateStatus = useUpdateOrderStatus();
  const [expandedOrder, setExpandedOrder] = useState<string | null>(null);

  const handleStatusChange = (orderId: string, newStatus: OrderStatus) => {
    updateStatus.mutate({ id: orderId, payload: { status: newStatus } });
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">Sipariş Yönetimi</h1>
          {data && <p className="text-sm text-muted-foreground">{data.totalElements || ((data as any).orders?.length) || (Array.isArray(data) ? data.length : 0)} sipariş</p>}
        </div>
        
        <div className="flex items-center gap-2">
          <Select 
            value={statusFilter} 
            onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
            className="w-[180px]"
          >
            <option value="ALL">Tüm Durumlar</option>
            {Object.entries(STATUS_CONFIG).map(([val, cfg]) => (
              <option key={val} value={val}>{cfg.label}</option>
            ))}
          </Select>
        </div>
      </div>

      {/* Table */}
      {isError ? (
        <ErrorMessage error={error} onRetry={refetch} />
      ) : (
        <div className="rounded-xl border border-border/50 overflow-hidden bg-card">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/30 border-b border-border/50">
              <tr>
                <th className="px-4 py-3 font-medium text-muted-foreground">Sipariş No</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">Tarih</th>
                <th className="px-4 py-3 font-medium text-muted-foreground hidden sm:table-cell">Müşteri</th>
                <th className="px-4 py-3 font-medium text-muted-foreground text-right">Tutar</th>
                <th className="px-4 py-3 font-medium text-muted-foreground">Durum</th>
                <th className="px-4 py-3 w-10"></th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: PAGE_SIZE }).map((_, i) => (
                  <tr key={i} className="border-b border-border/30">
                    <td className="px-4 py-3"><Skeleton className="h-5 w-24" /></td>
                    <td className="px-4 py-3"><Skeleton className="h-5 w-32" /></td>
                    <td className="px-4 py-3 hidden sm:table-cell"><Skeleton className="h-5 w-32" /></td>
                    <td className="px-4 py-3 text-right"><Skeleton className="h-5 w-20 ml-auto" /></td>
                    <td className="px-4 py-3"><Skeleton className="h-8 w-28" /></td>
                    <td className="px-4 py-3"><Skeleton className="h-8 w-8" /></td>
                  </tr>
                ))
              ) : (() => {
                  const rawOrders = data?.content || (data as any)?.orders || (Array.isArray(data) ? data : null);
                  
                  if (!rawOrders) {
                    return (
                      <tr>
                        <td colSpan={6} className="px-4 py-12 text-center text-muted-foreground text-lg">
                          Loading...
                        </td>
                      </tr>
                    );
                  }
                  
                  if (!rawOrders.length) {
                    return (
                      <tr>
                        <td colSpan={6} className="px-4 py-12 text-center text-muted-foreground">
                          <Package className="h-8 w-8 mx-auto mb-3 opacity-40" />
                          Sipariş bulunamadı
                        </td>
                      </tr>
                    );
                  }
                  const filteredOrders = rawOrders.filter((o: any) => {
                    if (statusFilter === "ALL" || !statusFilter) return true;
                    return (o.status || o.orderStatus) === statusFilter;
                  }).sort((a: any, b: any) => {
                    const dateA = new Date(a.createdAt || a.orderDate || 0).getTime();
                    const dateB = new Date(b.createdAt || b.orderDate || 0).getTime();
                    return dateB - dateA;
                  });

                  if (!filteredOrders.length) {
                    return (
                      <tr>
                        <td colSpan={6} className="px-4 py-12 text-center text-muted-foreground">
                          <Package className="h-8 w-8 mx-auto mb-3 opacity-40" />
                          Seçili duruma ait sipariş bulunamadı
                        </td>
                      </tr>
                    );
                  }
                  
                  const formatDate = (dateStr: any) => dateStr ? new Date(dateStr).toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : "-";

                  return filteredOrders.map((order: any) => {
                    const totalAmount = typeof order?.totalAmount === 'object' ? (order?.totalAmount?.amount || 0) : Number(order?.totalAmount || order?.totalPrice || 0);
                    const status = order?.status || order?.orderStatus || "PENDING";
                    
                    const customerText = order.customerName || order.userEmail || (order.keycloakUserId ? `Müşteri (${order.keycloakUserId.substring(0, 8)}...)` : "Müşteri");
                    const addressText = order.deliveryAddress?.addressLine || order.addressLine || (order.addressId ? `Adres ID: ${order.addressId}` : "Adres bilgisi girilmedi");
                    const cityInfo = order.deliveryAddress?.city ? `${order.deliveryAddress.city}, ${order.deliveryAddress.district || ''}` : (order.shippingAddress?.city ? `${order.shippingAddress.city}, ${order.shippingAddress.district || ''}` : "-");
                    
                    const orderNumber = order?.orderNumber || order?.id || "-";
                    const orderDate = order.createdAt || order.orderDate ? formatDate(order.createdAt || order.orderDate) : "-";
                    
                    const cfg = STATUS_CONFIG[status as OrderStatus] || STATUS_CONFIG['PENDING'];
                    const isExpanded = expandedOrder === order.id;
                    
                    return (
                      <React.Fragment key={order.id || Math.random()}>
                        <tr className="border-b border-border/30 hover:bg-muted/10 transition-colors group">
                          <td className="px-4 py-3 font-mono text-xs">{orderNumber}</td>
                        <td className="px-4 py-3">{orderDate}</td>
                          <td className="px-4 py-3 hidden sm:table-cell truncate max-w-[150px]">
                            {customerText}
                          </td>
                          <td className="px-4 py-3 text-right font-semibold">
                            {totalAmount.toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                          </td>
                          <td className="px-4 py-3">
                            <Select 
                              value={status}
                              onChange={(e) => handleStatusChange(order.id, e.target.value as OrderStatus)}
                              className={`h-8 py-0 pl-2 pr-8 text-xs font-medium border-0 w-[140px] ${cfg.color}`}
                              disabled={updateStatus.isPending && updateStatus.variables?.id === order.id}
                            >
                              {Object.entries(STATUS_CONFIG).map(([val, c]) => (
                                <option key={val} value={val}>{c.label}</option>
                              ))}
                            </Select>
                          </td>
                          <td className="px-4 py-3 text-right">
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              className="h-8 w-8"
                              onClick={() => setExpandedOrder(isExpanded ? null : order.id)}
                            >
                              <ChevronDown className={`h-4 w-4 transition-transform ${isExpanded ? 'rotate-180' : ''}`} />
                            </Button>
                          </td>
                        </tr>
                        {isExpanded && (
                          <tr className="bg-muted/5 border-b border-border/30">
                            <td colSpan={6} className="px-4 py-4">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
                              <div>
                                <p className="font-semibold mb-2">Sipariş İçeriği ({order.items.length} Ürün)</p>
                                <div className="space-y-2">
                                  {order.items.map((item, idx) => (
                                    <div key={idx} className="flex justify-between items-center bg-background rounded-lg p-2 border border-border/50">
                                      <div className="flex items-center gap-2">
                                        <div className="w-8 h-8 rounded bg-muted overflow-hidden shrink-0">
                                          {item.imageUrl ? <img src={item.imageUrl} className="w-full h-full object-cover" alt="" /> : <Package className="w-4 h-4 m-2 opacity-30" />}
                                        </div>
                                        <div className="min-w-0">
                                          <p className="font-medium truncate text-xs">{item.productName}</p>
                                          <p className="text-[10px] text-muted-foreground">{item.quantity} adet x {item.unitPrice} ₺</p>
                                        </div>
                                      </div>
                                      <span className="font-semibold text-xs">{item.totalPrice} ₺</span>
                                    </div>
                                  ))}
                                </div>
                              </div>
                              <div className="space-y-4">
                                <div>
                                  <p className="font-semibold mb-1">Teslimat Adresi</p>
                                  <div className="bg-background rounded-lg p-3 border border-border/50 text-muted-foreground text-xs space-y-1">
                                    <p className="font-medium text-foreground">{customerText}</p>
                                    <p>{addressText}</p>
                                    <p>{cityInfo}</p>
                                  </div>
                                </div>
                                <div className="flex justify-end">
                                  <Button variant="outline" size="sm" asChild>
                                    <Link to={`/orders/${order.id}`}>
                                      Müşteri Görünümü <ExternalLink className="h-3 w-3 ml-2" />
                                    </Link>
                                  </Button>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                });
              })()}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <Pagination
          currentPage={data.number}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          pageSize={data.size}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
