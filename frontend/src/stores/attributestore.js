import { defineStore } from 'pinia';
import attributeService from '@/apis/attribute';

export const useAttributeStore = defineStore('attributes', {
    state: () => ({
        // 6 Core Product Attributes (matching backend ThuocTinhController)
        cpu: [],
        ram: [],
        gpu: [],
        colors: [], // MauSac in backend
        storage: [], // BoNho in backend
        screen: [], // ManHinh in backend

        // 2 Additional Required Attributes
        category: [], // DanhMuc in backend
        brand: [], // ThuongHieu in backend

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

        async fetchColors() {
            try {
                this.loading = true;
                const response = await attributeService.getAllColors();
                this.colors = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching colors:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchStorage() {
            try {
                this.loading = true;
                const response = await attributeService.getAllStorage();
                this.storage = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching storage:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchScreen() {
            try {
                this.loading = true;
                const response = await attributeService.getAllScreen();
                this.screen = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching screen:', error);
            } finally {
                this.loading = false;
            }
        },



        async fetchBrand() {
            try {
                this.loading = true;
                const response = await attributeService.getAllBrand();
                this.brand = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching brand:', error);
            } finally {
                this.loading = false;
            }
        },

        async fetchCategory() {
            try {
                this.loading = true;
                const response = await attributeService.getAllCategory();
                this.category = response.data;
            } catch (error) {
                this.error = error.message;
                console.error('Error fetching category:', error);
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
                this.fetchColors(),
                this.fetchStorage(),
                this.fetchScreen(),

                // 2 Additional Required Attributes
                this.fetchCategory(),
                this.fetchBrand()
            ]);
        },

        // Reset error state
        clearError() {
            this.error = null;
        }
    },

    getters: {
        isLoading: (state) => state.loading,
        hasError: (state) => state.error !== null,
        getError: (state) => state.error
    }
});
