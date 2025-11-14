export const currencySymbols = {
  USD: '$',
  EUR: '€',
  INR: '₹',
  GBP: '£',
  JPY: '¥',
  CAD: 'C$'
};

export const getCurrencySymbol = (currency) => {
  return currencySymbols[currency] || '$';
};

export const formatAmount = (amount, currency) => {
  const symbol = getCurrencySymbol(currency);
  return `${symbol}${parseFloat(amount).toFixed(2)}`;
};