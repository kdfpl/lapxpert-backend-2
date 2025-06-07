import { privateApi } from './axiosAPI'

const STORAGE_BASE_URL = '/storage'

export default {
  /**
   * Upload files to MinIO storage
   * @param {File[]} files - Array of files to upload
   * @param {string} bucket - Bucket name (e.g., 'avatars', 'products')
   * @returns {Promise} - Promise resolving to array of uploaded file names
   */
  async uploadFiles(files, bucket = 'avatars') {
    const formData = new FormData()

    // Add files to form data
    files.forEach(file => {
      formData.append('files', file)
    })

    // Add bucket parameter
    formData.append('bucket', bucket)

    try {
      const response = await privateApi.post(`${STORAGE_BASE_URL}/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })
      return response.data
    } catch (error) {
      console.error('File upload error:', error)

      // Handle specific error types
      if (error.response?.status === 413) {
        // Payload Too Large
        const errorData = error.response.data
        if (errorData && typeof errorData === 'object') {
          throw new Error(errorData.message || 'File size exceeds maximum allowed limit (50MB)')
        }
        throw new Error('File size exceeds maximum allowed limit (50MB)')
      }

      // Handle other errors
      const errorMessage = error.response?.data?.message ||
                          error.response?.data?.error ||
                          'Failed to upload files'
      throw new Error(errorMessage)
    }
  },

  /**
   * Upload a single file to MinIO storage
   * @param {File} file - File to upload
   * @param {string} bucket - Bucket name
   * @returns {Promise} - Promise resolving to uploaded file name
   */
  async uploadFile(file, bucket = 'avatars') {
    const result = await this.uploadFiles([file], bucket)
    return result && result.length > 0 ? result[0] : null
  },

  /**
   * Get presigned URL for accessing a file
   * @param {string} bucket - Bucket name
   * @param {string} objectName - Object name/filename
   * @returns {Promise} - Promise resolving to presigned URL
   */
  async getPresignedUrl(bucket, objectName) {
    try {
      const response = await privateApi.get(`${STORAGE_BASE_URL}/url`, {
        params: { bucket, objectName }
      })
      return response.data
    } catch (error) {
      console.error('Get presigned URL error:', error)
      throw new Error(error.response?.data?.message || 'Failed to get file URL')
    }
  }
}
