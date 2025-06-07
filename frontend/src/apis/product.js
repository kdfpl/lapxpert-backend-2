import {privateApi} from "./axiosAPI";

const privateApi_URL = "/products";

const productService = {
  async getAllProducts() {
    try {
      const response = await privateApi.get(`${privateApi_URL}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching products:", error.response?.data || error.message);
      throw error;
    }
  },

  async getActiveProducts() {
    try {
      const response = await privateApi.get(`${privateApi_URL}/list`);
      return response.data;
    } catch (error) {
      console.error("Error fetching active products:", error.response?.data || error.message);
      throw error;
    }
  },

  async getProductById(id) {
    try {
      const response = await privateApi.get(`${privateApi_URL}/${id}`);
      return response.data;
    } catch (error) {
      console.error("Error fetching product by ID:", error.response?.data || error.message);
      throw error;
    }
  },

  async addProduct(sanPham) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/add`, sanPham);
      return response.data;
    } catch (error) {
      console.error("Error adding product:", error.response?.data || error.message);
      throw error;
    }
  },

  async addMultipleProduct(sanPham) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/addMultiple`, sanPham);
      return response.data;
    } catch (error) {
      console.error("Error adding product:", error.response?.data || error.message);
      throw error;
    }
  },

  async updateProduct(id, sanPham) {
    try {
      const response = await privateApi.put(`${privateApi_URL}/update/${id}`, sanPham);
      return response.data;
    } catch (error) {
      console.error("Error updating product:", error.response?.data || error.message);
      throw error;
    }
  },

  async updateProductWithVariants(id, sanPham) {
    try {
      const response = await privateApi.put(`${privateApi_URL}/updateWithVariants/${id}`, sanPham);
      return response.data;
    } catch (error) {
      console.error("Error updating product with variants:", error.response?.data || error.message);
      throw error;
    }
  },

  async softDeleteProduct(id) {
    try {
      const response = await privateApi.delete(`${privateApi_URL}/delete/${id}`);
      return response.data;
    } catch (error) {
      console.error("Error deleting product:", error.response?.data || error.message);
      throw error;
    }
  },

  // NEW: Audit trail methods
  async getProductAuditHistory(productId) {
    try {
      const response = await privateApi.get(`${privateApi_URL}/${productId}/audit-history`);
      return response.data;
    } catch (error) {
      console.error("Error fetching product audit history:", error.response?.data || error.message);
      throw error;
    }
  },

  async getProductDetailAuditHistory(productDetailId) {
    try {
      const response = await privateApi.get(`/products-details/${productDetailId}/audit-history`);
      return response.data;
    } catch (error) {
      console.error("Error fetching product detail audit history:", error.response?.data || error.message);
      throw error;
    }
  },

  // NEW: Status management methods
  async updateProductStatus(id, status, reason) {
    try {
      const response = await privateApi.put(`${privateApi_URL}/${id}/status`, {
        trangThai: status,
        lyDoThayDoi: reason
      });
      return response.data;
    } catch (error) {
      console.error("Error updating product status:", error.response?.data || error.message);
      throw error;
    }
  },

  async updateProductDetailStatus(id, status, reason) {
    try {
      const response = await privateApi.put(`/products-details/${id}/status`, {
        trangThai: status,
        lyDoThayDoi: reason
      });
      return response.data;
    } catch (error) {
      console.error("Error updating product detail status:", error.response?.data || error.message);
      throw error;
    }
  },

  // NEW: Enhanced search and filtering
  async searchProducts(filters) {
    try {
      const response = await privateApi.post(`${privateApi_URL}/search`, filters);
      return response.data;
    } catch (error) {
      console.error("Error searching products:", error.response?.data || error.message);
      throw error;
    }
  },

  // NEW: Batch operations
  async updateMultipleProductStatus(productIds, status, reason) {
    try {
      const response = await privateApi.put(`${privateApi_URL}/batch/status`, {
        productIds,
        trangThai: status,
        lyDoThayDoi: reason
      });
      return response.data;
    } catch (error) {
      console.error("Error updating multiple product status:", error.response?.data || error.message);
      throw error;
    }
  },

  async updateMultipleProductDetailStatus(productDetailIds, status, reason) {
    try {
      const response = await privateApi.put(`/products-details/batch/status`, {
        productDetailIds,
        trangThai: status,
        lyDoThayDoi: reason
      });
      return response.data;
    } catch (error) {
      console.error("Error updating multiple product detail status:", error.response?.data || error.message);
      throw error;
    }
  }
};

export default productService;
