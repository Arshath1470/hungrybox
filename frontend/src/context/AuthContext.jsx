import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, ApiError } from '../api/client.js';

const TOKEN_KEY = 'hungrybox.token';
const AuthContext = createContext(null);

function readToken() {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function writeToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // storage unavailable (private mode); the session simply won't persist
  }
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(readToken);
  const [user, setUser] = useState(null);
  const [checking, setChecking] = useState(Boolean(readToken()));

  const logout = useCallback(() => {
    writeToken(null);
    setToken(null);
    setUser(null);
  }, []);

  const acceptSession = useCallback((session) => {
    writeToken(session.token);
    setToken(session.token);
    setUser(session.user);
    return session.user;
  }, []);

  useEffect(() => {
    if (!token || user) {
      setChecking(false);
      return;
    }
    let cancelled = false;
    api('/api/users/me', { token })
      .then((me) => !cancelled && setUser(me))
      .catch(() => !cancelled && logout())
      .finally(() => !cancelled && setChecking(false));
    return () => {
      cancelled = true;
    };
  }, [token, user, logout]);

  const login = useCallback(
    (email, password) => api('/api/users/login', { method: 'POST', body: { email, password } }).then(acceptSession),
    [acceptSession],
  );

  const register = useCallback(
    (form) => api('/api/users/register', { method: 'POST', body: form }).then(acceptSession),
    [acceptSession],
  );

  /** Authenticated request; an expired session signs the user out. */
  const authed = useCallback(
    async (path, options = {}) => {
      try {
        return await api(path, { ...options, token });
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) logout();
        throw error;
      }
    },
    [token, logout],
  );

  const value = useMemo(
    () => ({
      user,
      token,
      checking,
      isAdmin: user?.role === 'ADMIN',
      login,
      register,
      logout,
      authed,
      setUser,
    }),
    [user, token, checking, login, register, logout, authed],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
