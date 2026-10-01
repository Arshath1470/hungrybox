export class ApiError extends Error {
  constructor(status, message, fieldErrors = null) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

function parse(text) {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

/**
 * Thin fetch wrapper. Services answer errors with RFC 9457 problem details,
 * so `detail` and `errors` become the message and per-field messages.
 */
export async function api(path, { method = 'GET', body, token } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  let response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'HungryBox could not be reached. Check your connection and try again.');
  }

  const text = await response.text();
  const data = text ? parse(text) : null;
  if (!response.ok) {
    const message =
      data?.detail ||
      (response.status === 503 ? 'That service is starting up. Try again in a moment.' : 'Request failed');
    throw new ApiError(response.status, message, data?.errors ?? null);
  }
  return data;
}
