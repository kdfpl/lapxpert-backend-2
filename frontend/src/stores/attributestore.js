import { defineStore } from 'pinia';
import attributeService from '@/apis/attribute';

export const useAttributeStore = defineStore('attributes', {
    state: () => ({
        // 6 Core Product Attributes (aligned with backend entity names)
        cpu: [],
        ram: [],
        gpu: [],
        mauSac: [], // Vietnamese naming aligned with backend MauSac entity
        boNho: [], // Vietnamese naming aligned with backend BoNho entity
        manHinh: [], // Vietnamese naming aligned with backend ManHinh entity

        // 2 Additional Required Attributes
        danhMuc: [], // Vietnamese naming aligned with backend DanhMuc entity
        thuongHieu: [], // Vietnamese naming aligned with backend ThuongHieu entity

        loading: false,
        error: null
    }),

    actions: {
        // 6 Core Product Attributes
        async fetchCpu() {
            try {
                this.loading = true;
                const response = await attributeService.getAllCpu();
                this.cpu = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching CPU:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchRam() {
            try {
                this.loading = true;
                const response = await attributeService.getAllRam();
                this.ram = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching RAM:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchGpu() {
            try {
                this.loading = true;
                const response = await attributeService.getAllGpu();
                this.gpu = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching GPU:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchMauSac() {
            try {
                this.loading = true;
                const response = await attributeService.getAllColors();
                this.mauSac = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching màu sắc:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchBoNho() {
            try {
                this.loading = true;
                const response = await attributeService.getAllStorage();
                this.boNho = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching bộ nhớ:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchManHinh() {
            try {
                this.loading = true;
                const response = await attributeService.getAllScreen();
                this.manHinh = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching màn hình:', error);
            } finally {
                this.loading = false;
            }
        },



        async fetchThuongHieu() {
            try {
                this.loading = true;
                const response = await attributeService.getAllBrand();
                this.thuongHieu = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching thương hiệu:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchDanhMuc() {
            try {
                this.loading = true;
                const response = await attributeService.getAllCategory();
                this.danhMuc = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching danh mục:', error);
            } finally {
                this.loading = false;
            }
        },

        // Utility action to fetch all core attributes at once
        async fetchAllAttributes() {
            await Promise.all([
                // 6 Core Product Attributes
                this.fetchCpu(),
                this.fetchRam(),
                this.fetchGpu(),
                this.fetchMauSac(),
                this.fetchBoNho(),
                this.fetchManHinh(),

                // 2 Additional Required Attributes
                this.fetchDanhMuc(),
                this.fetchThuongHieu()
            ]);
        },

        // Reset error state
        clearError() {
            this.error = null;
        },

        // Backward compatibility action methods for existing components
        async fetchColors() {
            return await this.fetchMauSac();
        },

        async fetchStorage() {
            return await this.fetchBoNho();
        },

        async fetchScreen() {
            return await this.fetchManHinh();
        },

        async fetchCategory() {
            return await this.fetchDanhMuc();
        },

        async fetchBrand() {
            return await this.fetchThuongHieu();
        }
    },

    getters: {
        isLoading: (state) => state.loading,
        hasError: (state) => state.error !== null,
        getError: (state) => state.error,

        // Backward compatibility getters for existing components
        colors: (state) => state.mauSac,
        storage: (state) => state.boNho,
        screen: (state) => state.manHinh,
        category: (state) => state.danhMuc,
        brand: (state) => state.thuongHieu
    }
});
