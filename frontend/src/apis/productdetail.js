import {privateApi} from "./axiosAPI";

const privateApi_URL = "/products-details";

const productDetailService = {
  async getActiveProductsDetailed() {
    try {
      const response = await privateApi.get(`${privateApi_URL}/list`);
      return response.data;
    } catch (error) {
      console.error("Error fetching detailed active products:", error.response?.data || error.message);
      throw error;
    }
  },

  async addProductDetailed(sanPham) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/add`, sanPham);
      return response.data;
    } catch (error) {
      console.error("Error adding detailed product:", error.response?.data || error.message);
      throw error;
    }
  },

  async updateProductDetailed(id, sanPham) {
    try {
      const response = await privateApi.put(`${privateApi_URL}/update/${id}`, sanPham);
      return response.data;
    } catch (error) {
      console.error("Error updating detailed product:", error.response?.data || error.message);
      throw error;
    }
  },

  async softDeleteProductDetailed(id) {
    try {
      const response = await privateApi.delete(`${privateApi_URL}/delete/${id}`);
      return response.data;
    } catch (error) {
      console.error("Error deleting detailed product:", error.response?.data || error.message);
      throw error;
    }
  },
};

export default productDetailService;