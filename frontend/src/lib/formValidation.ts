export function hasFormErrors(errors: object) {
  return Object.values(errors).some(Boolean);
}

export function isBlank(value: string) {
  return value.trim().length === 0;
}

export function parseFiniteNumber(value: string) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

export function blankToNumberFilter(value: string): number | '' {
  return isBlank(value) ? '' : Number(value);
}
