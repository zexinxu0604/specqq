import request from '@/utils/request'

/**
 * 客户端管理API
 */
export const clientApi = {
  /**
   * 分页查询客户端列表
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码
   * @param {number} params.size - 每页大小
   * @param {string} params.keyword - 关键词
   * @param {boolean} params.enabled - 是否启用
   */
  listClients(params) {
    return request({
      url: '/api/clients',
      method: 'get',
      params
    })
  },

  /**
   * 获取客户端详情
   * @param {number} id - 客户端ID
   */
  getClient(id) {
    return request({
      url: `/api/clients/${id}`,
      method: 'get'
    })
  },

  /**
   * 创建客户端
   * @param {Object} data - 客户端数据
   */
  createClient(data) {
    return request({
      url: '/api/clients',
      method: 'post',
      data
    })
  },

  /**
   * 更新客户端
   * @param {number} id - 客户端ID
   * @param {Object} data - 客户端数据
   */
  updateClient(id, data) {
    return request({
      url: `/api/clients/${id}`,
      method: 'put',
      data
    })
  },

  /**
   * 删除客户端
   * @param {number} id - 客户端ID
   */
  deleteClient(id) {
    return request({
      url: `/api/clients/${id}`,
      method: 'delete'
    })
  },

  /**
   * 测试客户端连接
   * @param {number} id - 客户端ID
   */
  testConnection(id) {
    return request({
      url: `/api/clients/${id}/test`,
      method: 'post'
    })
  },

  /**
   * 切换客户端启用状态
   * @param {number} id - 客户端ID
   * @param {boolean} enabled - 是否启用
   */
  toggleStatus(id, enabled) {
    return request({
      url: `/api/clients/${id}/status`,
      method: 'patch',
      params: { enabled }
    })
  }
}
