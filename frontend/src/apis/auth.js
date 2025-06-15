import { publicApi, privateApi } from "./axiosAPI";
import { isTokenExpired, clearAuthenticationState } from "./axiosAPI";

const AuthService = {
  async login(taiKhoan, matKhau) {
    try {
      const response = await publicApi.post("/auth/login", {
        taiKhoan,
        matKhau
      });

      const { token, user } = response.data;

      if (token && user) {
        // Store token first so we can make authenticated requests
        localStorage.setItem("token", token);
        localStorage.setItem("vaiTro", user.vaiTro);

        // Fetch complete user data if user is staff or admin
        if (user.vaiTro === 'STAFF' || user.vaiTro === 'ADMIN') {
          try {
            const completeUserResponse = await privateApi.get(`/user/staff/${user.id}`);

            if (completeUserResponse.data) {
              const completeUser = completeUserResponse.data;
              localStorage.setItem("nguoiDung", JSON.stringify(completeUser));
            } else {
              localStorage.setItem("nguoiDung", JSON.stringify(user));
            }
          } catch {
            // If fetching complete user data fails, fall back to basic user data
            localStorage.setItem("nguoiDung", JSON.stringify(user));
          }
        } else {
          // For customers, store basic user data
          localStorage.setItem("nguoiDung", JSON.stringify(user));
        }
      }

      return token;
    } catch (error) {
      console.error("Login failed:", error.message);
      throw error;
    }
  },

  logout() {
    clearAuthenticationState();
  },

  isAuthenticated() {
    const token = localStorage.getItem("token");
    if (!token) return false;

    // Check if token is expired
    if (isTokenExpired(token)) {
      this.logout();
      return false;
    }

    return true;
  },

  getToken() {
    const token = localStorage.getItem("token");
    if (token && isTokenExpired(token)) {
      this.logout();
      return null;
    }
    return token;
  },

  getRole() {
    if (!this.isAuthenticated()) return null;
    return localStorage.getItem("vaiTro");
  },

  getUser() {
    if (!this.isAuthenticated()) return null;
    const userStr = localStorage.getItem("nguoiDung");
    try {
      return userStr ? JSON.parse(userStr) : null;
    } catch (error) {
      console.error("Failed to parse user data:", error);
      this.logout();
      return null;
    }
  },

  // Validate token with backend
  async validateToken() {
    try {
      const token = this.getToken();
      if (!token) return false;

      const response = await publicApi.post("/auth/validate-token", {}, {
        headers: { Authorization: `Bearer ${token}` }
      });

      return response.data.valid;
    } catch (error) {
      console.warn("Token validation failed:", error.message);
      this.logout();
      return false;
    }
  },

  // Check if current session is valid
  async checkSession() {
    if (!this.isAuthenticated()) return false;

    // For performance, only validate with backend occasionally
    const lastValidation = localStorage.getItem("lastTokenValidation");
    const now = Date.now();

    // Validate with backend every 5 minutes
    if (!lastValidation || (now - parseInt(lastValidation)) > 5 * 60 * 1000) {
      const isValid = await this.validateToken();
      if (isValid) {
        localStorage.setItem("lastTokenValidation", now.toString());
      }
      return isValid;
    }

    return true;
  }
};

export default AuthService;
