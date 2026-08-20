package com.example.ecommerce.dto;

/**
 * 店铺信息更新请求 DTO。
 * 所有字段均可空，由 Service 层全字段覆盖更新。
 */
public class StoreRequest {

    private String storeName;
    private String storeDescription;
    private String storeLogo;
    private String contactPhone;
    private String contactEmail;
    private String address;

    // getter/setter（后端 Service 层通过 getXxx() 读取字段值）
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreDescription() { return storeDescription; }
    public void setStoreDescription(String storeDescription) { this.storeDescription = storeDescription; }
    public String getStoreLogo() { return storeLogo; }
    public void setStoreLogo(String storeLogo) { this.storeLogo = storeLogo; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
