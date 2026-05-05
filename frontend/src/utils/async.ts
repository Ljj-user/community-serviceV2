export async function withLoading<T>(
  loading: { value: boolean },
  task: () => Promise<T>,
): Promise<T> {
  loading.value = true
  try {
    return await task()
  } finally {
    loading.value = false
  }
}
