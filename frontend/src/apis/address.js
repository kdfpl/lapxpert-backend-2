import axios from 'axios';
import { privateApi } from './axiosAPI';

const VIETNAM_ADDRESS_API = 'https://provinces.open-api.vn/api/';
const GHN_ADDRESS_API = '/shipping/ghn';

export default {
  // Existing Vietnam Open API methods
  getProvinces() {
    return axios.get(`${VIETNAM_ADDRESS_API}p/`);
  },
  getDistricts(provinceCode) {
    return axios.get(`${VIETNAM_ADDRESS_API}p/${provinceCode}?depth=2`);
  },
  getWards(districtCode) {
    return axios.get(`${VIETNAM_ADDRESS_API}d/${districtCode}?depth=2`);
  },

  // New GHN address API methods
  getGHNProvinces() {
    return privateApi.get(`${GHN_ADDRESS_API}/provinces`);
  },
  getGHNDistricts(provinceId) {
    return privateApi.get(`${GHN_ADDRESS_API}/districts/${provinceId}`);
  },
  getGHNWards(districtId) {
    return privateApi.get(`${GHN_ADDRESS_API}/wards/${districtId}`);
  }
};
