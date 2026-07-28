package com.example.ecommerce.dto;

/**
 * 购物车结算（下单）请求 DTO。
 *
 * 用户在购物车页面点击"去结算"时，前端将用户选择的收货地址 ID 发送给后端，
 * 后端根据购物车内容和地址生成订单。
 *
 * 为什么只传 addressId 而不是完整地址？
 *   - 前端视角：用户在结算页选择一个已保存的收货地址，前端知道地址 ID
 *   - 后端视角：后端通过 ID 从数据库查询完整地址信息
 *   - 安全考虑：如果传完整地址，恶意用户可能篡改地址内容；
 *     只传 ID，后端从自己的数据库读取，确保地址数据的可信性
 *   这是 DTO 设计中"用 ID 引用，而非嵌入完整对象"的典型模式。
 *
 * 购物车商品信息从哪来？
 *   本类不包含商品信息——购物车数据存储在后端数据库中，属于"服务端状态"。
 *   用户下单时，后端直接从当前用户的购物车中读取商品，
 *   而不是让前端再次提交，避免前端篡改商品价格或数量的风险。
 *
 * 为什么不用 @NotNull 校验 addressId？
 *   地址有效性的校验需要查询数据库（是否存在、是否属于当前用户），
 *   属于业务逻辑层的职责，不适合放在 DTO 的声明式验证中。
 *   DTO 验证注解适合做格式校验（非空、长度、正则），
 *   而存在性/业务规则校验应在 Service 层处理。
 */
public class CartCheckoutRequest {

    /** 收货地址 ID（对应 user_address 表主键），后端通过此 ID 查询完整地址 */
    private Long addressId;

    // ========== Getter / Setter ==========

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
}