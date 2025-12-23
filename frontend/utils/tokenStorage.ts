const TOKEN_KEY = "collab_token";

const isBrowser = () => typeof window !== "undefined";

export const tokenStorage = {
  getToken(): string | null {
    if (!isBrowser()) return null;
    return window.localStorage.getItem(TOKEN_KEY);
  },
  setToken(token: string) {
    if (!isBrowser()) return;
    window.localStorage.setItem(TOKEN_KEY, token);
  },
  clear() {
    if (!isBrowser()) return;
    window.localStorage.removeItem(TOKEN_KEY);
  },
};
