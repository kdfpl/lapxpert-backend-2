import axios from 'axios';

const VIETNAM_ADDRESS_API = 'https://provinces.open-api.vn/api/';

export default {
  getProvinces() {
    return axios.get(`${VIETNAM_ADDRESS_API}p/`);
  },
  getDistricts(provinceCode) {
    return axios.get(`${VIETNAM_ADDRESS_API}p/${provinceCode}?depth=2`);
  },
  getWards(districtCode) {
    return axios.get(`${VIETNAM_ADDRESS_API}d/${districtCode}?depth=2`);
  }
};