

# 电商后端项目

## 项目简介

本项目是一个基于Spring Boot的电商平台后端服务，提供完整的商品管理、购物车、订单处理、用户认证等功能。项目采用RESTful API设计，支持与前端应用无缝对接，并集成了消息队列、缓存、安全认证等企业级特性。

## 技术栈

- **核心框架**: Spring Boot 3.x
- **数据库**: MyBatis + MySQL
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **安全认证**: JWT + Spring Security
- **接口文档**: Spring Doc OpenAPI 3
- **工具库**: Jackson, Lombok, Apache POI

## 项目结构

```
src/main/java/com/example/ecommerce/
├── common/          # 公共组件（异常处理、响应封装、XSS过滤）
├── config/          # 配置类（安全、缓存、消息队列、跨域）
├── controller/      # REST接口层
├── dto/             # 数据传输对象
├── entity/          # 实体类
├── mapper/          # MyBatis映射接口
├── messaging/       # RabbitMQ消息处理
├── security/        # 安全认证组件
├── service/         # 业务逻辑层
└── utils/           # 工具类
```

## 核心功能

### 商品管理
- 商品的增删改查与分页查询
- 商品搜索（关键词、分类、价格区间）
- 热销商品推荐
- 商品数据导入（Excel）

### 购物车
- 添加/修改/删除购物车商品
- 库存校验
- 结算功能

### 订单系统
- 订单创建与状态管理
- 订单项记录
- 支付状态回调处理

### 用户系统
- 用户注册与登录
- JWT Token认证
- 用户地址管理
- 敏感数据脱敏

### 数据统计
- 商品浏览量追踪
- 每日指标聚合
- 购物车转化分析
- 订单支付统计

### 消息队列
- 订单创建事件
- 支付状态事件
- 死信队列处理
- 消息重试机制

## API接口

### 商品接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/products/{id} | 获取商品详情 |
| GET | /api/products/query | 分页查询商品 |
| GET | /api/products/hot | 获取热销商品 |

### 认证接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 用户登录 |
| POST | /api/auth/register | 用户注册 |

## 配置说明

配置文件位于 `src/main/resources/application.yml`，支持多环境配置（dev/test/prod）。

主要配置项：
- 数据库连接（spring.datasource）
- Redis配置（spring.data.redis）
- RabbitMQ配置（ecommerce.rabbit）
- 认证配置（ecommerce.auth）
- 文件上传（app.upload）

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+

### 启动步骤

```bash
# 1. 导入数据库脚本
mysql -u root -p < database.sql

# 2. 修改配置文件
# 编辑 src/main/resources/application-dev.yml 中的数据库和Redis连接信息

# 3. 编译运行
./mvnw spring-boot:run
```

### 接口文档

启动后可访问Swagger文档：`http://localhost:8080/swagger-ui.html`

## 测试

项目包含完整的单元测试和集成测试，覆盖Mapper和Service层：

```bash
# 运行所有测试
./mvnw test

# 运行特定测试类
./mvnw test -Dtest=ProductMapperTest
```

测试使用独立的test数据库配置（application-test.yml），测试结束后自动回滚。

## License

本项目仅供学习参考使用。
