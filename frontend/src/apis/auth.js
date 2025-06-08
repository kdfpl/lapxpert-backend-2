import { publicApi, privateApi } from "./axiosAPI";

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
          } catch (fetchError) {
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
