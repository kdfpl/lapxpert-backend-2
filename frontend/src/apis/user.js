import {privateApi} from './axiosAPI'

const privateApi_BASE_URL = '/user'

export default {
  getCustomers(params = {}) {
    const queryParams = new URLSearchParams()

    // Add search parameter if provided
    if (params.search) {
      queryParams.append('search', params.search)
    }

    const url = queryParams.toString()
      ? `${privateApi_BASE_URL}/customer?${queryParams.toString()}`
      : `${privateApi_BASE_URL}/customer`

    console.log('UserAPI: Making request to URL:', url)
    return privateApi.get(url)
  },
  getCustomerById(id) {
    return privateApi.get(`${privateApi_BASE_URL}/customer/${id}`)
  },
  createCustomer(customerData) {
    return privateApi.post(`${privateApi_BASE_URL}/customer`, customerData)
  },
  updateCustomer(id, customerData) {
    return privateApi.put(`${privateApi_BASE_URL}/customer/${id}`, customerData)
  },
  deleteCustomer(id) {
    return privateApi.delete(`${privateApi_BASE_URL}/customer/${id}`)
  },
  restoreCustomer(id) {
    return privateApi.post(`${privateApi_BASE_URL}/customer/restore/${id}`)
  },

  getStaff() {
    return privateApi.get(`${privateApi_BASE_URL}/staff`)
  },
  getStaffById(id) {
    return privateApi.get(`${privateApi_BASE_URL}/staff/${id}`)
  },
  createStaff(staffData) {
    return privateApi.post(`${privateApi_BASE_URL}/staff`, staffData)
  },
  updateStaff(id, staffData) {
    return privateApi.put(`${privateApi_BASE_URL}/staff/${id}`, staffData)
  },
  deleteStaff(id) {
    return privateApi.delete(`${privateApi_BASE_URL}/staff/${id}`)
  },
  restoreStaff(id) {
    return privateApi.post(`${privateApi_BASE_URL}/staff/restore/${id}`)
  },

  // Validation endpoints for real-time form validation
  isEmailAvailable(email) {
    return privateApi.get(`${privateApi_BASE_URL}/validate/email/${encodeURIComponent(email)}`)
  },
  isPhoneAvailable(phone) {
    return privateApi.get(`${privateApi_BASE_URL}/validate/phone/${encodeURIComponent(phone)}`)
  },
  isCccdAvailable(cccd) {
    return privateApi.get(`${privateApi_BASE_URL}/validate/cccd/${encodeURIComponent(cccd)}`)
  },

  // Audit history endpoints
  getCustomerAuditHistory(id) {
    return privateApi.get(`${privateApi_BASE_URL}/customer/${id}/audit-history`)
  },
  getStaffAuditHistory(id) {
    return privateApi.get(`${privateApi_BASE_URL}/staff/${id}/audit-history`)
  },
}
