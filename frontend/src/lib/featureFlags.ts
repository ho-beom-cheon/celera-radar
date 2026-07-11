export function resolveBooleanFeatureFlag(value: string | undefined, defaultValue: boolean) {
  if (value === 'true') {
    return true;
  }
  if (value === 'false') {
    return false;
  }
  return defaultValue;
}

export const smartStoreMockEnabled = resolveBooleanFeatureFlag(
  import.meta.env.VITE_SMARTSTORE_MOCK_ENABLED,
  import.meta.env.DEV
);
