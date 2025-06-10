import axios from "axios";

const DEFAULT_TIMEOUT = 10000;

// Use environment variable for baseURL, with a fallback for local development
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api";

const commonConfig = {
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: DEFAULT_TIMEOUT,
  withCredentials: false
};

export const publicApi = axios.create({
  ...commonConfig,
  baseURL: commonConfig.baseURL
});

export const privateApi = axios.create({
  ...commonConfig,
  baseURL: `${commonConfig.baseURL}/v1`
});

// Token validation utility
const isTokenExpired = (token) => {
  if (!token) return true;

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const currentTime = Date.now() / 1000;
    return payload.exp < currentTime;
  } catch (error) {
    console.warn("Failed to parse token:", error);
    return true;
  }
};

// Authentication cleanup utility
const clearAuthenticationState = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("vaiTro");
  localStorage.removeItem("nguoiDung");
};

// Automatic logout utility
const performAutomaticLogout = (reason = "Session expired") => {
  console.warn(`Automatic logout triggered: ${reason}`);
  clearAuthenticationState();

  // Avoid infinite redirects by checking current location
  if (!window.location.pathname.includes('/login')) {
    window.location.href = "/login?expired=true";
  }
};

privateApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");

    if (token) {
      // Check if token is expired before making request
      if (isTokenExpired(token)) {
        performAutomaticLogout("Token expired");
        return Promise.reject(new Error("Token expired"));
      }

      config.headers.Authorization = `Bearer ${token}`;
    } else {
      console.warn("No token found - redirecting to login");
      performAutomaticLogout("Missing token");
      return Promise.reject(new Error("Missing authentication token"));
    }
    return config;
  },
  (error) => Promise.reject(error)
);

const handleError = (error) => {
  let errorMessage = 'An unexpected error occurred';

  if (error.response) {
    // Server responded with status code outside 2xx
    errorMessage = error.response.data?.message || error.response.statusText;

    // Handle authentication and authorization errors
    if (error.response.status === 401) {
      const errorCode = error.response.data?.code;

      if (errorCode === 'TOKEN_EXPIRED' || errorCode === 'TOKEN_INVALID') {
        performAutomaticLogout("Token expired or invalid");
      } else {
        performAutomaticLogout("Authentication failed");
      }
    } else if (error.response.status === 403) {
      const errorCode = error.response.data?.code;

      if (errorCode === 'ACCOUNT_INACTIVE') {
        performAutomaticLogout("Account is inactive");
      } else {
        console.warn("Access forbidden:", errorMessage);
      }
    }
  } else if (error.request) {
    // Request was made but no response received
    errorMessage = error.code === 'ECONNABORTED'
      ? 'Request timeout'
      : 'Network Error';
  }

  console.error("API Error:", errorMessage);
  return Promise.reject({
    message: errorMessage,
    status: error.response?.status,
    code: error.code,
    originalError: error
  });
};

[publicApi, privateApi].forEach(instance => {
  instance.interceptors.response.use(
    response => response,
    handleError
  );
});

// Export utilities for use in other modules
export { isTokenExpired, clearAuthenticationState, performAutomaticLogout };

export default {
  public: publicApi,
  private: privateApi
};
