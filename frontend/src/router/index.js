import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layout/AppLayout.vue'
import AuthService from '@/apis/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/auth/LoginDashboard.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '/',
          name: 'dashboard',
          component: () => import('@/views/Dashboard.vue'),
        },
        {
          path: '/discounts',
          name: 'DiscountList',
          component: () => import('@/views/discount/DiscountList.vue'),
        },
        {
          path: '/discounts/edit/:id',
          name: 'DiscountEdit',
          component: () => import('@/views/discount/DiscountForm.vue'),
          props: true
        },
        {
          path: '/discounts/add',
          name: 'DiscountAdd',
          component: () => import('@/views/discount/DiscountForm.vue')
        },
        {
          path: '/products/list',
          name: 'products',
          component: () => import('@/views/product/ProductList.vue'),
        },
        {
          path: '/products/add',
          name: 'product-add',
          component: () => import('@/views/product/ProductForm.vue'),
        },
        {
          path: '/products/edit/:id',
          name: 'product-edit',
          component: () => import('@/views/product/ProductForm.vue'),
          props: true
        },
        {
          path: '/products/detail/:id',
          name: 'product-detail',
          component: () => import('@/views/product/ProductDetail.vue'),
          props: true
        },
        // Individual Attribute Management Pages (matching backend ThuocTinhController)
        {
          path: '/products/attributes/cpu',
          name: 'cpu-management',
          component: () => import('@/views/product/attributes/CpuManagement.vue'),
        },
        {
          path: '/products/attributes/ram',
          name: 'ram-management',
          component: () => import('@/views/product/attributes/RamManagement.vue'),
        },
        {
          path: '/products/attributes/gpu',
          name: 'gpu-management',
          component: () => import('@/views/product/attributes/GpuManagement.vue'),
        },
        {
          path: '/products/attributes/colors',
          name: 'color-management',
          component: () => import('@/views/product/attributes/ColorManagement.vue'),
        },
        {
          path: '/products/attributes/storage',
          name: 'storage-management',
          component: () => import('@/views/product/attributes/StorageManagement.vue'),
        },
        {
          path: '/products/attributes/screen',
          name: 'screen-management',
          component: () => import('@/views/product/attributes/ScreenManagement.vue'),
        },
        {
          path: '/products/attributes/category',
          name: 'category-management',
          component: () => import('@/views/product/attributes/CategoryManagement.vue'),
        },
        {
          path: '/products/attributes/brand',
          name: 'brand-management',
          component: () => import('@/views/product/attributes/BrandManagement.vue'),
        },
        {
          path: '/users/employees',
          name: 'employees',
          component: () => import('@/views/user/employees/Staff.vue'),
        },
        {
          path: '/staff/add',
          name: 'StaffAdd',
          component: () => import('@/views/user/employees/StaffForm.vue'),
        },
        {
          path: '/staff/edit/:id',
          name: 'StaffEdit',
          component: () => import('@/views/user/employees/StaffForm.vue'),
          props: true,
        },
        {
          path: '/users/customers',
          name: 'customers',
          component: () => import('@/views/user/customer/Customer.vue'),
        },
        {
          path: '/users/customers/edit/:id',
          name: 'CustomerEdit',
          component: () => import('@/views/user/customer/CustomerForm.vue'),
          props: true,
        },
        {
          path: '/users/customers/add',
          name: 'CustomerAdd',
          component: () => import('@/views/user/customer/CustomerForm.vue'),
        },
        {
          path: '/discounts/coupons',
          name: 'coupons',
          component: () => import('@/views/coupons/Coupons.vue'),
        },
        {
          path: '/discounts/couponsCRUD/:id?',
          name: 'couponsCRUD',
          component: () => import('@/views/coupons/CrudCoupons.vue'),
          props: true,
        },

        // Order Management Routes
        {
          path: '/orders',
          name: 'OrderList',
          component: () => import('@/views/orders/OrderList.vue'),
          meta: {
            title: 'Quản lý đơn hàng',
            breadcrumb: [
              { label: 'Trang chủ', to: '/' },
              { label: 'Quản lý đơn hàng', to: '/orders' }
            ],
            permissions: ['ORDER_VIEW', 'ORDER_MANAGE'],
            icon: 'pi pi-shopping-cart'
          }
        },
        {
          path: '/orders/create',
          name: 'OrderCreate',
          component: () => import('@/views/orders/OrderCreate.vue'),
          meta: {
            title: 'Tạo đơn hàng mới',
            breadcrumb: [
              { label: 'Trang chủ', to: '/' },
              { label: 'Quản lý đơn hàng', to: '/orders' },
              { label: 'Tạo đơn hàng mới', to: '/orders/create' }
            ],
            permissions: ['ORDER_CREATE'],
            icon: 'pi pi-plus'
          }
        },
        {
          path: '/orders/:id/edit',
          name: 'OrderEdit',
          component: () => import('@/views/orders/OrderEdit.vue'),
          props: true,
          meta: {
            title: 'Chỉnh sửa đơn hàng',
            breadcrumb: [
              { label: 'Trang chủ', to: '/' },
              { label: 'Quản lý đơn hàng', to: '/orders' },
              { label: 'Chỉnh sửa đơn hàng', to: null }
            ],
            permissions: ['ORDER_EDIT', 'ORDER_MANAGE'],
            icon: 'pi pi-pencil'
          }
        },
        {
          path: '/orders/:id',
          name: 'OrderDetail',
          component: () => import('@/views/orders/OrderDetail.vue'),
          props: true,
          meta: {
            title: 'Chi tiết đơn hàng',
            breadcrumb: [
              { label: 'Trang chủ', to: '/' },
              { label: 'Quản lý đơn hàng', to: '/orders' },
              { label: 'Chi tiết đơn hàng', to: null }
            ],
            permissions: ['ORDER_VIEW'],
            icon: 'pi pi-eye'
          }
        },
        {
          path: '/orders/payment-return',
          name: 'PaymentReturn',
          component: () => import('@/views/orders/PaymentReturn.vue'),
          meta: {
            title: 'Kết quả thanh toán',
            breadcrumb: [
              { label: 'Trang chủ', to: '/' },
              { label: 'Quản lý đơn hàng', to: '/orders' },
              { label: 'Kết quả thanh toán', to: null }
            ],
            permissions: ['ORDER_VIEW'],
            icon: 'pi pi-credit-card'
          }
        },

        // WebSocket Test Page (Development only)
        {
          path: '/websocket-test',
          name: 'WebSocketTest',
          component: () => import('@/views/WebSocketTest.vue'),
          meta: {
            title: 'WebSocket Connection Test',
            breadcrumb: [
              { label: 'Trang chủ', to: '/' },
              { label: 'WebSocket Test', to: '/websocket-test' }
            ],
            icon: 'pi pi-wifi'
          }
        },

      ],
    },



    {
      path: '/:pathMatch(.*)*',
      component: () => import('@/views/auth/Error.vue'),
    },
  ],
})

// Navigation guard with enhanced authentication checks
router.beforeEach(async (to, from, next) => {
  // Check if route requires authentication
  if (to.meta.requiresAuth) {
    // Use AuthService for comprehensive authentication check
    const isAuthenticated = AuthService.isAuthenticated();

    if (!isAuthenticated) {
      console.warn('Redirecting to login - authentication required');
      next('/login');
      return;
    }

    // Check user role permissions
    const role = AuthService.getRole();
    if (role === 'CUSTOMER') {
      console.warn('CUSTOMER role does not have access to admin dashboard');
      next('/error');
      return;
    }

    // For critical routes, validate session with backend
    if (to.meta.requiresValidation) {
      try {
        const sessionValid = await AuthService.checkSession();
        if (!sessionValid) {
          console.warn('Session validation failed - redirecting to login');
          next('/login');
          return;
        }
      } catch (error) {
        console.error('Session validation error:', error);
        next('/login');
        return;
      }
    }

    next();
  } else {
    // Public route - check if already authenticated and redirect to dashboard
    if (to.path === '/login' && AuthService.isAuthenticated()) {
      next('/');
      return;
    }
    next();
  }
});

export default router
