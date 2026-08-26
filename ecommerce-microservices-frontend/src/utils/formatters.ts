export const formatOrderNumber = (orderId: string | undefined | null): string => {
  if (!orderId) return 'ORD-000000';
  if (orderId.startsWith('ORD-')) return orderId;
  let hash = 0;
  for (let i = 0; i < orderId.length; i++) {
    hash = (hash << 5) - hash + orderId.charCodeAt(i);
    hash |= 0;
  }
  const sixDigit = Math.abs(hash % 900000) + 100000;
  return `ORD-${sixDigit}`;
};
