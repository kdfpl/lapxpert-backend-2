import { publicApi } from "./axiosAPI";

const AuthService = {
  async login(taiKhoan, matKhau) {
    try {
      const response = await publicApi.post("/auth/login", {
        taiKhoan,
        matKhau
      });

      const { token, user } = response.data;

      if (token && user) {
        localStorage.setItem("token", token);
        localStorage.setItem("vaiTro", user.vaiTro);
        localStorage.setItem("nguoiDung", JSON.stringify(user));
      }

      return token;
    } catch (error) {
      console.error("Login failed:", error.message);
      throw error;
    }
  },

  logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("vaiTro");
    localStorage.removeItem("nguoiDung");
  },

  isAuthenticated() {
    return !!localStorage.getItem("token");
  },

  getToken() {
    return localStorage.getItem("token");
  },

  getRole() {
    return localStorage.getItem("vaiTro");
  }
};

export default AuthService;
