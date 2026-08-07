/**
 * 省市区三级联动数据。
 * 数据结构：嵌套对象数组（省 → 市[] → 区[]）
 *
 * 数据来源说明：
 * - 本项目为演示用，只包含部分省市
 * - 生产环境可从国家统计局或 npm 包（如 china-division）获取完整数据
 */

export const REGION_DATA = [
  {
    name: '北京市',
    cities: [{ name: '北京市', districts: ['朝阳区', '海淀区', '通州区', '丰台区', '昌平区'] }]
  },
  {
    name: '上海市',
    cities: [{ name: '上海市', districts: ['浦东新区', '闵行区', '徐汇区', '静安区', '宝山区'] }]
  },
  {
    name: '广东省',
    cities: [
      { name: '广州市', districts: ['天河区', '越秀区', '海珠区', '白云区', '番禺区'] },
      { name: '深圳市', districts: ['南山区', '福田区', '宝安区', '龙岗区', '龙华区'] }
    ]
  },
  {
    name: '浙江省',
    cities: [
      { name: '杭州市', districts: ['西湖区', '余杭区', '滨江区', '拱墅区', '上城区'] },
      { name: '宁波市', districts: ['鄞州区', '海曙区', '江北区', '北仑区', '镇海区'] }
    ]
  },
  {
    name: '江苏省',
    cities: [
      { name: '南京市', districts: ['鼓楼区', '玄武区', '建邺区', '雨花台区', '江宁区'] },
      { name: '苏州市', districts: ['工业园区', '姑苏区', '吴中区', '相城区', '虎丘区'] }
    ]
  }
  // ... 其他省份省略
]

/**
 * 根据省份获取城市列表。
 * 使用可选链 ?. 和空值合并 || 提供默认空数组。
 * @param {string} provinceName - 省份名
 * @returns {Array} 城市数组，找不到时返回 []
 */
export const getCities = (provinceName) => {
  return REGION_DATA.find((province) => province.name === provinceName)?.cities || []
}

/**
 * 根据省份和城市获取区县列表。
 * 复用 getCities，函数组合模式。
 * @param {string} provinceName - 省份名
 * @param {string} cityName - 城市名
 * @returns {Array} 区县数组，找不到时返回 []
 */
export const getDistricts = (provinceName, cityName) => {
  return getCities(provinceName).find((city) => city.name === cityName)?.districts || []
}
